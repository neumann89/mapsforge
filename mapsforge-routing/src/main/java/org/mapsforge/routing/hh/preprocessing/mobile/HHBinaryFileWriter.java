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
package org.mapsforge.routing.hh.preprocessing.mobile;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.mapsforge.routing.Rect;
import org.mapsforge.routing.preprocessing.data.clustering.Cluster;
import org.mapsforge.routing.preprocessing.data.clustering.ClusterBlockMapping;
import org.mapsforge.routing.preprocessing.data.clustering.ClusterUtils;
import org.mapsforge.routing.preprocessing.data.clustering.Clustering;
import org.mapsforge.routing.preprocessing.data.clustering.KCenterClusteringAlgorithm;
import org.mapsforge.routing.preprocessing.data.clustering.QuadTreeClusteringAlgorithm;
import org.mapsforge.routing.preprocessing.io.AddressLookupTableWriter;
import org.mapsforge.routing.preprocessing.io.IOUtils;
import org.mapsforge.routing.preprocessing.io.StaticRTreeWriter;

/**
 * This class is responsible for writing a binary file for the mobile highway hierarchies algorithm.
 */
public class HHBinaryFileWriter {
	/** parameter for specifying the clustering algorithm */
	public static final String CLUSTERING_ALGORTHM_K_CENTER = "k_center";
	/** parameter for specifying the clustering algorithm */
	public static final String CLUSTERING_ALGORITHM_QUAD_TREE = "quad_tree";

	private final static byte[] HEADER_MAGIC = HHGlobals.BINARY_FILE_HEADER_MAGIC;
	private final static int HEADER_LENGTH = HHGlobals.BINARY_FILE_HEADER_LENGTH;

	private final static int BUFFER_SIZE = 16384 * 1000;

	/**
	 * Writes the binary file for the mobile highway hierarchies algorithm. input is take from a
	 * database.
	 * 
	 * @param conn
	 *            input database.
	 * @param clusteringAlgorithmName
	 *            name of the clustering algorithm, see static class variables.
	 * @param clusterSizeThreshold
	 *            limit on the number of nodes per logical block of the graph.
	 * @param kcenterOversamplingFactor
	 *            controls the quality of the k-center clusters.
	 * @param targetFile
	 *            file to write output to.
	 * @param indexGroupSizeThreshold
	 *            controls compression and runtime overhead of the address lookup table.
	 * @param rtreeBlockSize
	 *            sets size and alignment of r tree nodes.
	 * @param includeHopIndices
	 *            set to true for storing pre-computed information for shortcut expansion.
	 * @throws IOException
	 *             on error reading or writing file.
	 * @throws SQLException
	 *             on error with database.
	 */
	public static void writeBinaryFile(Connection conn, String clusteringAlgorithmName,
			int clusterSizeThreshold, int kcenterOversamplingFactor,
			File targetFile, int indexGroupSizeThreshold, int rtreeBlockSize,
			boolean includeHopIndices) throws IOException, SQLException {

		// load the graph into ram
		LevelGraph levelGraph = new LevelGraph(conn);
		conn.close();

		// compute the clustering
		// compute clustering
		System.out.println("compute clustering: ");
		System.out.println("algorithm = " + clusteringAlgorithmName);
		System.out.println("clusterSizeThreshold = " + clusterSizeThreshold);
		Clustering[] clustering;
		if (clusteringAlgorithmName.equals(CLUSTERING_ALGORITHM_QUAD_TREE)) {
			clustering = QuadTreeClusteringAlgorithm.computeClustering(levelGraph
					.getLevels(),
					levelGraph.getVertexLongitudesE6(), levelGraph.getVertexLatitudesE6(),
					QuadTreeClusteringAlgorithm.HEURISTIC_MEDIAN,
					clusterSizeThreshold);

		} else if (clusteringAlgorithmName.equals(CLUSTERING_ALGORTHM_K_CENTER)) {
			clustering = KCenterClusteringAlgorithm.computeClustering(levelGraph
					.getLevels(), clusterSizeThreshold, kcenterOversamplingFactor,
					KCenterClusteringAlgorithm.HEURISTIC_MIN_SIZE);
		} else {
			System.out.println("invalid clustering algorithm specified in properties.");
			return;
		}

		// ---------------- WRITE TEMPORARY FILES --------------------------
		System.out.println("targetFile = '" + targetFile.getAbsolutePath() + "'");

		File fBlocks = new File(targetFile.getAbsolutePath() + ".blocks");
		File fileAddressLookupTable = new File(targetFile.getAbsolutePath()
				+ ".addressLookupTable");
		File fRTree = new File(targetFile.getAbsolutePath() + ".rtree");

		// write the graphs cluster blocks
		ClusterBlockMapping mapping = new ClusterBlockMapping(clustering);
		int[] blockSize = BlockWriter.writeClusterBlocks(fBlocks, levelGraph, clustering,
				mapping, includeHopIndices);

		// write block index
		AddressLookupTableWriter.writeTable(blockSize, indexGroupSizeThreshold,
				fileAddressLookupTable);

		// construct and write r-tree (insert only level 0 clusters)
		int[] minLat = new int[clustering[0].size()];
		int[] maxLat = new int[clustering[0].size()];
		int[] minLon = new int[clustering[0].size()];
		int[] maxLon = new int[clustering[0].size()];
		int[] blockId = new int[clustering[0].size()];
		{
			int i = 0;
			for (Cluster c : clustering[0].getClusters()) {
				Rect r = ClusterUtils.getBoundingBox(c, levelGraph.getLevel(0));
				minLat[i] = r.minLatitudeE6;
				maxLat[i] = r.maxLatitudeE6;
				minLon[i] = r.minLongitudeE6;
				maxLon[i] = r.maxLongitudeE6;
				blockId[i] = mapping.getBlockId(c);
				i++;
			}
		}
		StaticRTreeWriter.packSortTileRecursive(minLon, maxLon, minLat, maxLat, blockId,
				rtreeBlockSize, fRTree, HHGlobals.STATIC_RTREE_HEADER_MAGIC);

		// ---------------- WRITE THE BINARY FILE --------------------------

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(targetFile)));

		// write header of the binary
		long startAddrGraph = HEADER_LENGTH;
		long endAddrGraph = startAddrGraph + fBlocks.length();

		long startAddrBlockIdx = endAddrGraph;
		long endAddrBlockIdx = startAddrBlockIdx + fileAddressLookupTable.length();

		long startAddrRTree = endAddrBlockIdx;
		long endAddrRTree = startAddrRTree + fRTree.length();

		out.write(HEADER_MAGIC);
		out.writeLong(startAddrGraph);
		out.writeLong(endAddrGraph);
		out.writeLong(startAddrBlockIdx);
		out.writeLong(endAddrBlockIdx);
		out.writeLong(startAddrRTree);
		out.writeLong(endAddrRTree);

		IOUtils.finalizeHeader(out, HEADER_LENGTH);

		// write components
		IOUtils.writeFilesToStream(out, new File[] { fBlocks, fileAddressLookupTable, fRTree },
				BUFFER_SIZE);

		System.out.println(out.size() + " bytes written to '" + targetFile + "'");

		out.flush();
		out.close();

		// ---------------- CLEAN UP TEMPORARY FILES --------------------------
		fBlocks.delete();
		fileAddressLookupTable.delete();
		fRTree.delete();
	}
}
