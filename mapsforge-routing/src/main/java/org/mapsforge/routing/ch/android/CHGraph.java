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
package org.mapsforge.routing.ch.android;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;

import org.mapsforge.routing.android.data.AddressLookupTable;
import org.mapsforge.routing.android.data.LRUCache;
import org.mapsforge.routing.android.data.ObjectPool;
import org.mapsforge.routing.android.data.Offset;
import org.mapsforge.routing.android.data.Pointer;
import org.mapsforge.routing.android.data.StaticRTree;
import org.mapsforge.routing.android.io.IOUtils;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.Rect;
import org.mapsforge.routing.ch.preprocessing.io.HeaderGlobals;

/**
 * Mobile Contraction Hierarchies graph, based on the related binary file format, created by
 * {@link org.mapsforge.routing.ch.preprocessing.io.FileWriter}.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHGraph.java 1746 2012-01-16 22:38:34Z Patrick.Jungermann@googlemail.com $
 */
class CHGraph implements Closeable {

	/**
	 * Need to read 4 bytes too much since the Deserializer requires that.
	 */
	private static final int OVERHEAD_DESERIALIZER = 4;

	/**
	 * Initial size of the used pool of vertices.
	 */
	private static final int INITIAL_POOL_SIZE_VERTICES = 1;
	/**
	 * Initial size of the used pool of edges.
	 */
	private static final int INITIAL_POOL_SIZE_EDGES = 1;
	/**
	 * Initial size of the used pool of offsets.
	 */
	private static final int INITIAL_POOL_SIZE_OFFSETS = 1;

	/**
	 * The access to the binary data file.
	 */
	private final RandomAccessFile raf;
	/**
	 * The R-tree of the MCH file, containing blocks indexed by their contained vertices' and edges'
	 * geographic coordinates.
	 */
	private final StaticRTree rTree;
	/**
	 * Address table, containing the addresses of all blocks.
	 */
	private final AddressLookupTable blockIndex;
	/**
	 * LRU-based cache used for caching blocks.
	 */
	private final LRUCache<Block> blockCache;

	/**
	 * The start address of the graph blocks (start of the first block).
	 */
	private final long startAddressBlocks;
	/**
	 * Bitmask used for the conversion between (vertex identifier) &lt;-&gt; (block identifier, vertex
	 * offset).
	 */
	private final int bitmask;

	/**
	 * Flag, whether the used MCH file was created in debug mode or not.
	 */
	final boolean debug;

	/**
	 * The number of bits used for encoding a block identifier.
	 */
	final byte bitsPerBlockId;
	/**
	 * The number of bits used to encode a vertex offset.
	 */
	final byte bitsPerVertexOffset;
	/**
	 * The number of bits used to encode an edge's weight.
	 */
	final byte bitsPerEdgeWeight;
	/**
	 * The number of bits used to encode a street type identifier.
	 */
	final byte bitsPerStreetType;

	/**
	 * All available street types, accessible by their identifiers.
	 */
	final String[] streetTypes;

	/**
	 * The pool of vertex objects.
	 */
	final ObjectPool<CHVertex> poolVertices;
	/**
	 * The pool of edge objects.
	 */
	final ObjectPool<CHEdge> poolEdges;
	/**
	 * The pool of offset objects.
	 */
	final ObjectPool<Offset> poolOffsets;

	/**
	 * Creates a Contraction Hierarchies graph, which was serialized to the given binary file and uses a
	 * cache of the defined size in bytes.
	 * 
	 * @param mchFile
	 *            The binary file containing the graph's data.
	 * @param cacheSizeInBytes
	 *            The size of the used cache in bytes.
	 * @throws IOException
	 *             if there was a problem with reading the data from the binary file.
	 */
	public CHGraph(final File mchFile, final int cacheSizeInBytes) throws IOException {
		// fetch header
		raf = new RandomAccessFile(mchFile, "r");
		byte[] header = read(0, HeaderGlobals.BINARY_FILE_HEADER_LENGTH);

		DataInputStream dis = null;
		try {
			// read header
			dis = new DataInputStream(new ByteArrayInputStream(header));

			// verify header
			if (!isValidHeader(dis)) {
				throw new IOException("Invalid header.");
			}

			// get all start addresses
			final long startAddressGraph = dis.readLong();
			final long startAddressIndex = dis.readLong();
			final long startAddressRTree = dis.readLong();

			startAddressBlocks = startAddressGraph + HeaderGlobals.GRAPH_BLOCKS_HEADER_LENGTH;

			dis.close();
			dis = null;

			// read the graph header
			header = read(startAddressGraph, HeaderGlobals.GRAPH_BLOCKS_HEADER_LENGTH);

			// extract all information from the graph header.
			dis = new DataInputStream(new ByteArrayInputStream(header));
			debug = dis.readBoolean();
			bitsPerBlockId = dis.readByte();
			bitsPerVertexOffset = dis.readByte();
			bitsPerEdgeWeight = dis.readByte();
			bitsPerStreetType = dis.readByte();
			streetTypes = readStreetTypes(dis);

			bitmask = getBitmask(bitsPerVertexOffset);

			// basic components
			blockIndex = new AddressLookupTable(startAddressIndex, startAddressRTree, mchFile);
			rTree = new StaticRTree(mchFile, startAddressRTree, raf.length(),
					HeaderGlobals.STATIC_R_TREE_HEADER_MAGIC);
			blockCache = new LRUCache<Block>(cacheSizeInBytes);

		} finally {
			if (dis != null) {
				dis.close();
			}
		}

		// initialize pools
		this.poolVertices = createPoolForVertices();
		this.poolEdges = createPoolForEdges();
		this.poolOffsets = createPoolForOffsets();
	}

	/**
	 * Reads the specified number of bytes, beginning at the start address.
	 * 
	 * @param from
	 *            The start address.
	 * @param length
	 *            The number of bytes, which have to be read.
	 * @return The data, read from the file.
	 * @throws IOException
	 *             if there was a problem with reading the data from the file.
	 */
	protected byte[] read(final long from, final int length) throws IOException {
		raf.seek(from);
		final byte[] data = new byte[length];
		raf.readFully(data);

		return data;
	}

	/**
	 * Creates a bitmask, used for vertexOffset calculations.
	 * 
	 * @param bitsPerVertexOffset
	 *            Number of bits used to encode a vertex offset.
	 * @return The bitmask, which can be used to calculate vertexOffset.
	 */
	protected int getBitmask(final int bitsPerVertexOffset) {
		int bMask = 0;
		for (int i = 0; i < bitsPerVertexOffset; i++) {
			bMask = (bMask << 1) | 1;
		}

		return bMask;
	}

	/**
	 * Verifies the binary file's header based on its header magic.
	 * 
	 * @param dis
	 *            The input stream, containing the data of the header.
	 * @return {@code true}, if it's a valid header, otherwise {@code false}.
	 * @throws IOException
	 *             if there was any problem related to the reading of the data from the input stream.
	 */
	protected boolean isValidHeader(final DataInputStream dis) throws IOException {
		byte[] headerMagic = new byte[HeaderGlobals.BINARY_FILE_HEADER_MAGIC.length];

		return headerMagic.length == dis.read(headerMagic)
				&& Arrays.equals(headerMagic, HeaderGlobals.BINARY_FILE_HEADER_MAGIC);
	}

	/**
	 * Creates a pool for {@link CHVertex vertices}.
	 * 
	 * @return A pool for {@link CHVertex vertices}.
	 */
	protected ObjectPool<CHVertex> createPoolForVertices() {
		return new ObjectPool<CHVertex>(
				new ObjectPool.PoolableFactory<CHVertex>() {

					@Override
					public CHVertex makeObject() {
						return new CHVertex();
					}
				}, INITIAL_POOL_SIZE_VERTICES);
	}

	/**
	 * Creates a pool for {@link CHEdge edges}.
	 * 
	 * @return A pool for {@link CHEdge edges}.
	 */
	protected ObjectPool<CHEdge> createPoolForEdges() {
		return new ObjectPool<CHEdge>(
				new ObjectPool.PoolableFactory<CHEdge>() {

					@Override
					public CHEdge makeObject() {
						return new CHEdge();
					}
				}, INITIAL_POOL_SIZE_EDGES);
	}

	/**
	 * Creates a pool for {@link Offset offsets}.
	 * 
	 * @return A pool for {@link Offset offsets}.
	 */
	protected ObjectPool<Offset> createPoolForOffsets() {
		return new ObjectPool<Offset>(
				new ObjectPool.PoolableFactory<Offset>() {

					@Override
					public Offset makeObject() {
						return new Offset();
					}
				}, INITIAL_POOL_SIZE_OFFSETS);
	}

	/**
	 * Reads and returns all the street types from the input stream.
	 * 
	 * @param dis
	 *            The input stream, which contains the street types.
	 * @return All the extracted street types.
	 * @throws IOException
	 *             if there was a problem with reading the street types.
	 */
	protected String[] readStreetTypes(final DataInputStream dis) throws IOException {
		final int num = dis.readByte();
		final String[] types = new String[num];

		for (int i = 0; i < num; i++) {
			types[i] = IOUtils.getZeroTerminatedString(dis, "UTF-8");
		}

		return types;
	}

	/**
	 * Returns the minimum bounding rectangle around all vertices and edges.
	 * 
	 * @return The minimum bounding rectangle around all vertices and edges.
	 */
	public Rect getBoundingBox() {
		return rTree.getBoundingBox();
	}

	/**
	 * Returns the vertex for the given identifier.
	 * 
	 * @param id
	 *            The vertex' identifier.
	 * @return The vertex for the given identifier.
	 * @throws IOException
	 *             if there was a problem with retrieving the vertex.
	 */
	public CHVertex getVertex(final int id) throws IOException {
		final Block block = getBlock(getBlockId(id));
		final int vertexOffset = getVertexOffset(id);

		return block.getVertex(vertexOffset);
	}

	/**
	 * Returns the nearest vertex to the given geographical point.
	 * 
	 * @param coordinate
	 *            The reference point.
	 * @param maxDistanceMeters
	 *            The maximum distance in meters.
	 * @return The nearest vertex to the given point, if any.
	 * @throws IOException
	 *             if there was a problem with retrieving the data.
	 */
	public CHVertex getNearestVertex(final GeoCoordinate coordinate, final int maxDistanceMeters)
			throws IOException {
		final Rect rect = new Rect(coordinate, maxDistanceMeters);

		double minDistance = Double.MAX_VALUE;
		CHVertex vertex = null;
		for (final int blockId : rTree.overlaps(rect)) {
			final Block block = getBlock(blockId);
			final int num = block.getNumVertices();

			for (int i = 0; i < num; i++) {
				final CHVertex candidate = block.getVertex(i);
				final double distance = GeoCoordinate.sphericalDistance(coordinate.getLatitudeE6(),
						coordinate.getLongitudeE6(), candidate.latitudeE6, candidate.longitudeE6);

				if (minDistance > distance) {
					minDistance = distance;
					release(vertex);
					vertex = candidate;

				} else {
					release(candidate);
				}
			}
		}

		return vertex;
	}

	/**
	 * Returns all vertices, which are located within the given bounding box.
	 * 
	 * @param boundingBox
	 *            The bounding box, from which all included vertices are requested.
	 * @return All vertices, which are located within the given bounding box.
	 * @throws IOException
	 *             if there was a problem with retrieving the information.
	 */
	public LinkedList<CHVertex> getVerticesWithinBoundingBox(Rect boundingBox) throws IOException {
		final LinkedList<CHVertex> vertices = new LinkedList<CHVertex>();

		for (final int blockId : rTree.overlaps(boundingBox)) {
			final Block block = getBlock(blockId);
			final int num = block.getNumVertices();

			for (int i = 0; i < num; i++) {
				final CHVertex candidate = block.getVertex(i);

				if (boundingBox.includes(candidate.latitudeE6, candidate.longitudeE6)) {
					vertices.add(candidate);
				}
			}
		}

		return vertices;
	}

	/**
	 * Returns all outgoing edges of the related vertex, which are leading to vertices of higher levels.
	 * 
	 * @param vertexId
	 *            The related vertex' identifier.
	 * @return All outgoing edges of the related vertex, which are leading to vertices of higher levels.
	 * @throws IOException
	 *             if there was a problem with reading the required data.
	 */
	public CHEdge[] getOutgoingEdgesToHigherVertices(final int vertexId) throws IOException {
		final int blockId = getBlockId(vertexId);
		final Block block = getBlock(blockId);
		final int vertexOffset = getVertexOffset(vertexId);

		return block != null ? block.getOutgoingEdgesToHigherVertices(vertexOffset) : null;
	}

	/**
	 * Returns all ingoing edges of the related vertex, which are leading to vertices of higher levels.
	 * 
	 * @param vertexId
	 *            The related vertex' identifier.
	 * @return All ingoing edges of the related vertex, which are leading to vertices of higher levels.
	 * @throws java.io.IOException
	 *             if there was a problem with reading the required data.
	 */
	public CHEdge[] getIngoingEdgesFromHigherVertices(final int vertexId) throws IOException {
		final int blockId = getBlockId(vertexId);
		final Block block = getBlock(blockId);
		final int vertexOffset = getVertexOffset(vertexId);

		return block != null ? block.getIngoingEdgesFromHigherVertices(vertexOffset) : null;
	}

	/**
	 * Unpacks the given shortcut edge to a sequence of normal / real edges.
	 * 
	 * @param edge
	 *            The shortcut edge.
	 * @param startId
	 *            The path's start vertex' identifier.
	 * @return The sequence of normal edges.
	 * @throws IOException
	 *             if there was a problem with reading the required data.
	 */
	public CHEdge[] unpackShortcut(final CHEdge edge, final int startId) throws IOException {
		final CHEdge[] path;
		if (edge.shortcut) {
			Block block = getBlock(getBlockId(edge.getLowestVertexId()));
			path = block.unpackShortcut(edge, startId);

		} else {
			// no unpacking needed
			if (edge.getSourceId() != startId) {
				edge.switchSourceAndTarget();
			}

			path = new CHEdge[] { edge };
		}

		return path;
	}

	/**
	 * Computes the vertexId from a blockId and a vertexOffset.
	 * 
	 * @param blockId
	 *            the block the vertex belongs to.
	 * @param vertexOffset
	 *            the offset within the block the vertex is stored at.
	 * @return Returns the id of the specified vertex, it is not verified if the id is valid!
	 */
	protected int getVertexId(final int blockId, final int vertexOffset) {
		return (blockId << bitsPerVertexOffset) | vertexOffset;
	}

	/**
	 * Extracts the blockId from a given vertex id.
	 * 
	 * @param vertexId
	 *            not verified.
	 * @return Returns the block identifier, the given vertex belongs to.
	 */
	protected int getBlockId(final int vertexId) {
		return vertexId >>> bitsPerVertexOffset;
	}

	/**
	 * Extracts the offset of the vertex within its block.
	 * 
	 * @param vertexId
	 *            not verified.
	 * @return the offset within the block.
	 */
	protected int getVertexOffset(final int vertexId) {
		return vertexId & bitmask;
	}

	/**
	 * Returns the block for the given identifier.
	 * 
	 * @param id
	 *            The block's identifier.
	 * @return The block for the given identifier.
	 * @throws IOException
	 *             if there was a problem with retrieving the block.
	 */
	protected Block getBlock(final int id) throws IOException {
		Block block = blockCache.getItem(id);
		if (block == null) {
			block = readBlock(id);
			blockCache.putItem(block);

		} else if (QueryingStatistics.getInstance().isEnabled()) {
			QueryingStatistics.getInstance().numCacheHits++;
		}

		return block;
	}

	/**
	 * Reads a block from the file.
	 * 
	 * @param blockId
	 *            The block's identifier.
	 * @return The related block.
	 * @throws IOException
	 *             if there was a problem while reading the block.
	 */
	protected Block readBlock(final int blockId) throws IOException {
		final Pointer pointer = blockIndex.getPointer(blockId);
		if (pointer != null) {
			if (QueryingStatistics.getInstance().isEnabled()) {
				QueryingStatistics.getInstance().numBlockReads++;
			}

			final long startAddress = startAddressBlocks + pointer.startAddr;
			// needs to read too much since the Deserializer requires that.
			final int numBytes = pointer.lengthBytes + OVERHEAD_DESERIALIZER;

			raf.seek(startAddress);
			final byte[] buff = new byte[numBytes];
			raf.readFully(buff);

			return new Block(blockId, buff, this);
		}

		return null;
	}

	/**
	 * Releases the given vertex.
	 * 
	 * @param vertex
	 *            The vertex, which has to be released.
	 */
	public void release(final CHVertex vertex) {
		poolVertices.release(vertex);
	}

	/**
	 * Releases the given edge.
	 * 
	 * @param edge
	 *            The edge, which has to be released.
	 */
	public void release(final CHEdge edge) {
		poolEdges.release(edge);
	}

	/**
	 * Closes the access to the binary data file. Other methods will not work after that.
	 * 
	 * @throws IOException
	 *             if there was a problem with closing the access to the binary file.
	 */
	public void close() throws IOException {
		IOException ex = null;
		try {
			raf.close();

		} catch (IOException e) {
			ex = e;
		}

		try {
			rTree.close();

		} catch (IOException e) {
			if (ex == null) {
				ex = e;
			}
		}

		if (ex != null) {
			throw ex;
		}
	}
}
