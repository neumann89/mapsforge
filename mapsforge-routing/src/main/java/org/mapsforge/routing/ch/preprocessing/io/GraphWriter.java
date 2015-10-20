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

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.Rect;
import org.mapsforge.routing.ch.preprocessing.data.PathCombiner;
import org.mapsforge.routing.ch.preprocessing.graph.CHEdge;
import org.mapsforge.routing.ch.preprocessing.graph.CHGraph;
import org.mapsforge.routing.ch.preprocessing.graph.CHVertex;
import org.mapsforge.routing.ch.preprocessing.graph.ShortcutPath;
import org.mapsforge.routing.preprocessing.data.ArrayUtils;
import org.mapsforge.routing.preprocessing.data.BitArrayOutputStream;
import org.mapsforge.routing.preprocessing.data.clustering.Cluster;
import org.mapsforge.routing.preprocessing.data.clustering.ClusterBlockMapping;
import org.mapsforge.routing.preprocessing.data.clustering.ClusterUtils;
import org.mapsforge.routing.preprocessing.data.clustering.Clustering;
import org.mapsforge.routing.preprocessing.io.IOUtils;

/**
 * Serializer and writer for a Contraction Hierarchies graph.
 * 
 * @author Patrick Jungermann
 * @version $Id: GraphWriter.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
class GraphWriter {

	/**
	 * Class-level logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(GraphWriter.class.getName());

	/**
	 * The Flash space's block size. This <em>block</em> is <strong>not</strong> related to a graph's
	 * block (serialized cluster).
	 * 
	 * TODO: make customizable?
	 */
	private static final int BLOCK_SIZE = 4096;
	/**
	 * The output buffer's size.
	 */
	private static final int OUTPUT_BUFFER_SIZE = 4000 * BLOCK_SIZE;
	/**
	 * The size of the buffer, related to the block serialization.
	 * 
	 * TODO: make customizable?
	 */
	private static final int BLOCK_BUFFER_SIZE = 100 * BLOCK_SIZE;

	/**
	 * The graph, which has to be serialized.
	 */
	private final CHGraph graph;
	/**
	 * The clustering of the graph, which will be used for the serialization.
	 */
	private final Clustering clustering;
	/**
	 * The cluster-to-block mapping.
	 */
	private final ClusterBlockMapping mapping;

	/**
	 * All street types.
	 */
	private final String[] streetTypes;
	/**
	 * The number of bits used to encode one block ID.
	 */
	private final int bitsPerBlockId;
	/**
	 * The number of bits used to encode one vertex offset.
	 */
	private final int bitsPerVertexOffset;
	/**
	 * The number of bits used to encode one edge weight.
	 */
	private final int bitsPerEdgeWeight;
	/**
	 * The number of bits used to encode one street type reference.
	 */
	private final int bitsPerStreetType;

	/**
	 * Mapping from a vertex identifier to vertex offset (a relative identifier, related to its block).
	 */
	private final int[] vertexIdToVertexOffset;

	/**
	 * Whether the debug mode is enabled or not.
	 */
	private boolean debug = false;

	/**
	 * Constructs a writer / serializer for the graph.
	 * 
	 * @param graph
	 *            The graph, which has to be serialized.
	 * @param clustering
	 *            The clustering of the graph, which will be used for the serialization.
	 * @param mapping
	 *            The cluster-to-block mapping.
	 */
	public GraphWriter(final CHGraph graph, final Clustering clustering,
			final ClusterBlockMapping mapping) {
		this.graph = graph;
		this.clustering = clustering;
		this.mapping = mapping;

		streetTypes = graph.getOsmStreetTypes();

		bitsPerBlockId = IOUtils.numBitsToEncode(0, clustering.size() - 1);
		bitsPerVertexOffset = IOUtils.numBitsToEncode(0,
				ClusterUtils.maxNumVerticesPerCluster(clustering) - 1);
		bitsPerEdgeWeight = IOUtils.numBitsToEncode(0, maxEdgeWeight());
		bitsPerStreetType = IOUtils.numBitsToEncode(0, streetTypes.length - 1);

		vertexIdToVertexOffset = mapVertexIdToVertexOffset();
	}

	/**
	 * Activates or deactivates the debug mode. In debug mode, the serializer will some more data like
	 * the original identifiers of vertices and edges.
	 * 
	 * @param debug
	 *            Whether to enable or disable the debug mode.
	 */
	public void setDebug(final boolean debug) {
		this.debug = debug;
	}

	/**
	 * Returns the maximum of all edge weights of the graph.
	 * 
	 * @return The maximum of all edge weights.
	 */
	private int maxEdgeWeight() {
		int maxWeight = 0; // no negative values allowed..

		final Iterator<? extends CHVertex> iterator = graph.getVertices();
		while (iterator.hasNext()) {
			final CHVertex vertex = iterator.next();

			for (final CHEdge edge : vertex.getEdgesFromOrToHigherVertices()) {
				maxWeight = Math.max(maxWeight, edge.getWeight());
			}
		}

		return maxWeight;
	}

	/**
	 * Creates and returns the mapping from a vertex ID to a vertex offset.
	 * 
	 * @return The mapping from a vertex ID to a vertex offset.
	 */
	protected int[] mapVertexIdToVertexOffset() {
		final int[] mapping = new int[graph.numVertices()];

		for (final Cluster cluster : clustering.getClusters()) {
			int offset = 0;
			for (final int vertexId : cluster.getVertices()) {
				mapping[vertexId] = offset++;
			}
		}

		return mapping;
	}

	/**
	 * Writes the serialized graph to the selected target file.
	 * 
	 * @param targetFile
	 *            The target file, to which the graph serialization will be written.
	 * @return The number of bytes per written block, from which each one is related to one cluster.
	 * @throws java.io.IOException
	 *             if there was any problem with writing the data to the target file.
	 */
	public int[] write(final File targetFile) throws IOException {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(targetFile), OUTPUT_BUFFER_SIZE));

			writeHeader(out);
			return writeBlocks(out);

		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Writes the binary file's header to the output stream.
	 * 
	 * @param out
	 *            The output stream, to which all the data has to be written.
	 * @throws java.io.IOException
	 *             if there was any problem with writing the header to the stream.
	 */
	protected void writeHeader(final DataOutputStream out) throws IOException {
		out.writeBoolean(debug);
		out.writeByte(bitsPerBlockId);
		out.writeByte(bitsPerVertexOffset);
		out.writeByte(bitsPerEdgeWeight);
		out.writeByte(bitsPerStreetType);

		writeStreetTypes(out);

		// fill header with empty data
		IOUtils.finalizeHeader(out, HeaderGlobals.GRAPH_BLOCKS_HEADER_LENGTH);
	}

	/**
	 * Writes all street types to the output stream.
	 * 
	 * @param out
	 *            The output stream, to which all the data has to be written.
	 * @throws java.io.IOException
	 *             if there was any problem with writing the street types to the stream.
	 */
	protected void writeStreetTypes(final DataOutputStream out) throws IOException {
		// write #streetTypes
		out.writeByte(streetTypes.length);

		// write each street type, followed by a 0-byte
		for (String streetType : streetTypes) {
			if (streetType != null) {
				out.write(streetType.getBytes("UTF-8"));
			}
			out.write((byte) 0);
		}
	}

	/**
	 * Writes all blocks (serialized clusters) to the output stream.
	 * 
	 * @param out
	 *            The output stream, to which all the data has to be written.
	 * @return The number of bytes per written block, from which each one is related to one cluster.
	 * @throws java.io.IOException
	 *             if there was any problem with writing the blocks to the stream.
	 */
	protected int[] writeBlocks(final DataOutputStream out) throws IOException {
		final int[] blockSizes = new int[mapping.size()];

		// two iterations needed:
		// 1: retrieve the blockSize (suppressed writing) + reordering
		// 2: write the block
		int runs = 2;
		do {
			for (int blockId = 0; blockId < mapping.size(); blockId++) {
				final Cluster cluster = mapping.getCluster(blockId);
				blockSizes[blockId] = writeBlock(out, cluster, runs == 2);
			}

			if (runs == 2) {
				ClusterUtils.sortBySize(mapping, blockSizes);
			}

		} while (--runs > 0);

		if (LOGGER.isLoggable(Level.INFO)) {
			final int sum = ArrayUtils.sum(blockSizes);
			Arrays.sort(blockSizes);
			LOGGER.info("block size stats: {min=" + blockSizes[0] + ", max="
					+ blockSizes[blockSizes.length - 1] + " avg=" + (sum * 1f / blockSizes.length)
					+ "}");
		}

		return blockSizes;
	}

	/**
	 * Writes the data of one block (serialized cluster) to the output stream.
	 * 
	 * 
	 * @param out
	 *            The output stream, to which all the data has to be written.
	 * @param cluster
	 *            The cluster, which has to be serialized.
	 * @param suppressWriting
	 *            Whether the actual writing of the block should be suppressed (only simulation), or not
	 *            (writing).
	 * @return The number of bytes, which has been written to the stream.
	 * @throws java.io.IOException
	 *             if there was any problem with the serialization or the writing.
	 */
	protected int writeBlock(final DataOutputStream out, final Cluster cluster,
			final boolean suppressWriting) throws IOException {
		final byte[] block = new BlockSerializer(cluster).serialize();

		if (!suppressWriting) {
			out.write(block);

			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine(String.format("#vertices: %d, blockSize: %d, ratio: %f",
						cluster.size(), block.length, block.length * 1d / cluster.size()));
			}
		}

		return block.length;
	}

	/**
	 * Serializer for creating binary block out of {@link Cluster}s.
	 * 
	 * @author Patrick Jungermann
	 * @version $Id: GraphWriter.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
	 */
	class BlockSerializer {

		private final int blockId;
		private final int[] vertexIds;
		private final Rect boundingBox;

		private final int[] offsetsEdgesPerVertex;

		private final TObjectIntHashMap<String> streetNameOffsets;
		private final byte[] streetNamesBytes;

		private final byte bitsPerCoordinate;
		private final byte bitsPerStreetNamesOffset;
		private final byte bitsPerShortcutPathLength;
		private byte bitsPerShortcutPathOffset = 15;
		private byte bitsPerEdgeOffset = 15;

		private final HashSet<CHEdge> externalEdges = new HashSet<CHEdge>();
		private int numShortcutPaths = 0;
		private final TIntObjectHashMap<int[]> externalShortcutPaths = new TIntObjectHashMap<int[]>();
		private final PathCombiner shortcutPathCombiner;

		/**
		 * Constructs a serializer for that cluster object.
		 * 
		 * @param cluster
		 *            The graph cluster, which should be serialized to a block.
		 * @throws IOException
		 *             if there was a problem with the initialization.
		 */
		public BlockSerializer(final Cluster cluster) throws IOException {
			blockId = mapping.getBlockId(cluster);
			vertexIds = cluster.getVertices();

			collectShortcutPathsAndExternalEdges();

			boundingBox = getBoundingBox(cluster);

			offsetsEdgesPerVertex = new int[cluster.size()];

			bitsPerCoordinate = (byte) Math.max(
					IOUtils.numBitsToEncode(boundingBox.getMinLongitudeE6(),
							boundingBox.getMaxLongitudeE6()),
					IOUtils.numBitsToEncode(boundingBox.getMinLatitudeE6(),
							boundingBox.getMaxLatitudeE6()));

			streetNameOffsets = new TObjectIntHashMap<String>(cluster.size());
			streetNamesBytes = serializeStreetNames();
			bitsPerStreetNamesOffset = IOUtils.numBitsToEncode(0, streetNamesBytes.length);

			// combine paths, where possible
			shortcutPathCombiner = new PathCombiner(externalShortcutPaths);
			shortcutPathCombiner.combinePaths();

			bitsPerShortcutPathLength = IOUtils.numBitsToEncode(0, maxShortcutPathLength());

			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("block " + blockId + " -- #externalEdges: " + externalEdges.size()
						+ "; #shortcut_paths: " + numShortcutPaths + "; #external_shortcut_paths: "
						+ externalShortcutPaths.size());
			}
		}

		/**
		 * Returns the block's bounding box.
		 * 
		 * @param cluster
		 *            The block's cluster.
		 * @return The block's bounding box.
		 */
		protected Rect getBoundingBox(Cluster cluster) {
			Rect rect = ClusterUtils.getBoundingBox(cluster, graph);

			for (final int vertexId : vertexIds) {
				final CHVertex vertex = graph.getVertex(vertexId);
				for (final CHEdge edge : vertex.getEdgesFromOrToHigherVertices()) {
					extendBy(rect, edge);
				}
			}

			for (final CHEdge edge : externalEdges) {
				extendBy(rect, edge);
			}

			return rect;
		}

		/**
		 * Extends the rectangular with the given edge.
		 * 
		 * @param rect
		 *            The current rectangular region.
		 * @param edge
		 *            The edge, which has to be included by the region.
		 */
		protected void extendBy(final Rect rect, final CHEdge edge) {
			for (final GeoCoordinate coordinate : edge.getWaypoints()) {
				extendBy(rect, coordinate);
			}
		}

		/**
		 * Extends the rectangular with the given geographical point.
		 * 
		 * @param rect
		 *            The current rectangular region.
		 * @param point
		 *            The point, which has to be included by the region.
		 */
		protected void extendBy(final Rect rect, final GeoCoordinate point) {
			final int latitudeE6 = point.getLatitudeE6();
			if (latitudeE6 < rect.minLatitudeE6) {
				rect.minLatitudeE6 = latitudeE6;
			}
			if (latitudeE6 > rect.maxLatitudeE6) {
				rect.maxLatitudeE6 = latitudeE6;
			}

			final int longitudeE6 = point.getLongitudeE6();
			if (longitudeE6 < rect.minLongitudeE6) {
				rect.minLongitudeE6 = longitudeE6;
			}
			if (longitudeE6 > rect.maxLongitudeE6) {
				rect.maxLongitudeE6 = longitudeE6;
			}
		}

		/**
		 * Calculates and returns the external shortcuts' max. path length.
		 * 
		 * @return The external shortcuts' max. path length.
		 */
		protected int maxShortcutPathLength() {
			int max = 0;

			for (final int key : externalShortcutPaths.keys()) {
				final int[] path = externalShortcutPaths.get(key);
				if (path.length > max) {
					max = path.length;
				}
			}

			return max;
		}

		/**
		 * Collect all shortcut paths and external edges and stores them to the global fields.
		 */
		protected void collectShortcutPathsAndExternalEdges() {
			for (final int vertexId : vertexIds) {
				collectShortcutPathsAndExternalEdges(graph.getVertex(vertexId));
			}
		}

		/**
		 * Collect all shortcut paths and external edges related to this vertex and stores them to the
		 * global fields.
		 * 
		 * @param vertex
		 *            The vertex, from which the edges has to be checked.
		 */
		protected void collectShortcutPathsAndExternalEdges(final CHVertex vertex) {
			final CHEdge[] edges = vertex.getEdgesFromOrToHigherVertices();

			for (final CHEdge edge : edges) {
				collectShortcutPathAndExternalEdges(edge);
			}
		}

		/**
		 * Checks, if this edge is a (internal/external) shortcut and collects all of its external edges
		 * and stores both to the global fields.
		 * 
		 * @param edge
		 *            The edge, which has to be checked.
		 */
		protected void collectShortcutPathAndExternalEdges(final CHEdge edge) {
			if (edge.isShortcut()) {
				// get the unpacked shortcut path
				// starting from the lowest vertex for forward/forward+backward edges
				// and from the highest for backward only edges (its source)
				final ShortcutPath shortcutPath = edge.unpack(
						edge.isUndirected() ? edge.getLowestVertexId() : edge.getSourceId());

				boolean isExternal = false;

				// to unpack an internal shortcut edge, we have to unpack each(!)
				// shortcut edge on the way of resolving the original path
				// -> all of these shortcut edges have to be part of the same block!
				for (CHEdge shortcutEdge : shortcutPath.getUsedShortcuts()) {
					// edges will always be written into the block,
					// which contains their lower endpoint (source or target)
					// (independent from their direction)
					final int relatedVertexId = shortcutEdge.getLowestVertexId();
					final int relatedBlockId = mapping.getBlockId(
							clustering.getCluster(relatedVertexId));

					// it is an external shortcut, iff at least one of the used shortcut edges is part
					// of a different cluster (stored at a different block)
					if (blockId != relatedBlockId) {
						isExternal = true;
						break;
					}
				}

				// also each edge of the shortcut's unpacked path has to be part of the same cluster /
				// logical block to be an internal shortcut edge
				final CHEdge[] path = shortcutPath.getPath();

				// collect all external edges, they will be written later (fewer redundancy)
				// and get the array of edge IDs
				final int[] idBasedPath = new int[path.length];

				for (int i = 0; i < path.length; i++) {
					final CHEdge pathEdge = path[i];
					idBasedPath[i] = pathEdge.getId();

					// edges will always be written into the block,
					// which contains their lower endpoint (source or target)
					// (independent from their direction)
					final int relatedVertexId = pathEdge.getLowestVertexId();
					final int relatedBlockId = mapping.getBlockId(
							clustering.getCluster(relatedVertexId));

					if (blockId != relatedBlockId) {
						isExternal = true;
						externalEdges.add(pathEdge);
					}
				}

				// store the path of edge IDs
				numShortcutPaths++;
				if (isExternal) {
					externalShortcutPaths.put(edge.getId(), idBasedPath);
				}
			}
		}

		/**
		 * Serializes all street names and reference names of this cluster into a byte array and stores
		 * all offsets into the given map.
		 * 
		 * @return The byte array, representing all street names etc. of this cluster.
		 * @throws java.io.IOException
		 *             if there was any problem related to the serialization.
		 */
		protected byte[] serializeStreetNames() throws IOException {
			final ByteArrayOutputStream arrOut = new ByteArrayOutputStream();
			final DataOutputStream out = new DataOutputStream(arrOut);

			for (final int vertexId : vertexIds) {
				final CHVertex vertex = graph.getVertex(vertexId);

				for (final CHEdge edge : vertex.getEdgesFromOrToHigherVertices()) {
					writeStreetName(out, edge.getName());
					writeStreetName(out, edge.getRef());
				}

				for (final CHEdge edge : externalEdges) {
					writeStreetName(out, edge.getName());
					writeStreetName(out, edge.getRef());
				}
			}

			return arrOut.toByteArray();
		}

		/**
		 * Writes a street name to the stream, if there is any writable data and if it was not written
		 * before. The offset of a written street name will be stored to {@link #streetNameOffsets}.
		 * 
		 * @param out
		 *            The output stream, to which the data will be written.
		 * @param name
		 *            The name, which has to be written.
		 * @throws java.io.IOException
		 *             if there was a problem with writing the name to the stream.
		 */
		protected void writeStreetName(final DataOutputStream out, final String name)
				throws IOException {

			if (name != null && !name.isEmpty() && !streetNameOffsets.containsKey(name)) {
				streetNameOffsets.put(name, out.size());

				out.write(name.getBytes("UTF-8"));
				out.writeByte((byte) 0);
			}
		}

		/**
		 * Serialized the graph's cluster into its binary representation.
		 * 
		 * @return The cluster's serialization.
		 * @throws java.io.IOException
		 *             if there was any problem with the serialization.
		 */
		public byte[] serialize() throws IOException {
			byte bitsPerVertexEdgesOffset = (byte) 0;
			byte bitsPerVertexEdgesOffsetOld;
			int[] offsetEdgesPerVertexOld;

			int streetNamesOffset = 0;

			byte[] buffer;
			BitArrayOutputStream out;

			// - first run needed to calculate the offsets etc.,
			// - second to write all data
			// - for some blocks, three runs are needed, because the edge offsets have changed
			// because of a different number of bits, to store them, calculated at the end
			// - if the number of bits are the same, then the correct space was used
			// (may result in additional runs)
			// - because the vertex edges' offsets will be re-calculated after they have been written,
			// a check for their equality is needed (may result in additional runs)
			int runs = 3;
			do {
				bitsPerVertexEdgesOffsetOld = bitsPerVertexEdgesOffset;
				offsetEdgesPerVertexOld = offsetsEdgesPerVertex.clone();

				buffer = new byte[BLOCK_BUFFER_SIZE];
				out = new BitArrayOutputStream(buffer);

				writeHeader(out, streetNamesOffset, bitsPerVertexEdgesOffset);
				writeVertices(out, bitsPerVertexEdgesOffset);
				writeEdges(out);

				// street names
				out.alignPointer(1);
				streetNamesOffset = out.getByteOffset();
				out.write(streetNamesBytes);

				// num of bits
				bitsPerVertexEdgesOffset = IOUtils.numBitsToEncode(0,
						ArrayUtils.max(offsetsEdgesPerVertex));

			} while (--runs > 0
					|| bitsPerVertexEdgesOffset != bitsPerVertexEdgesOffsetOld
					|| !Arrays.equals(offsetsEdgesPerVertex, offsetEdgesPerVertexOld));

			out.alignPointer(1); // align pointer to full bytes, if not yet done
			final byte[] block = new byte[out.getByteOffset()];
			System.arraycopy(buffer, 0, block, 0, block.length);

			return block;
		}

		/**
		 * Writes a block's header to the output stream.
		 * 
		 * @param out
		 *            The output stream, to which all the data has to be written.
		 * @param streetNamesOffset
		 *            Offset of the street names within the related block.
		 * @param bitsPerVertexEdgesOffset
		 *            Number of bits used to encode one offset to a block of edges.
		 * @throws java.io.IOException
		 *             if there was any problem with writing the data to the stream.
		 */
		protected void writeHeader(final BitArrayOutputStream out, final int streetNamesOffset,
				final byte bitsPerVertexEdgesOffset) throws IOException {
			// #vertices
			out.writeShort((short) vertexIds.length);

			// min. latitude and longitude
			out.writeInt(boundingBox.getMinLatitudeE6());
			out.writeInt(boundingBox.getMinLongitudeE6());

			// number of bits used to encode the different parts
			out.writeByte(bitsPerCoordinate);
			out.writeByte(bitsPerVertexEdgesOffset);
			out.writeByte(bitsPerStreetNamesOffset);
			out.writeByte(bitsPerShortcutPathOffset);
			out.writeByte(bitsPerShortcutPathLength);
			out.writeByte(bitsPerEdgeOffset);

			// offset of the street names
			// TODO: use a variable number of bits?
			out.writeUInt(streetNamesOffset, 24);
		}

		/**
		 * Writes all vertices to the output stream.
		 * 
		 * @param out
		 *            The target output stream.
		 * @param bitsPerEdgesOffset
		 *            The number of bits used to encode the offset of its edges.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeVertices(final BitArrayOutputStream out,
				final byte bitsPerEdgesOffset) throws IOException {

			for (int i = 0; i < vertexIds.length; i++) {
				writeVertex(out, graph.getVertex(vertexIds[i]), offsetsEdgesPerVertex[i],
						bitsPerEdgesOffset);
			}
		}

		/**
		 * Writes the vertex to the output stream.
		 * 
		 * @param out
		 *            The target output stream.
		 * @param vertex
		 *            The vertex, which has to be written to the stream.
		 * @param offsetEdges
		 *            The offset of its edges.
		 * @param bitsPerEdgesOffset
		 *            The number of bits used to encode the offset of its edges.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeVertex(final BitArrayOutputStream out, final CHVertex vertex,
				final int offsetEdges, final byte bitsPerEdgesOffset) throws IOException {
			// the coordinates will be stored relatively to the cluster's bounding box' min. values
			writeCoordinate(out, vertex.getCoordinate());

			// offsets to the start of out- and ingoing edges of this vertex
			out.writeUInt(offsetEdges, bitsPerEdgesOffset);

			// additional debug data - not included per default
			if (debug) {
				out.writeInt(vertex.getId());
			}
		}

		/**
		 * Writes all edges to the output stream.
		 * 
		 * @param out
		 *            The target output stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the output stream.
		 */
		protected void writeEdges(final BitArrayOutputStream out) throws IOException {
			final TIntIntHashMap shortcutPathOffsets = new TIntIntHashMap(200, 0.5f, -1, -1);
			final TIntIntHashMap edgeOffsets = new TIntIntHashMap(200, 0.5f, -1, -1);

			final byte[] buffer = new byte[BLOCK_BUFFER_SIZE];
			BitArrayOutputStream dummy = null;
			byte bitsPerShortcutPathOffsetOld, bitsPerEdgeOffsetOld;
			int[] edgeOffsetsValuesOld, shortcutPathOffsetsValuesOld;

			int runs = 3;
			do {
				edgeOffsetsValuesOld = edgeOffsets.values();
				shortcutPathOffsetsValuesOld = shortcutPathOffsets.values();

				if (runs > 1) {
					dummy = new BitArrayOutputStream(buffer);
					// adjust the position to out's position
					dummy.write(new byte[out.getByteOffset()]);
					dummy.writeUInt(0, out.getBitOffset());
				}

				// shortcut path offsets are unknown at the first iteration, a second or third one is
				// maybe needed
				writeEdgesFromOrToHigherVertices(runs > 1 ? dummy : out, shortcutPathOffsets,
						edgeOffsets);

				// store the shortcut related data
				// 1) external edges
				writeExternalEdges(runs > 1 ? dummy : out, edgeOffsets);

				// track changes: more iterations needed?
				bitsPerEdgeOffsetOld = bitsPerEdgeOffset;
				// calculate new value
				final int maxEdgeOffset = ArrayUtils.max(edgeOffsets.values());
				bitsPerEdgeOffset = IOUtils.numBitsToEncode(0, maxEdgeOffset);
				if (runs == 2 && bitsPerEdgeOffset != bitsPerEdgeOffsetOld) {
					runs = 3;
				}

				// 2) shortcut paths
				writeShortcutPaths(runs > 1 ? dummy : out, shortcutPathOffsets, edgeOffsets);

				// track changes: more iterations needed?
				bitsPerShortcutPathOffsetOld = bitsPerShortcutPathOffset;
				// calculate new value
				bitsPerShortcutPathOffset = IOUtils.numBitsToEncode(0,
						ArrayUtils.max(shortcutPathOffsets.values()));

				if (runs == 2 && (bitsPerShortcutPathOffset != bitsPerShortcutPathOffsetOld
						|| !Arrays.equals(edgeOffsetsValuesOld, edgeOffsets.values())
						|| !Arrays.equals(shortcutPathOffsetsValuesOld, shortcutPathOffsets.values()))) {
					runs = 3;
				}

			} while (--runs > 0);
		}

		/**
		 * Writes all edges to the output stream.
		 * 
		 * @param out
		 *            The target output stream.
		 * @param shortcutPathOffsets
		 *            The all shortcut paths' offsets.
		 * @param edgeOffsets
		 *            The offsets of all edges, to which their offsets will be added.
		 * @throws IOException
		 *             if there was a problem with writing the data to the output stream.
		 */
		protected void writeEdgesFromOrToHigherVertices(final BitArrayOutputStream out,
				final TIntIntHashMap shortcutPathOffsets, final TIntIntHashMap edgeOffsets)
				throws IOException {

			for (int i = 0; i < vertexIds.length; i++) {
				final int vertexId = vertexIds[i];

				out.alignPointer(1); // align pointer to full bytes, if not yet done
				offsetsEdgesPerVertex[i] = out.getByteOffset();

				final CHEdge[] edges = graph.getVertex(vertexId).getEdgesFromOrToHigherVertices();
				writeEscapableNumber(out, edges.length, 24);

				for (final CHEdge edge : edges) {
					writeEdge(out, edge, edgeOffsets, shortcutPathOffsets);
				}
			}
		}

		/**
		 * Writes all external edges, which are part of one of the external shortcut paths to the output
		 * stream.
		 * 
		 * @param out
		 *            The target output stream.
		 * @param edgeOffsets
		 *            The offsets of all edges, to which their offsets will be added.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeExternalEdges(final BitArrayOutputStream out,
				final TIntIntHashMap edgeOffsets) throws IOException {

			for (final CHEdge edge : externalEdges) {
				writeEdge(out, edge, edgeOffsets, null);
			}
		}

		/**
		 * Writes all shortcut paths as list of edge offsets to the output stream.
		 * 
		 * @param out
		 *            The target output stream.
		 * @param shortcutPathOffsets
		 *            The offsets of all shortcuts, to which their offsets will be added.
		 * @param edgeOffsets
		 *            The offset of all edges.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeShortcutPaths(final BitArrayOutputStream out,
				final TIntIntHashMap shortcutPathOffsets, final TIntIntHashMap edgeOffsets)
				throws IOException {
			// write each path
			for (final int pathId : shortcutPathCombiner.getPathIds()) {
				final int[] path = shortcutPathCombiner.getPath(pathId);
				final int[][] shortcutsPerStartIndex = shortcutPathCombiner
						.getPathKeysByPathId(pathId);

				for (int index = 0; index < path.length; index++) {
					// save the BIT offset of the path part for each starting shortcut
					for (final int shortcutId : shortcutsPerStartIndex[index]) {
						shortcutPathOffsets.put(shortcutId,
								8 * out.getByteOffset() + out.getBitOffset());
					}

					// get and check the offset
					final int offset = edgeOffsets.get(path[index]);
					if (offset == -1) {
						throw new RuntimeException(
								String.format(
										"Not able to create the binary file. There was no offset for the edge %d at block %d",
										path[index], blockId
										));
					}

					// write the path's edge's offset
					out.writeUInt(offset, bitsPerEdgeOffset);
				}
			}
		}

		/**
		 * Writes an edge to the output stream.
		 * 
		 * @param out
		 *            The target output stream.
		 * @param edge
		 *            The edge, which has to be written to the stream.
		 * @param edgeOffsets
		 *            The offsets of all edges, to which its offset will be written.
		 * @param shortcutPathOffsets
		 *            The offsets of all shortcut paths.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeEdge(final BitArrayOutputStream out, final CHEdge edge,
				final TIntIntHashMap edgeOffsets, final TIntIntHashMap shortcutPathOffsets)
				throws IOException {

			final boolean shortcut = edge.isShortcut();
			if (!shortcut) {
				// store the BIT offset for "normal" edges
				edgeOffsets.put(edge.getId(), 8 * out.getByteOffset() + out.getBitOffset());
			}

			// additional debug data - not included per default
			if (debug) {
				out.writeInt(edge.getId());
			}

			final int lowestVertexId = edge.getLowestVertexId();
			final int highestVertexId = edge.getHighestVertexId();

			// forward? / backward?
			final boolean forward = edge.getTargetId() == highestVertexId || edge.isUndirected();
			final boolean backward = edge.getSourceId() == highestVertexId || edge.isUndirected();
			out.writeBit(forward);
			out.writeBit(backward);

			// lowest vertex
			final int lowestVertexBlockId = mapping.getBlockId(clustering.getCluster(lowestVertexId));
			final int lowestVertexOffset = vertexIdToVertexOffset[lowestVertexId];
			out.writeUInt(lowestVertexBlockId, bitsPerBlockId);
			out.writeUInt(lowestVertexOffset, bitsPerVertexOffset);

			// highest vertex
			final int highestVertexBlockId = mapping.getBlockId(clustering.getCluster(highestVertexId));
			final int highestVertexOffset = vertexIdToVertexOffset[highestVertexId];
			out.writeUInt(highestVertexBlockId, bitsPerBlockId);
			out.writeUInt(highestVertexOffset, bitsPerVertexOffset);

			// generic edge data
			out.writeUInt(edge.getWeight(), bitsPerEdgeWeight);
			out.writeBit(shortcut);

			if (shortcut) {
				final boolean external = externalShortcutPaths.containsKey(edge.getId());
				out.writeBit(external);

				if (external) {
					// write the BIT offset to the shortcut path's start
					final int shortcutOffset;
					if (shortcutPathOffsets.containsKey(edge.getId())) {
						shortcutOffset = shortcutPathOffsets.get(edge.getId());
					} else {
						shortcutOffset = 0;
					}
					out.writeUInt(shortcutOffset, bitsPerShortcutPathOffset);
					// write the path length
					writeEscapableNumber(out, externalShortcutPaths.get(edge.getId()).length,
							bitsPerShortcutPathLength);

				} else {
					// write the middle / bypassed vertex' reference
					// (needed for the unpacking of internal shortcuts)
					final int bypassedBlockId = mapping.getBlockId(clustering.getCluster(edge
							.getBypassedVertexId()));
					final int bypassedOffset = vertexIdToVertexOffset[edge.getBypassedVertexId()];

					out.writeUInt(bypassedBlockId, bitsPerBlockId);
					out.writeUInt(bypassedOffset, bitsPerVertexOffset);
				}

			} else {
				// "normal" edge data
				out.writeUInt(edge.getType(), bitsPerStreetType);
				out.writeBit(edge.isRoundabout());

				// street name and ref
				final String name = edge.getName();
				final boolean hasName = name != null && !name.isEmpty()
						&& streetNameOffsets.containsKey(name);
				out.writeBit(hasName);

				final String ref = edge.getRef();
				final boolean hasRef = ref != null && !ref.isEmpty()
						&& streetNameOffsets.containsKey(ref);
				out.writeBit(hasRef);

				if (hasName) {
					out.writeUInt(streetNameOffsets.get(name), bitsPerStreetNamesOffset);
				}

				if (hasRef) {
					out.writeUInt(streetNameOffsets.get(ref), bitsPerStreetNamesOffset);
				}

				// waypoints
				writeWaypoints(out, getCleanedAndReorderedWaypoints(edge));
			}
		}

		/**
		 * Expects that the waypoints of an edge are ordered from source to target!<br/>
		 * If the edge's waypoints containing also the source and the target, both will be removed. It
		 * will also ensure that the waypoints will be ordered from source to target, but after the
		 * reader's interpretations (will be reversed for undirected edges, where the target is lower
		 * than the source).
		 * 
		 * @param edge
		 *            The edge, from which the cleaned and (re-)ordered waypoints are requested.
		 * @return The edge's cleaned and (re-)ordered waypoints.
		 */
		protected GeoCoordinate[] getCleanedAndReorderedWaypoints(final CHEdge edge) {
			GeoCoordinate[] waypoints = edge.getWaypoints();
			final GeoCoordinate source = edge.getSource().getCoordinate();
			final GeoCoordinate target = edge.getTarget().getCoordinate();

			// cleaning: remove source and target, if there exists as start/end at the waypoints
			if (source.equals(waypoints[0])) {
				if (target.equals(waypoints[waypoints.length - 1])) {
					// remove source and target from the waypoints
					final GeoCoordinate[] tmp = new GeoCoordinate[waypoints.length - 2];
					System.arraycopy(waypoints, 1, tmp, 0, waypoints.length - 2);
					waypoints = tmp;

				} else {
					// remove source from the waypoints
					final GeoCoordinate[] tmp = new GeoCoordinate[waypoints.length - 1];
					System.arraycopy(waypoints, 1, tmp, 0, waypoints.length - 1);
					waypoints = tmp;
				}

			} else {
				// remove target from the waypoints
				final GeoCoordinate[] tmp = new GeoCoordinate[waypoints.length - 1];
				System.arraycopy(waypoints, 0, tmp, 0, waypoints.length - 1);
				waypoints = tmp;
			}

			// reordering: order from original source to target
			// -- should be ordered from resolved source to resolved target
			// -> if fwd then source := low else source := high
			// --> reversed order for undirected edges with low == target
			if (edge.isUndirected() && edge.getLowestVertexId() == edge.getTargetId()) {
				ArrayUtils.reverse(waypoints);
			}

			return waypoints;
		}

		/**
		 * Writes all waypoints to the output stream.
		 * 
		 * @param out
		 *            The target output stream.
		 * @param waypoints
		 *            The waypoints, which have to be written to the stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeWaypoints(final BitArrayOutputStream out, final GeoCoordinate[] waypoints)
				throws IOException {
			// #waypoints
			writeEscapableNumber(out, waypoints.length, 16);

			for (final GeoCoordinate coordinate : waypoints) {
				writeCoordinate(out, coordinate);
			}
		}

		/**
		 * Writes the coordinate values (latitude, longitude) to the output stream.
		 * 
		 * @param out
		 *            The output stream, to which the data has to be written.
		 * @param coordinate
		 *            The coordinate, which has to be written to the stream.
		 * @throws java.io.IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeCoordinate(final BitArrayOutputStream out, final GeoCoordinate coordinate)
				throws IOException {
			out.writeUInt(coordinate.getLatitudeE6() - boundingBox.minLatitudeE6, bitsPerCoordinate);
			out.writeUInt(coordinate.getLongitudeE6() - boundingBox.minLongitudeE6, bitsPerCoordinate);
		}

		/**
		 * Writes the number to the output stream, using different numbers of bits to encode this
		 * number, depending on the number itself. (4 for small numbers, the specified number of bits
		 * for large numbers.)
		 * 
		 * @param out
		 *            The output stream, to which the number has to be written.
		 * @param num
		 *            The number, which has to be written to the output stream.
		 * @param bitsForEscapedVariant
		 *            The number of bits, which have to be used for the escaped variant.
		 * @throws java.io.IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeEscapableNumber(final BitArrayOutputStream out, final int num,
				final int bitsForEscapedVariant) throws IOException {

			if (num < 15) {
				out.writeUInt(num, 4);

			} else {
				// escape field by 0xFF
				out.writeUInt(15, 4);
				out.writeUInt(num, bitsForEscapedVariant);
			}
		}

	}

}
