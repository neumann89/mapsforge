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
package org.mapsforge.routing.ch.preprocessing.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.mapsforge.routing.Rect;
import org.mapsforge.routing.ch.preprocessing.evaluation.Statistics;
import org.mapsforge.routing.ch.preprocessing.graph.CHGraphImpl;
import org.mapsforge.routing.preprocessing.data.clustering.Cluster;
import org.mapsforge.routing.preprocessing.data.clustering.ClusterBlockMapping;
import org.mapsforge.routing.preprocessing.data.clustering.ClusterUtils;
import org.mapsforge.routing.preprocessing.data.clustering.Clustering;
import org.mapsforge.routing.preprocessing.data.clustering.ClusteringSettings;
import org.mapsforge.routing.preprocessing.data.clustering.KCenterClusteringAlgorithm;
import org.mapsforge.routing.preprocessing.data.clustering.QuadTreeClusteringAlgorithm;
import org.mapsforge.routing.preprocessing.data.clustering.UnsupportedClusteringAlgorithmException;
import org.mapsforge.routing.preprocessing.io.AddressLookupTableWriter;
import org.mapsforge.routing.preprocessing.io.IOUtils;
import org.mapsforge.routing.preprocessing.io.StaticRTreeWriter;

/**
 * Writer for Contraction Hierarchies' binary file.
 * 
 * @author Patrick Jungermann
 * @version $Id: FileWriter.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class FileWriter {

	/**
	 * Class-level logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(FileWriter.class.getName());

	/**
	 * Writes the binary targetFile for
	 * {@link org.mapsforge.routing.ch.preprocessing.CommandLine.Format#MOBILE
	 * mobile devices}.
	 * 
	 * @param targetFile
	 *            The target file, to which the data has to be written.
	 * @param connection
	 *            The connection, from which the data could be retrieved.
	 * @param settings
	 *            The settings related to the data clustering.
	 * @param indexGroupSizeThreshold
	 *            The threshold for each index group's size.
	 * @param rTreeBlockSize
	 *            The size of one R-tree's block.
	 * @throws SQLException
	 *             if there was any problem with the reading of the data.
	 * @throws UnsupportedClusteringAlgorithmException
	 *             if the selected algorithm is not supported.
	 * @throws IOException
	 *             if there was any problem with writing the data.
	 */
	public static void write(final File targetFile, final Connection connection,
			final ClusteringSettings settings, final int indexGroupSizeThreshold,
			final int rTreeBlockSize)
			throws SQLException, UnsupportedClusteringAlgorithmException, IOException {
		// load the graph
		final CHGraphImpl graph = new CHGraphImpl(connection);

		long start = 0;
		if (Statistics.getInstance().isEnabled()) {
			start = System.nanoTime();
		}

		// compute the clustering
		final Clustering clustering;
		switch (settings.algorithm) {
			case K_CENTER:
				clustering = KCenterClusteringAlgorithm.computeClustering(
						graph,
						graph.numVertices() / settings.clusterSizeThreshold,
						settings.oversamplingFactor,
						KCenterClusteringAlgorithm.HEURISTIC_MIN_RADIUS);
				break;
			case QUAD_TREE:
				clustering = QuadTreeClusteringAlgorithm.computeClustering(
						graph,
						graph.getVertexLongitudesE6(), graph.getVertexLatitudesE6(),
						QuadTreeClusteringAlgorithm.HEURISTIC_MEDIAN,
						settings.clusterSizeThreshold, graph.numVertices());
				break;
			default:
				throw new UnsupportedClusteringAlgorithmException(settings.algorithm);
		}

		if (Statistics.getInstance().isEnabled()) {
			final Statistics.Clustering stats = Statistics.getInstance().clustering;

			stats.durationInNs = System.nanoTime() - start;
			stats.numClusters = clustering.size();
		}

		// temp. files
		final File dir = targetFile.getParentFile();
		final String name = targetFile.getName();
		final File blocks = new File(dir, name + ".blocks");
		final File addressLookupTable = new File(dir, name + ".alt");
		final File rTree = new File(dir, name + ".rtree");

		// write the graph's cluster blocks
		final ClusterBlockMapping mapping = new ClusterBlockMapping(new Clustering[] { clustering });
		final int[] blockSizes = new GraphWriter(graph, clustering, mapping).write(blocks);

		if (Statistics.getInstance().isEnabled()) {
			final Statistics.Clustering stats = Statistics.getInstance().clustering;

			int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
			float sum = 0;
			for (final int blockSize : blockSizes) {
				sum += blockSize;

				if (blockSize < min) {
					min = blockSize;
				}
				if (blockSize > max) {
					max = blockSize;
				}
			}

			stats.minBlockSize = min;
			stats.maxBlockSize = max;
			stats.avgBlockSize = sum / blockSizes.length;
		}

		// write block index
		AddressLookupTableWriter.writeTable(blockSizes, indexGroupSizeThreshold, addressLookupTable);

		// write R-tree
		final int[] minLongitudes = new int[clustering.size()];
		final int[] maxLongitudes = new int[clustering.size()];
		final int[] minLatitudes = new int[clustering.size()];
		final int[] maxLatitudes = new int[clustering.size()];
		final int[] blockIds = new int[clustering.size()];

		Rect boundingBox;
		int i = 0;
		for (final Cluster cluster : clustering.getClusters()) {
			boundingBox = ClusterUtils.getBoundingBox(cluster, graph);

			minLongitudes[i] = boundingBox.getMinLongitudeE6();
			maxLongitudes[i] = boundingBox.getMaxLongitudeE6();
			minLatitudes[i] = boundingBox.getMinLatitudeE6();
			maxLatitudes[i] = boundingBox.getMaxLatitudeE6();

			blockIds[i++] = mapping.getBlockId(cluster);
		}

		StaticRTreeWriter.packSortTileRecursive(minLongitudes, maxLongitudes,
				minLatitudes, maxLatitudes, blockIds, rTreeBlockSize, rTree,
				HeaderGlobals.STATIC_R_TREE_HEADER_MAGIC);

		// combine all parts to the final binary file
		writeFile(targetFile, blocks, addressLookupTable, rTree);

		// cleanup: delete temp. files
		if (!blocks.delete()) {
			LOGGER.severe("Failed to delete the file " + blocks.getAbsolutePath());
		}
		if (!addressLookupTable.delete()) {
			LOGGER.severe("Failed to delete the file " + addressLookupTable.getAbsolutePath());
		}
		if (!rTree.delete()) {
			LOGGER.severe("Failed to delete the file " + rTree.getAbsolutePath());
		}
	}

	/**
	 * Writes the final binary file consisting of the file's header and the data of the other files: The
	 * graph's data of the {@code blocks} file, the address lookup table of {@code addressLookup} file
	 * and the R-tree of the {@code rTree} file.
	 * 
	 * @param targetFile
	 *            The target file, to which all the data will be written.
	 * @param blocks
	 *            The file, containing the graph's data.
	 * @param addressLookupTable
	 *            The file, containing the data related to the address lookup table.
	 * @param rTree
	 *            The file, containing the data of the R-tree.
	 * @throws IOException
	 *             if there was any problem with writing the data to the file or reading the other files
	 *             data.
	 */
	private static void writeFile(final File targetFile, final File blocks,
			final File addressLookupTable,
			final File rTree) throws IOException {

		DataOutputStream out = null;

		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(targetFile)));

			writeFileHeader(out, blocks.length(), addressLookupTable.length());

			// append all parts
			IOUtils.writeFilesToStream(out, new File[] { blocks, addressLookupTable, rTree });

		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Writes the binary files' header to the output stream.
	 * 
	 * @param out
	 *            The output stream, to which the header has to be written.
	 * @param fileLengthBlocks
	 *            The length of the {@link File}, which contains the graph blocks.
	 * @param fileLengthAddressLookupTable
	 *            The length of the {@link File}, which contains the address lookup table.
	 * @throws IOException
	 *             if there was a problem with writing the header to the stream.
	 */
	private static void writeFileHeader(final DataOutputStream out, final long fileLengthBlocks,
			final long fileLengthAddressLookupTable) throws IOException {
		// get file part addresses / positions
		// a start equals the end of the previous part; end addresses are redundant
		final long startAddressGraphBlocks = HeaderGlobals.BINARY_FILE_HEADER_LENGTH;
		final long startAddressIndex = startAddressGraphBlocks + fileLengthBlocks;
		final long startAddressRTree = startAddressIndex + fileLengthAddressLookupTable;

		// write the file header
		out.write(HeaderGlobals.BINARY_FILE_HEADER_MAGIC);
		out.writeLong(startAddressGraphBlocks);
		out.writeLong(startAddressIndex);
		out.writeLong(startAddressRTree);

		// fill header with empty data
		IOUtils.finalizeHeader(out, HeaderGlobals.BINARY_FILE_HEADER_LENGTH);
	}
}
