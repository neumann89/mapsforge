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

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectByteHashMap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.Rect;
import org.mapsforge.routing.preprocessing.data.ArrayUtils;
import org.mapsforge.routing.preprocessing.data.BitArrayOutputStream;
import org.mapsforge.routing.preprocessing.data.clustering.Cluster;
import org.mapsforge.routing.preprocessing.data.clustering.ClusterBlockMapping;
import org.mapsforge.routing.preprocessing.data.clustering.ClusterUtils;
import org.mapsforge.routing.preprocessing.data.clustering.Clustering;
import org.mapsforge.routing.hh.preprocessing.hierarchyComputation.HHComputation;
import org.mapsforge.routing.hh.preprocessing.mobile.LevelGraph.Level;
import org.mapsforge.routing.hh.preprocessing.mobile.LevelGraph.Level.LevelEdge;
import org.mapsforge.routing.hh.preprocessing.mobile.LevelGraph.Level.LevelVertex;
import org.mapsforge.routing.preprocessing.io.IOUtils;

class BlockWriter {

	private static final int BUFFER_SIZE = 25000000;
	private static final byte[] BUFFER = new byte[BUFFER_SIZE];
	private static final int OUTPUT_BUFFER_SIZE = 16 * 1000 * 1024;
	private static final int HEADER_LENGTH = 4096;
	private static final int MESSAGE_INTERVAL = 100;

	private static LevelGraph _levelGraph;
	private static Clustering[] _clustering;

	private static TIntIntHashMap[] mapVertexIdToVertexOffset;
	private static TObjectByteHashMap<Cluster> mapClusterToLevel;
	private static ClusterBlockMapping mapClusterToBlockId;
	private static byte bitsPerBlockId;
	private static byte bitsPerVertexOffset;
	private static byte bitsPerEdgeWeight;
	private static byte bitsPerStreetType;
	private static DijkstraAlgorithm dijkstraAlgorithm;
	private static HashMap<String, Integer> streetTypeIds;

	public static int[] writeClusterBlocks(File targetFile, LevelGraph levelGraph,
			Clustering[] clustering, ClusterBlockMapping mapping, boolean includeHopIndices)
			throws IOException {
		_levelGraph = levelGraph;
		_clustering = clustering;
		dijkstraAlgorithm = new DijkstraAlgorithm(_levelGraph);

		sortClusterVerticesByLevelAndNeighborhood(_levelGraph, clustering);

		mapVertexIdToVertexOffset = mapVertexToVertexOffset(clustering);
		mapClusterToLevel = mapClusterToLevel(clustering);
		mapClusterToBlockId = mapping;
		bitsPerBlockId = IOUtils.numBitsToEncode(0, mapClusterToBlockId.size());
		bitsPerVertexOffset = IOUtils.numBitsToEncode(0,
				ClusterUtils.maxNumVerticesPerCluster(clustering) - 1);
		bitsPerEdgeWeight = IOUtils.numBitsToEncode(0,
				getMaxEdgeWeight());
		final String[] streetTypes = levelGraph.getAllOsmStreetTypes();
		streetTypeIds = new HashMap<String, Integer>();
		for (int i = 0; i < streetTypes.length; i++) {
			streetTypeIds.put(streetTypes[i], i);
		}
		bitsPerStreetType = IOUtils.numBitsToEncode(0, streetTypes.length - 1);

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(
						targetFile), OUTPUT_BUFFER_SIZE));

		// write header
		out.writeByte(_levelGraph.numLevels());
		out.writeByte(bitsPerBlockId);
		out.writeByte(bitsPerVertexOffset);
		out.writeByte(bitsPerEdgeWeight);
		out.writeByte(bitsPerStreetType);
		out.writeBoolean(includeHopIndices);

		out.writeByte(streetTypes.length);
		for (String streetType : streetTypes) {
			if (streetType != null) {
				out.write(streetType.getBytes());
			}
			out.write(new byte[] { (byte) 0 });
		}

		IOUtils.finalizeHeader(out, HEADER_LENGTH);

		// serialize blocks only to get byte sizes
		int[] blockSize = new int[mapClusterToBlockId.size()];
		for (int blockId = 0; blockId < mapping.size(); blockId++) {
			Cluster cluster = mapping.getCluster(blockId);
			byte[] block = serializeBlock(cluster, includeHopIndices);
			blockSize[blockId] = block.length;
			if ((blockId + 1) % MESSAGE_INTERVAL == 0) {
				System.out.println("dummy write blocks " + (blockId + 1 - MESSAGE_INTERVAL)
						+ " - " + (blockId + 1));
			}
		}

		// reassign block id's by ascending byte size
		ClusterUtils.sortBySize(mapClusterToBlockId, blockSize);

		// write blocks
		for (int blockId = 0; blockId < mapping.size(); blockId++) {
			Cluster cluster = mapping.getCluster(blockId);
			byte[] block = serializeBlock(cluster, includeHopIndices);
			blockSize[blockId] = block.length;
			out.write(block);
			if ((blockId + 1) % MESSAGE_INTERVAL == 0) {
				System.out.println("write blocks " + (blockId + 1 - MESSAGE_INTERVAL) + " - "
						+ (blockId + 1));
			}
		}
		out.flush();
		out.close();

		// release resources
		_levelGraph = null;
		_clustering = null;
		mapVertexIdToVertexOffset = null;
		mapClusterToLevel = null;
		mapClusterToBlockId = null;
		dijkstraAlgorithm = null;

		return blockSize;
	}

	private static void sortClusterVerticesByLevelAndNeighborhood(LevelGraph levelGraph,
			Clustering[] clustering) {
		QuickSort quicksort = new QuickSort();
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			Level graph = levelGraph.getLevel(lvl);
			for (final Cluster c : clustering[lvl].getClusters()) {
				final int[] vertexIds = c.getVertices();
				final int[] nh = new int[vertexIds.length];
				final int[] level = new int[vertexIds.length];
				for (int i = 0; i < vertexIds.length; i++) {
					nh[i] = graph.getVertex(vertexIds[i]).getNeighborhood();
					level[i] = graph.getVertex(vertexIds[i]).getMaxLevel();
				}
				quicksort.sort(new IndexedSortable() {

					@Override
					public void swap(int i, int j) {
						ArrayUtils.swap(level, i, j);
						ArrayUtils.swap(nh, i, j);
						ArrayUtils.swap(vertexIds, i, j);
						c.swapVertices(i, j);
					}

					@Override
					public int compare(int i, int j) {
						if (level[i] != level[j]) {
							return level[j] - level[i];
						}
						return nh[i] - nh[j];
					}
				}, 0, c.size());
			}
		}
	}

	private static TIntIntHashMap[] mapVertexToVertexOffset(Clustering[] clustering) {
		TIntIntHashMap vertex2Offset[] = new TIntIntHashMap[clustering.length];
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			vertex2Offset[lvl] = new TIntIntHashMap();
			for (Cluster c : clustering[lvl].getClusters()) {
				int offs = 0;
				for (int vId : c.getVertices()) {
					vertex2Offset[lvl].put(vId, offs++);
				}
			}
		}
		return vertex2Offset;
	}

	private static TObjectByteHashMap<Cluster> mapClusterToLevel(Clustering[] clustering) {
		TObjectByteHashMap<Cluster> cluster2Level = new TObjectByteHashMap<Cluster>();
		for (byte lvl = 0; lvl < clustering.length; lvl++) {
			for (Cluster c : clustering[lvl].getClusters()) {
				cluster2Level.put(c, lvl);
			}
		}
		return cluster2Level;
	}

	private static int getMaxNeighborHood(Cluster cluster) {
		int maxNeighborhood = Integer.MIN_VALUE;
		byte level = mapClusterToLevel.get(cluster);
		Level graph = _levelGraph.getLevel(level);
		for (int vertexId : cluster.getVertices()) {
			LevelVertex v = graph.getVertex(vertexId);
			if (v.getNeighborhood() != HHComputation.INFINITY_1
					&& v.getNeighborhood() != HHComputation.INFINITY_2) {
				maxNeighborhood = Math.max(maxNeighborhood, v.getNeighborhood());
			}
		}
		return maxNeighborhood;
	}

	private static int getMaxEdgeWeight() {
		int maxEdgeWeight = Integer.MIN_VALUE;
		for (int level = 0; level < _clustering.length; level++) {
			Level graph = _levelGraph.getLevel(level);
			for (Iterator<LevelVertex> iter = graph.getVertices(); iter.hasNext();) {
				LevelVertex v = iter.next();
				for (LevelEdge e : v.getOutboundEdges()) {
					maxEdgeWeight = Math.max(maxEdgeWeight, e.getWeight());
				}
			}
		}
		return maxEdgeWeight;
	}

	private static short countVerticesTypeA(Cluster cluster) {
		short count = 0;
		int level = mapClusterToLevel.get(cluster);
		Level graph = _levelGraph.getLevel(level);
		for (int vertexId : cluster.getVertices()) {
			if (graph.getVertex(vertexId).getMaxLevel() > level) {
				count++;
			}
		}
		return count;
	}

	private static short countVerticesTypeB(Cluster cluster) {
		short count = 0;
		byte level = mapClusterToLevel.get(cluster);
		Level graph = _levelGraph.getLevel(level);
		for (int vertexId : cluster.getVertices()) {
			if (graph.getVertex(vertexId).getMaxLevel() == level
					&& graph.getVertex(vertexId).getNeighborhood() != HHComputation.INFINITY_1
					&& graph.getVertex(vertexId).getNeighborhood() != HHComputation.INFINITY_2) {
				count++;
			}
		}
		return count;
	}

	private static short countVerticesTypeC(Cluster cluster) {
		short count = 0;
		byte level = mapClusterToLevel.get(cluster);
		Level graph = _levelGraph.getLevel(level);
		for (int vertexId : cluster.getVertices()) {
			if (graph.getVertex(vertexId).getNeighborhood() == HHComputation.INFINITY_1
					|| graph.getVertex(vertexId).getNeighborhood() == HHComputation.INFINITY_2) {
				count++;
			}
		}
		return count;
	}

	private static void clearBuffer(int n) {
		for (int i = 0; i < n; i++) {
			BUFFER[i] = 0x00;
		}
	}

	private static byte[] serializeBlock(Cluster cluster, boolean includeHopIndices)
			throws IOException {

		// write everything two times,
		// during the first run, the following three values are not known
		int[] vertexEdgeOffset = new int[cluster.size()];
		int offsetStreetNames = 0;
		int offsetReferencedBlocks = 0;

		BitArrayOutputStream out = new BitArrayOutputStream(BUFFER);

		byte level = mapClusterToLevel.get(cluster);
		Rect bBox = ClusterUtils.getBoundingBox(cluster, _levelGraph.getLevel(0));
		int maxNeighborhood = getMaxNeighborHood(cluster);

		// TODO: street name serialization is only needed for level 0, will not be used for other levels
		// put all street names on a map,
		// each name is map to offset where it is stored
		byte[] streetNamesBytes;
		HashMap<String, Integer> streetNames = new HashMap<String, Integer>();
		{
			int[] vertexIds = cluster.getVertices();

			ByteArrayOutputStream arrOut = new ByteArrayOutputStream();
			DataOutputStream dArrOut = new DataOutputStream(arrOut);

			for (int vertexId : vertexIds) {
				LevelVertex v = _levelGraph.getLevel(level).getVertex(vertexId);
				for (LevelEdge e : v.getOutboundEdges()) {
					String name = e.getName();
					String ref = e.getRef();
					if (name != null && !name.equals("") && !streetNames.containsKey(name)) {
						streetNames.put(name, dArrOut.size());
						dArrOut.write(name.getBytes());
						dArrOut.writeByte(0);
					}
					if (ref != null && !ref.equals("") && !streetNames.containsKey(ref)) {
						streetNames.put(ref, dArrOut.size());
						dArrOut.write(ref.getBytes());
						dArrOut.writeByte(0);
					}
				}
			}
			dArrOut.flush();
			streetNamesBytes = arrOut.toByteArray();
		}
		// put all referenced blocks on a map
		HashMap<Integer, Integer> referencedBlocksIdx = new HashMap<Integer, Integer>();
		LinkedList<Integer> referencedBlocks = new LinkedList<Integer>();
		{
			int[] vertexIds = cluster.getVertices();
			for (int vertexId : vertexIds) {
				LevelVertex v = _levelGraph.getLevel(level).getVertex(vertexId);
				for (int j = 0; j < level; j++) {
					int blockId = mapClusterToBlockId
							.getBlockId(_clustering[j].getCluster(v.getId()));
					if (!referencedBlocksIdx.containsKey(blockId)) {
						referencedBlocksIdx.put(blockId, referencedBlocks.size());
						referencedBlocks.addLast(blockId);
					}
				}
				if (v.getMaxLevel() > level) {
					int blockId = mapClusterToBlockId.getBlockId(_clustering[level + 1]
							.getCluster(v.getId()));
					if (!referencedBlocksIdx.containsKey(blockId)) {
						referencedBlocksIdx.put(blockId, referencedBlocks.size());
						referencedBlocks.addLast(blockId);
					}
				}
				for (LevelEdge e : v.getOutboundEdges()) {
					int blockId = mapClusterToBlockId.getBlockId(_clustering[level]
							.getCluster(e.getTarget().getId()));
					if (!referencedBlocksIdx.containsKey(blockId)) {
						referencedBlocksIdx.put(blockId, referencedBlocks.size());
						referencedBlocks.addLast(blockId);
					}
				}
			}
		}

		for (int run = 0; run < 2; run++) {
			if (run == 1) {
				clearBuffer(out.getByteOffset() + 1);
				out = new BitArrayOutputStream(BUFFER);
			}

			/* BLOCK HEADER */

			// level
			out.writeByte(level);

			// number of vertices which are in next level
			out.writeShort(countVerticesTypeA(cluster));

			// number of core vertices not in next level
			out.writeShort(countVerticesTypeB(cluster));

			// number of non core vertices
			out.writeShort(countVerticesTypeC(cluster));

			// minimal latitude
			out.writeInt(bBox.minLatitudeE6);

			// minimal longitude
			out.writeInt(bBox.minLongitudeE6);

			// bits per coordinate
			byte bitsPerCoordinate = IOUtils.numBitsToEncode(bBox.minLatitudeE6,
					bBox.maxLatitudeE6);
			bitsPerCoordinate = (byte) Math.max(bitsPerCoordinate, IOUtils.numBitsToEncode(
					bBox.minLongitudeE6, bBox.maxLongitudeE6));
			out.writeByte(bitsPerCoordinate);

			// bit per neighborhood
			byte bitsPerNeighborhood = IOUtils.numBitsToEncode(0, maxNeighborhood);
			out.writeByte(bitsPerNeighborhood);

			// bits per street name offset
			byte bitsPerStreetNameOffset = IOUtils.numBitsToEncode(0, streetNamesBytes.length);
			out.writeByte(bitsPerStreetNameOffset);

			byte bitsPerIndirectBlockRef = IOUtils.numBitsToEncode(0, referencedBlocks.size());
			out.writeByte(bitsPerIndirectBlockRef);

			// offset street names
			// TODO: use a variable number of bits?
			out.writeUInt(offsetStreetNames, 24);

			// offset referencedBlocks
			// TODO: use a variable number of bits?
			out.writeUInt(offsetReferencedBlocks, 24);

			/* VERTICES */
			{
				int[] vertexIds = cluster.getVertices();
				for (int i = 0; i < vertexIds.length; i++) {
					LevelVertex v = _levelGraph.getLevel(level).getVertex(vertexIds[i]);
					// vertex id's of lower levels
					for (int j = 0; j < level; j++) {
						int blockId = mapClusterToBlockId
								.getBlockId(_clustering[j].getCluster(v.getId()));
						int vertexOffset = mapVertexIdToVertexOffset[j].get(v.getId());
						out.writeUInt(referencedBlocksIdx.get(blockId),
								bitsPerIndirectBlockRef);
						out.writeUInt(vertexOffset, bitsPerVertexOffset);
					}

					// vertex id of next level
					if (v.getMaxLevel() > level) {
						int blockId = mapClusterToBlockId.getBlockId(_clustering[level + 1]
								.getCluster(v.getId()));
						int vertexOffset = mapVertexIdToVertexOffset[level + 1].get(v.getId());
						out.writeUInt(referencedBlocksIdx.get(blockId),
								bitsPerIndirectBlockRef);
						out.writeUInt(vertexOffset, bitsPerVertexOffset);

					}

					// neighborhood
					if (v.getNeighborhood() != HHComputation.INFINITY_1
							&& v.getNeighborhood() != HHComputation.INFINITY_2) {
						out.writeUInt(v.getNeighborhood(), bitsPerNeighborhood);
					}

					// first outbound edge Offset
					out.writeInt(vertexEdgeOffset[i]);

					// coordinate
					if (v.getLevel() == 0) {
						out.writeUInt(v.getCoordinate().getLatitudeE6() - bBox.minLatitudeE6,
								bitsPerCoordinate);
						out.writeUInt(v.getCoordinate().getLongitudeE6() - bBox.minLongitudeE6,
								bitsPerCoordinate);
					}
				}
			}

			/* EDGES */
			{
				int[] vertexIds = cluster.getVertices();
				for (int i = 0; i < vertexIds.length; i++) {
					LevelVertex v = _levelGraph.getLevel(level).getVertex(vertexIds[i]);
					vertexEdgeOffset[i] = (out.getByteOffset() * 8) + out.getBitOffset();
					// number of edges (4 bits if less than 15, else escape and append a 24 bit
					// value)
					if (v.getOutboundEdges().length < 15) {
						out.writeUInt(v.getOutboundEdges().length, 4);
					} else {
						// escape by 0xff
						out.writeUInt(15, 4);
						// write number of edges as 24bit unsigned integer
						out.writeUInt(v.getOutboundEdges().length, 24);
					}

					for (LevelEdge e : v.getOutboundEdges()) {
						// weight
						out.writeUInt(e.getWeight(), bitsPerEdgeWeight);
						// is internal
						boolean isInternal = _clustering[level].getCluster(
								e.getTarget().getId()).equals(cluster);
						out.writeBit(isInternal);

						// target id
						int blockId = mapClusterToBlockId.getBlockId(_clustering[level]
								.getCluster(e.getTarget().getId()));
						int vertexOffset = mapVertexIdToVertexOffset[level].get(e.getTarget()
								.getId());
						if (!isInternal) {
							out.writeUInt(referencedBlocksIdx.get(blockId),
									bitsPerIndirectBlockRef);
						}
						out.writeUInt(vertexOffset, bitsPerVertexOffset);

						// is forward
						out.writeBit(e.isForward());

						// is backward
						out.writeBit(e.isBackward());

						// is core
						out
								.writeBit(e.getSource().getNeighborhood() != HHComputation.INFINITY_1
										&& e.getTarget().getNeighborhood() != HHComputation.INFINITY_1);

						if (level == 0 && e.isForward()) {
							// osm street type
							out.writeUInt(streetTypeIds.get(e.getOsmStreetType()),
									bitsPerStreetType);

							// isRoundabout
							out.writeBit(e.isRoundabout());

							// hasName
							boolean hasName = e.getName() != null && !e.getName().equals("");
							out.writeBit(hasName);

							// hasRef
							boolean hasRef = e.getRef() != null && !e.getRef().equals("");
							out.writeBit(hasRef);

							// name byte offset from block start
							if (hasName) {
								int nameOffset = streetNames.get(e.getName());
								out.writeUInt(nameOffset, bitsPerStreetNameOffset);
							}

							// ref byte offset from block start
							if (hasRef) {
								int refOffset = streetNames.get(e.getRef());
								out.writeUInt(refOffset, bitsPerStreetNameOffset);
							}

							// way points
							GeoCoordinate[] waypoints = e.getWaypoints();
							if (waypoints.length < 15) {
								out.writeUInt(waypoints.length, 4);
							} else {
								// escape length field by 0xf and write 16 bit length field
								out.writeUInt(15, 4);
								out.writeUInt(waypoints.length, 16);
							}
							for (GeoCoordinate c : waypoints) {
								out.writeUInt(c.getLatitudeE6() - bBox.minLatitudeE6,
										bitsPerCoordinate);
								out.writeUInt(c.getLongitudeE6() - bBox.minLongitudeE6,
										bitsPerCoordinate);
							}
						}

						// lowest level this edge belongs to level
						if (level > 0) {
							out.writeByte((byte) e.getMinLevel());
						}

						// write hop indices for expanding shortcuts
						if (e.getMinLevel() > 0 && includeHopIndices) {
							LinkedList<Integer> hopIndices = new LinkedList<Integer>();
							dijkstraAlgorithm.getShortestPath(e.getSource().getId(), e
									.getTarget().getId(), e.getMinLevel() - 1,
									new LinkedList<LevelVertex>(), hopIndices, e.isForward(), e
											.isBackward(), true);
							if (hopIndices.size() < 31) {
								out.writeUInt(hopIndices.size(), 5);
							} else {
								out.writeUInt(31, 5);
								out.writeUInt(hopIndices.size(), 8);
							}

							for (int hopIdx : hopIndices) {
								if (hopIdx < 15) {
									out.writeUInt(hopIdx, 4);
								} else {
									out.writeUInt(15, 4);
									out.writeUInt(hopIdx, 24);
								}
							}
							LevelVertex _v = _levelGraph.getLevel(e.getMinLevel() - 1)
									.getVertex(e.getSource().getId());
							for (Integer hopIndex : hopIndices) {
								LevelEdge _e = _v.getOutboundEdges()[hopIndex];
								_v = _e.getTarget();
							}
						}
					}
				}
			}

			/* STREET NAMES */
			out.alignPointer(1);
			offsetStreetNames = out.getByteOffset();
			if (level == 0) {
				out.write(streetNamesBytes);
			}

			/* REFERENCED BLOCKS */
			out.alignPointer(1);
			offsetReferencedBlocks = out.getByteOffset();
			for (Integer blockId : referencedBlocks) {
				out.writeUInt(blockId, bitsPerBlockId);
			}
			out.alignPointer(1);
		}

		// copy written data from buffer and return it
		out.alignPointer(1);
		byte[] result = new byte[out.getByteOffset()];
		System.arraycopy(BUFFER, 0, result, 0, result.length);

		// clear buffer
		clearBuffer(out.getByteOffset());
		return result;
	}
}
