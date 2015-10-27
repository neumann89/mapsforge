/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.routing.ch.preprocessing;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.routing.ch.preprocessing.evaluation.Statistics;
import org.mapsforge.routing.ch.preprocessing.graph.Graph;
import org.mapsforge.routing.ch.preprocessing.simulation.ContractionSimulationResult;
import org.mapsforge.routing.preprocessing.data.ArrayUtils;

/**
 * Provides the preprocessing of a graph as part of the Contraction Hierarchies algorithm.
 * 
 * @author Patrick Jungermann
 * @version $Id: Preprocessor.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class Preprocessor {

	/**
	 * Logger, used for this class.
	 */
	private static final Logger LOGGER = Logger.getLogger(Graph.class.getName());

	/**
	 * The preprocessing's settings.
	 */
	private final PreprocessorSettings settings;

	/**
	 * Constructor. Creates a {@link Preprocessor} instance with the given settings.
	 * 
	 * @param settings
	 *            The {@link Preprocessor}'s settings.
	 */
	public Preprocessor(final PreprocessorSettings settings) {
		this.settings = settings;
	}

	/**
	 * Loads the graph from a database and executes the preprocessing for this graph.
	 * 
	 * @param connection
	 *            The connection to the database, from the graph has to be loaded.
	 * @return The loaded graph.
	 * @throws SQLException
	 *             if there was a problem with the loading of the graph.
	 * @throws NoSuchMethodException
	 *             if there was a problem with the worker pool.
	 * @throws IllegalStateException
	 *             if there was a problem with instantiate the worker threads.
	 * @see Preprocessor#execute(Graph)
	 */
	public Graph execute(final Connection connection) throws SQLException, IllegalStateException,
			NoSuchMethodException {
		// load the basic graph
		final Graph graph = Graph.loadGraph(connection);
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("graph: #vertices=" + graph.getNumOfVertices() + ", #edges="
					+ graph.getNumOfEdges());
		}

		return execute(graph);
	}

	/**
	 * Executes the preprocessing for the given graph and returns this preprocessed graph at the end of
	 * it.
	 * 
	 * @param graph
	 *            The graph, which has to be preprocessed.
	 * @return The preprocessed graph.
	 * @throws NoSuchMethodException
	 *             if there was a problem with the worker pool.
	 * @throws IllegalStateException
	 *             if there was a problem with instantiate the worker threads.
	 */
	public Graph execute(final Graph graph) throws IllegalStateException, NoSuchMethodException {
		long executionStart = 0;
		if (Statistics.getInstance().isEnabled()) {
			executionStart = System.nanoTime();
		}

		final WorkerPool pool = new WorkerPool(graph, settings, settings.numThreads,
				5 * settings.numThreads);
		final ContractionSimulationResult[] simulationResults = new ContractionSimulationResult[graph
				.getNumOfVertices()];
		final float[] priorities = new float[graph.getNumOfVertices()];

		int[] vertexIds = graph.getVertexIds();

		final boolean[] processedVertices = new boolean[graph.getNumOfVertices()];

		// for each node: simulate contraction + initial calculation of priorities
		initialUpdate(pool, vertexIds, simulationResults, priorities, processedVertices);

		int[] independentVertices;
		int currentLayer = 0;
		long time = 0;
		while (vertexIds.length > 0) {
			if (LOGGER.isLoggable(Level.FINE)) {
				time = System.nanoTime();
			}
			if (LOGGER.isLoggable(Level.FINER)) {
				LOGGER.finer("current priorities: " + Arrays.toString(priorities));
			}

			// calculate the independent node set
			independentVertices = independentVertices(settings, graph, vertexIds, priorities,
					processedVertices);
			vertexIds = ArrayUtils.removeAllFrom(independentVertices, vertexIds);
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("#independent vertices: " + independentVertices.length
						+ ", #open vertices: " + vertexIds.length + " - "
						+ (System.nanoTime() - time) / 1000000000d + " seconds");

				if (LOGGER.isLoggable(Level.FINER)) {
					LOGGER.finer("remaining vertices: " + Arrays.toString(vertexIds));
				}
			}
			if (independentVertices.length == 0) {
				throw new RuntimeException(
						"Modification of the field [vertices] is not working. {size: "
								+ vertexIds.length + "}");
			}

			// layer will be incremented per loop; all nodes of a set have the same
			graph.setVertexLayer(independentVertices, currentLayer++);

			// for each node in I: add necessary shortcuts (aka contraction)
			contractVertices(pool, independentVertices, simulationResults, processedVertices);
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("Current #shortcuts: " + graph.getNumOfShortcuts());
			}

			// update all open vertices
			updateUnprocessed(pool, vertexIds, simulationResults, priorities, processedVertices);
		}

		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine(String.format(
					"graph: #vertices=%d, #edges=%d (#normal=%d, #shortcuts=%d)",
					graph.getNumOfVertices(), graph.getNumOfEdges(),
					graph.getNumOfEdges() - graph.getNumOfShortcuts(),
					graph.getNumOfShortcuts()
					));
		}

		if (Statistics.getInstance().isEnabled()) {
			Statistics.getInstance().preprocessing.durationInNs = System.nanoTime() - executionStart;

			graph.applyStatistics();
		}

		return graph;
	}

	/**
	 * Initial update of all vertices' data.
	 * 
	 * @param pool
	 *            The worker pool, used to get the update done.
	 * @param vertexIds
	 *            The identifier of each vertex, which has to be updated.
	 * @param simulationResults
	 *            The storage, containing each vertex' simulation result.
	 * @param priorities
	 *            The storage, containing each vertex' priority
	 * @param processedVertices
	 *            All processed vertices.
	 * @throws NoSuchMethodException
	 *             if there was a problem with the worker pool.
	 */
	protected void initialUpdate(final WorkerPool pool, final int[] vertexIds,
			final ContractionSimulationResult[] simulationResults, final float[] priorities,
			final boolean[] processedVertices) throws NoSuchMethodException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("Start the initial contraction simulations and priority calculations.");
		}

		updateVertices(pool, vertexIds, simulationResults, priorities, processedVertices);

		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("Initial vertex data updates finished.");
		}
	}

	/**
	 * Update for all unprocessed vertices.
	 * 
	 * @param pool
	 *            The worker pool, used to get the update done.
	 * @param vertexIds
	 *            The identifier of each vertex, which has to be updated.
	 * @param simulationResults
	 *            The storage, containing each vertex' simulation result.
	 * @param priorities
	 *            The storage, containing each vertex' priority
	 * @param processedVertices
	 *            All processed vertices.
	 * @throws NoSuchMethodException
	 *             if there was a problem with the worker pool.
	 */
	protected void updateUnprocessed(final WorkerPool pool, final int[] vertexIds,
			final ContractionSimulationResult[] simulationResults, final float[] priorities,
			final boolean[] processedVertices) throws NoSuchMethodException {

		updateVertices(pool, vertexIds, simulationResults, priorities, processedVertices);

		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("update of all open vertices finished");
		}
	}

	/**
	 * Updates each vertex' data.
	 * 
	 * @param pool
	 *            The worker pool, used to get the update done.
	 * @param vertexIds
	 *            The identifier of each vertex, which has to be updated.
	 * @param simulationResults
	 *            The storage, containing each vertex' simulation result.
	 * @param priorities
	 *            The storage, containing each vertex' priority
	 * @param processedVertices
	 *            All processed vertices.
	 * @throws NoSuchMethodException
	 *             if there was a problem with the worker pool.
	 */
	protected void updateVertices(final WorkerPool pool, final int[] vertexIds,
			final ContractionSimulationResult[] simulationResults, final float[] priorities,
			final boolean[] processedVertices) throws NoSuchMethodException {

		pool.startWork(vertexIds, VertexDataUpdaterThread.class, simulationResults, priorities,
				processedVertices);
		waitUntilPoolIsFinished(pool);
	}

	/**
	 * Contracts all vertices.
	 * 
	 * @param pool
	 *            The worker pool, used to get the update done.
	 * @param vertexIds
	 *            The identifier of each vertex, which has to be updated.
	 * @param simulationResults
	 *            The storage, containing each vertex' simulation result.
	 * @param processedVertices
	 *            The storage, containing the information, whether a vertex is already processed or not.
	 * @throws NoSuchMethodException
	 *             if there was a problem with the worker pool.
	 */
	protected void contractVertices(final WorkerPool pool, final int[] vertexIds,
			final ContractionSimulationResult[] simulationResults,
			final boolean[] processedVertices) throws NoSuchMethodException {
		// for each node in I: add necessary shortcuts (aka contraction)
		pool.startWork(vertexIds, IndependentVertexContractorThread.class,
				simulationResults, processedVertices);
		waitUntilPoolIsFinished(pool);

		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("All contractions of the current independent vertices are finished.");
		}
	}

	/**
	 * Waits until the pool has finished its work.
	 * 
	 * @param pool
	 *            The pool, which is handling some work.
	 */
	protected static void waitUntilPoolIsFinished(WorkerPool pool) {
		while (!pool.isFinished()) {
			try {
				Thread.sleep(100);

			} catch (InterruptedException e) {
				LOGGER.log(Level.SEVERE,
						"Waiting for the pool's work to be finished failed due to an interruption: "
								+ e.getMessage(), e);

				throw new RuntimeException("Pool was not able to finish its work.", e);
			}
		}
	}

	/**
	 * Determines all independent vertices from the given ones.
	 * 
	 * @param settings
	 *            The preprocessing's settings.
	 * @param graph
	 *            The related graph.
	 * @param vertexIds
	 *            The vertices, which has to be checked.
	 * @param priorities
	 *            The priority values of all vertices.
	 * @param processedVertices
	 *            All processed vertices.
	 * @return All independent vertices.
	 */
	protected static int[] independentVertices(final PreprocessorSettings settings, final Graph graph,
			final int[] vertexIds, final float[] priorities, final boolean[] processedVertices) {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("find independent vertices out of " + vertexIds.length);
		}
		final int[] tmp = new int[vertexIds.length];
		int idx = 0;

		for (final int vertexId : vertexIds) {
			if (isIndependent(settings, graph, vertexId, priorities, processedVertices)) {
				tmp[idx++] = vertexId;
			}
		}

		final int[] independentVertices = new int[idx];
		System.arraycopy(tmp, 0, independentVertices, 0, idx);

		return independentVertices;
	}

	/**
	 * Checks, if the vertex is independent from other vertices, based on the given settings and data.
	 * 
	 * @param settings
	 *            The settings related to the preprocessing.
	 * @param graph
	 *            The related graph.
	 * @param vertexId
	 *            The vertex' identifier.
	 * @param priorities
	 *            The current priority values of all vertices.
	 * @param processedVertices
	 *            The information about which vertices have already be processed and which of them not.
	 * @return {@code true}, if this vertex is independent, otherwise {@code false}.
	 */
	protected static boolean isIndependent(final PreprocessorSettings settings, final Graph graph,
			final int vertexId, final float[] priorities, final boolean[] processedVertices) {
		final float ownPriority = priorities[vertexId];

		final TIntHashSet alreadyChecked = new TIntHashSet();
		TIntHashSet toBeChecked = new TIntHashSet();
		toBeChecked.add(vertexId);
		alreadyChecked.add(vertexId);

		for (int hop = 1; hop <= settings.kNeighborhood && !toBeChecked.isEmpty(); hop++) {
			final TIntHashSet tmp = new TIntHashSet();
			final TIntIterator iterator = toBeChecked.iterator();

			while (iterator.hasNext()) {
				final int currentVertexId = iterator.next();

				// check each neighbor, given by outgoing edges
				if (!isIndependentOfNeighbors(graph, vertexId, currentVertexId,
						graph.getOutgoingEdgesOfVertex(currentVertexId),
						alreadyChecked, tmp, processedVertices, priorities, ownPriority)) {
					return false;
				}

				// check each neighbor, given by ingoing edges
				if (!isIndependentOfNeighbors(graph, vertexId, currentVertexId,
						graph.getIngoingEdgesOfVertex(currentVertexId),
						alreadyChecked, tmp, processedVertices, priorities, ownPriority)) {
					return false;
				}
			}

			toBeChecked = tmp;
		}

		if (LOGGER.isLoggable(Level.FINER)) {
			LOGGER.finer("vertex " + vertexId + " - checked (in)direct neighbors: "
					+ Arrays.toString(alreadyChecked.toArray()));
		}

		return true;
	}

	/**
	 * Checks, if the vertex is independent of all (in)direct neighbors, given by the edges.
	 * 
	 * @param graph
	 *            The related graph.
	 * @param vertexId
	 *            The vertex, which has to be checked.
	 * @param currentVertexId
	 *            The vertex' identifier, which is the origin of the edges. Might be the vertex, which
	 *            has to be checked, or only an intermediate vertex.
	 * @param edgeIds
	 *            All edges' identifiers.
	 * @param alreadyChecked
	 *            Contains all vertices, which have already been found.
	 * @param toBeChecked
	 *            All vertices, from which the neighbors have to be checked afterwards. All newly found
	 *            vertices will be added there.
	 * @param processedVertices
	 *            The information about which vertices have already be processed and which of them not.
	 * @param priorities
	 *            The current priority values of all vertices.
	 * @param ownPriority
	 *            The priority of the vertex.
	 * @return {@code true}, if it is independent, otherwise {@code false}.
	 */
	protected static boolean isIndependentOfNeighbors(final Graph graph, final int vertexId,
			final int currentVertexId, final int[] edgeIds,
			final TIntHashSet alreadyChecked, final TIntHashSet toBeChecked,
			final boolean[] processedVertices, final float[] priorities,
			final float ownPriority) {

		for (final int edgeId : edgeIds) {
			final int neighborId = graph.getOtherVertexOfEdge(edgeId, currentVertexId);
			if (!alreadyChecked.contains(neighborId)) {
				toBeChecked.add(neighborId);
				alreadyChecked.add(neighborId);
			}

			// ignore all vertices, which are already handled
			if (!processedVertices[neighborId]) {
				final float priority = priorities[neighborId];
				if (ownPriority > priority
						|| (ownPriority == priority && vertexId > neighborId)) {
					return false;
				}
			}
		}

		return true;
	}
}
