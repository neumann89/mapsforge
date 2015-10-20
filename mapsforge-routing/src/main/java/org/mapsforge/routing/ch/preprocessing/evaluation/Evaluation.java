package org.mapsforge.routing.ch.preprocessing.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.routing.ch.android.CHRouter;
import org.mapsforge.routing.ch.android.QueryingStatistics;
import org.mapsforge.routing.ch.preprocessing.Preprocessor;
import org.mapsforge.routing.ch.preprocessing.PreprocessorSettings;
import org.mapsforge.routing.ch.preprocessing.graph.Graph;
import org.mapsforge.routing.ch.preprocessing.io.FileWriter;
import org.mapsforge.routing.preprocessing.data.clustering.ClusteringAlgorithm;
import org.mapsforge.routing.preprocessing.data.clustering.ClusteringSettings;
import org.mapsforge.routing.preprocessing.data.clustering.UnsupportedClusteringAlgorithmException;
import org.mapsforge.routing.evaluation.Route;
import org.mapsforge.routing.preprocessing.sql.DBConnection;

/**
 * @author Patrick Jungermann
 * @version $Id$
 */
// TODO: documentation
public class Evaluation {

	/**
	 * Class-level logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(Evaluation.class.getName());

	private static final int INDEX_GROUP_SIZE_THRESHOLD = 50;
	private static final int R_TREE_BLOCK_SIZE = 4096;
	private static final int ROUTER_CACHE_SIZE = 1024 * 1024 * 2;

	public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException,
			NoSuchMethodException, UnsupportedClusteringAlgorithmException {
		if (args.length != 3) {
			System.out.println(Evaluation.class.getSimpleName()
					+ " {evaluation settings properties file} {ROUTES file} {statistics output file}");
			System.out.println(java.util.Arrays.toString(args));
			System.exit(1);
		}

		Properties settings = new Properties(getDefaultSettings());
		settings.load(new FileInputStream(args[0]));
		File routes = new File(args[1]);
		File statistics = new File(args[2]);

		Connection connection = createConnection(settings);

		new Evaluation().evaluate(connection, routes, statistics, EvaluationSettings.create(settings));
	}

	public void evaluate(Connection connection, File routesFile, File statsFile,
			EvaluationSettings settings) throws IOException, SQLException,
			UnsupportedClusteringAlgorithmException, NoSuchMethodException, ClassNotFoundException {
		if (LOGGER.isLoggable(Level.INFO)) {
			LOGGER.info("evaluation started");
		}

		File mchFile = null;
		java.io.FileWriter statsOut = null;
		try {
			mchFile = File.createTempFile("evaluation", ".mch");
			statsOut = new java.io.FileWriter(statsFile);
			Route[] routes = deserializeRoutes(routesFile);
			Statistics.getInstance().enable(true);

			final PreprocessorSettings preprocessorSettings = new PreprocessorSettings();
			// check different settings
			for (int searchSpaceHopLimit : settings.searchSpaceHopLimits) {
				preprocessorSettings.searchSpaceHopLimit = searchSpaceHopLimit;

				for (int kNeighborHood : settings.kNeighborhood) {
					preprocessorSettings.kNeighborhood = kNeighborHood;

					for (int edgeQuotientFactor : settings.edgeQuotientFactor) {
						preprocessorSettings.edgeQuotientFactor = edgeQuotientFactor;

						for (int hierarchyDepthsFactor : settings.hierarchyDepthsFactor) {
							preprocessorSettings.hierarchyDepthsFactor = hierarchyDepthsFactor;

							for (int originalEdgeQuotientFactor : settings.originalEdgeQuotientFactor) {
								// case 0,0,0 makes no sense - all priority parts would be ignored
								if (edgeQuotientFactor != 0 || hierarchyDepthsFactor != 0
										|| originalEdgeQuotientFactor != 0) {
									preprocessorSettings.originalEdgeQuotientFactor = originalEdgeQuotientFactor;

									String rowPrefix = evaluatePreprocessing(connection,
											preprocessorSettings);
									evaluateClusterings(connection, routes, statsOut, mchFile,
											settings, rowPrefix);
								}
							}
						}
					}
				}
			}

		} finally {
			if (mchFile != null) {
				if (!mchFile.delete()) {
					mchFile.deleteOnExit();
					LOGGER.severe("Failed to delete the file: " + mchFile.getCanonicalPath());
				}
			}

			if (statsOut != null) {
				statsOut.close();
			}
		}

		if (LOGGER.isLoggable(Level.INFO)) {
			LOGGER.info("evaluation finished");
		}
	}

	protected Route[] deserializeRoutes(File routesFile) throws IOException, ClassNotFoundException {
		final ObjectInputStream in = new ObjectInputStream(new FileInputStream(routesFile));
		return (Route[]) in.readObject();
	}

	protected String evaluatePreprocessing(Connection connection, PreprocessorSettings settings)
			throws NoSuchMethodException, SQLException {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("start evaluatePreprocessing with " + settings.toString());
        }

		Statistics.getInstance().reset();
		Statistics.Preprocessing stats = Statistics.getInstance().preprocessing;

		final Preprocessor preprocessor = new Preprocessor(settings);
		final Graph graph = preprocessor.execute(connection);
		if (!graph.saveGraph(connection)) {
			throw new RuntimeException("There was a problem with saving the graph to the database.");
		}

		return String.format(
				"%d;%d;%d;%d;%d;%d;%d;%d;%d;%.2f;%d;%.2f;%d;%d;%.2f;%d;%d",
				settings.searchSpaceHopLimit, settings.kNeighborhood, settings.edgeQuotientFactor,
				settings.hierarchyDepthsFactor, settings.originalEdgeQuotientFactor,
				stats.numVertices, stats.numEdges, stats.numNormalEdges, stats.numShortcutEdges,
				stats.avgLevel, stats.maxLevel, stats.avgHierarchyDepth, stats.maxHierarchyDepth,
				stats.minOriginalEdgeCountOfShortcuts, stats.avgOriginalEdgeCountOfShortcuts,
				stats.maxOriginalEdgeCountOfShortcuts,
				stats.durationInNs
				);
	}

	protected void evaluateClusterings(Connection connection, Route[] routes,
			java.io.FileWriter statsOut, File mchFile, EvaluationSettings settings, String rowPrefix)
			throws IOException, SQLException, UnsupportedClusteringAlgorithmException {
		ClusteringSettings clusteringSettings = new ClusteringSettings();

		for (ClusteringAlgorithm algorithm : settings.algorithms) {
			clusteringSettings.algorithm = algorithm;

			for (int clusterSizeThreshold : settings.clusterSizeThresholds) {
				clusteringSettings.clusterSizeThreshold = clusterSizeThreshold;

				// only needed for K_Center
				int[] oversamplingFactors = algorithm.equals(ClusteringAlgorithm.K_CENTER) ? settings.oversamplingFactors
						: new int[] { 0 };
				for (int oversamplingFactor : oversamplingFactors) {
					clusteringSettings.oversamplingFactor = oversamplingFactor;

					String newRowPrefix = evaluateClustering(connection, clusteringSettings, mchFile,
							rowPrefix);
					evaluateQuerying(mchFile, routes, statsOut, newRowPrefix);
				}
			}
		}
	}

	protected String evaluateClustering(Connection connection, ClusteringSettings settings,
			File mchFile, String rowPrefix) throws IOException, SQLException,
			UnsupportedClusteringAlgorithmException {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("start evaluateClustering with " + settings.toString());
        }

		Statistics.getInstance().reset();
		Statistics.Clustering stats = Statistics.getInstance().clustering;

		FileWriter.write(mchFile, connection, settings,
				INDEX_GROUP_SIZE_THRESHOLD, R_TREE_BLOCK_SIZE);

		if (!rowPrefix.isEmpty() && rowPrefix.charAt(rowPrefix.length() - 1) != ';') {
			rowPrefix += ";";
		}

		return rowPrefix + String.format(
                "\"%s\";%d;%d;%d;%d;%.2f;%d;%d",
                settings.algorithm.name(),
                settings.clusterSizeThreshold, settings.oversamplingFactor,
                stats.durationInNs, stats.minBlockSize, stats.avgBlockSize, stats.maxBlockSize,
                mchFile.length()
        );
	}

	protected void evaluateQuerying(File mchFile, Route[] routes, java.io.FileWriter statsOut,
			String rowPrefix) throws IOException {
		QueryingStatistics stats = QueryingStatistics.getInstance();
		stats.enable(true);
		CHRouter chRouter = null;

		QueryingEvaluationResults cold = new QueryingEvaluationResults();
		QueryingEvaluationResults warm = new QueryingEvaluationResults();
		int i = 0;
		for (final Route route : routes) {
			try {
				if (LOGGER.isLoggable(Level.INFO)) {
					LOGGER.info((i++) + " " + route.toString());
				}

				chRouter = new CHRouter(mchFile, ROUTER_CACHE_SIZE);

				// cold cache
				stats.reset();
				evaluateQuerying(chRouter, route, cold, stats);

				// warm cache
				stats.reset();
				evaluateQuerying(chRouter, route, warm, stats);

			} finally {
				if (chRouter != null) {
					chRouter.close();
				}
			}
		}

		statsOut.append(rowPrefix);
		if (!rowPrefix.isEmpty() && rowPrefix.charAt(rowPrefix.length() - 1) != ';') {
			statsOut.append(';');
		}
		statsOut.append(String.format(
				"%d;%.2f;%d;%d;%.2f;%d;%d;%.2f;%d;%d;%.2f;%d;%d;%.2f;%d;%d;%.2f;%d\n",
				cold.minDuration, cold.sumDurations / routes.length, cold.maxDuration,
				warm.minDuration, warm.sumDurations / routes.length, warm.maxDuration,
				cold.minBlockReads, cold.sumBlockReads / routes.length, cold.maxBlockReads,
				warm.minBlockReads, warm.sumBlockReads / routes.length, warm.maxBlockReads,
				cold.minVisitedVertices, cold.sumVisitedVertices / routes.length,
				cold.maxVisitedVertices,
				warm.minVisitedVertices, warm.sumVisitedVertices / routes.length,
				warm.maxVisitedVertices
				));
        statsOut.flush();
	}

	protected void evaluateQuerying(CHRouter chRouter, Route route, QueryingEvaluationResults result,
			QueryingStatistics stats) {
		// calculate the route
		long start = System.nanoTime();
		chRouter.getShortestPath(
				chRouter.getNearestVertex(route.source).getId(),
				chRouter.getNearestVertex(route.target).getId()
				);
		long duration = System.nanoTime() - start;

		// durations
		result.sumDurations += duration;
		if (duration < result.minDuration) {
			result.minDuration = duration;
		}
		if (duration > result.maxDuration) {
			result.maxDuration = duration;
		}

		// block reads
		result.sumBlockReads += stats.numBlockReads;
		if (stats.numBlockReads < result.minBlockReads) {
			result.minBlockReads = stats.numBlockReads;
		}
		if (stats.numBlockReads > result.maxBlockReads) {
			result.maxBlockReads = stats.numBlockReads;
		}

		// visited vertices
		int numVisited = stats.getNumVisitedVertices();
		result.sumVisitedVertices += numVisited;
		if (numVisited < result.minVisitedVertices) {
			result.minVisitedVertices = numVisited;
		}
		if (numVisited > result.maxVisitedVertices) {
			result.maxVisitedVertices = numVisited;
		}
	}

	protected static Connection createConnection(final Properties config) throws SQLException {
		// initialize database connection
		return DBConnection.getConnectionToPostgreSQL(
                config.getProperty("db.host"),
                Integer.parseInt(config.getProperty("db.port"), 10),
                config.getProperty("db.name"),
                config.getProperty("db.user"),
                config.getProperty("db.pass"));
	}

	protected static Properties getDefaultSettings() {
		Properties properties = new Properties();
		properties.setProperty("db.host", "localhost");
		properties.setProperty("db.port", "5432");
		properties.setProperty("db.name", "osm");
		properties.setProperty("db.user", "osm");
		properties.setProperty("db.pass", "osm");

		return properties;
	}

}
