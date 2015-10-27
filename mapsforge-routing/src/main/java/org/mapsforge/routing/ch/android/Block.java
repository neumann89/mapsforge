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

import org.mapsforge.routing.android.data.CacheItem;
import org.mapsforge.routing.android.data.Deserializer;
import org.mapsforge.routing.android.data.Offset;
import org.mapsforge.routing.android.io.IOUtils;
import org.mapsforge.routing.preprocessing.data.ArrayUtils;

/**
 * Represents a logical graph block of the Contraction Hierarchies graph's binary file.
 * 
 * @author Patrick Jungermann
 * @version $Id: Block.java 1746 2012-01-16 22:38:34Z Patrick.Jungermann@googlemail.com $
 */
class Block implements CacheItem {

	/**
	 * Fixed number of bytes, written for the header. <strong>This is not the full size of the
	 * header.</strong> Use {@link #getFirstVertexBitOffset()}, to get the bit offset of the first
	 * vertex or the size of the header in bits.
	 */
	private static final int FIRST_VERTEX_FIXED_BYTE_OFFSET = 16;

	/**
	 * The block's identifier.
	 */
	private final int blockId;
	/**
	 * The block's binary data.
	 */
	private final byte[] data;
	/**
	 * The related graph.
	 */
	private final CHGraph graph;

	/**
	 * The number of vertices, contained by this block.
	 */
	private final short numVertices;

	/**
	 * Min. latitude of this block's bounding box. All stored latitude values are only diffs to this
	 * value.
	 */
	private final int minLatitude;
	/**
	 * Min. longitude of this block's bounding box. All stored longitude values are only diffs to this
	 * value.
	 */
	private final int minLongitude;

	/**
	 * The number of bits, used to encode one coordinate value.
	 */
	private final byte bitsPerCoordinate;
	/**
	 * The number of bits, used to encode one offset to a vertex' edges.
	 */
	private final byte bitsPerVertexEdgesOffset;
	/**
	 * The number of bits, used to encode one street name's offset.
	 */
	private final byte bitsPerStreetNamesOffset;
	/**
	 * The number of bits, used to encode one shortcut path's offset
	 */
	private final byte bitsPerShortcutPathOffset;
	/**
	 * The number of bits, used to encode one shortcut path's length.
	 */
	private final byte bitsPerShortcutPathLength;
	/**
	 * The number of bits, used to encode one edge's offset.
	 */
	private final byte bitsPerEdgeOffset;

	/**
	 * The offset to all street names.
	 */
	private final int streetNamesOffset;

	/**
	 * The number of bits, used to store one vertex.
	 */
	private final int bitsPerVertex;

	/**
	 * Creates a block, representing a logical graph block of the Contraction Hierarchies graph's binary
	 * file.
	 * 
	 * @param blockId
	 *            The block's identifier.
	 * @param data
	 *            The block's binary data.
	 * @param graph
	 *            The related graph.
	 */
	public Block(final int blockId, final byte[] data, final CHGraph graph) {
		this.blockId = blockId;
		this.data = data;
		this.graph = graph;

		// deserialize header
		final Offset offset = graph.poolOffsets.borrow();
		offset.set(0, 0);

		// #vertices
		numVertices = Deserializer.readShort(data, offset);

		// min. latitude and longitude
		minLatitude = Deserializer.readInt(data, offset);
		minLongitude = Deserializer.readInt(data, offset);

		// #bits used for the different kinds of data
		bitsPerCoordinate = Deserializer.readByte(data, offset);
		bitsPerVertexEdgesOffset = Deserializer.readByte(data, offset);
		bitsPerStreetNamesOffset = Deserializer.readByte(data, offset);
		bitsPerShortcutPathOffset = Deserializer.readByte(data, offset);
		bitsPerShortcutPathLength = Deserializer.readByte(data, offset);
		bitsPerEdgeOffset = Deserializer.readByte(data, offset);

		streetNamesOffset = Deserializer.readUInt(data, 24, offset);

		graph.poolOffsets.release(offset);

		bitsPerVertex = 2 * bitsPerCoordinate + bitsPerVertexEdgesOffset + (graph.debug ? 32 : 0);
	}

	/**
	 * Returns the bit offset of the first vertex, and therefore also the header length.
	 * 
	 * @return The bit offset of the first vertex, and therefore also the header length.
	 */
	protected int getFirstVertexBitOffset() {
		// TODO: has to be modified, if a variable #bits will be used for the street names offset or
		// removed (24 bits = 3 bytes; could be used at the byte offset field)
		return FIRST_VERTEX_FIXED_BYTE_OFFSET * 8 + 24;
	}

	/**
	 * Returns the bit offset of the vertex.
	 * 
	 * @param vertexOffset
	 *            The vertex' offset (position inside of this block).
	 * @return The bit offset of the vertex.
	 */
	protected int getVertexBitOffset(final int vertexOffset) {
		return getFirstVertexBitOffset() + vertexOffset * bitsPerVertex;
	}

	@Override
	public int getId() {
		return blockId;
	}

	@Override
	public int getSizeBytes() {
		return 30 + data.length;
	}

	/**
	 * Returns the number of its contained vertices.
	 * 
	 * @return The number of its contained vertices.
	 */
	public int getNumVertices() {
		return numVertices;
	}

	/**
	 * Returns the vertex based on its offset (0..n) at this block.
	 * 
	 * @param vertexOffset
	 *            The offset of the vertex at this block.
	 * @return The vertex based on its offset (0..n) at this block.
	 */
	public CHVertex getVertex(final int vertexOffset) {
		if (vertexOffset < 0 || vertexOffset >= numVertices) {
			return null;
		}

		// recycle a vertex from the pool
		final CHVertex vertex = graph.poolVertices.borrow();

		// recycle an offset from the pool
		final Offset offset = graph.poolOffsets.borrow();
		// jump the the searched vertex via the given vertexOffset
		offset.set(0, getVertexBitOffset(vertexOffset));

		// read the vertex data
		vertex.id = graph.getVertexId(blockId, vertexOffset);
		vertex.latitudeE6 = minLatitude + Deserializer.readUInt(data, bitsPerCoordinate, offset);
		vertex.longitudeE6 = minLongitude + Deserializer.readUInt(data, bitsPerCoordinate, offset);

		// release objects
		graph.poolOffsets.release(offset);

		if (QueryingStatistics.getInstance().isEnabled()) {
			QueryingStatistics.getInstance().addVisitedVertex(vertex.id);
		}

		return vertex;
	}

	/**
	 * Returns all outgoing edges of the related vertex, which are leading to vertices of higher levels.
	 * 
	 * @param vertexOffset
	 *            The related vertex' offset.
	 * @return All outgoing edges of the related vertex, which are leading to vertices of higher levels.
	 */
	public CHEdge[] getOutgoingEdgesToHigherVertices(final int vertexOffset) {
		return readEdges(vertexOffset, true);
	}

	/**
	 * Returns all ingoing edges of the related vertex, which are leading to vertices of higher levels.
	 * 
	 * @param vertexOffset
	 *            The related vertex' offset.
	 * @return All ingoing edges of the related vertex, which are leading to vertices of higher levels.
	 */
	public CHEdge[] getIngoingEdgesFromHigherVertices(final int vertexOffset) {
		return readEdges(vertexOffset, false);
	}

	/**
	 * Reads and returns all (outgoing or ingoing) edges of the vertex from the given bit offset.
	 * 
	 * @param vertexOffset
	 *            Their vertex' offset (source or target).
	 * @param outgoing
	 *            Whether its an outgoing edge of the vertex, or not (ingoing edge).
	 * @return All (outgoing or ingoing) edges of the vertex.
	 */
	protected CHEdge[] readEdges(final int vertexOffset, final boolean outgoing) {
		// bit (!) offset of the position, where the vertex' edges offset could be extracted
		final int bitOffset = getVertexBitOffset(vertexOffset) + 2 * bitsPerCoordinate;

		// recycle an offset from the pool
		final Offset offset = graph.poolOffsets.borrow();
		// jump the the searched vertex via the given vertexOffset
		offset.set(0, bitOffset);

		// get edges offset (byte offset!)
		final int edgesOffset = Deserializer.readUInt(data, bitsPerVertexEdgesOffset, offset);
		// jump to the outgoing edges
		offset.set(edgesOffset, 0);

		// get the data for each edge
		final int num = readEscapableNum(offset, 24);
		final CHEdge[] tmp = new CHEdge[num];

		int k = 0;
		for (int i = 0; i < num; i++) {
			CHEdge edge = readEdge(offset);

			if ((outgoing && edge.forward) || (!outgoing && edge.backward)) {
				tmp[k++] = edge;

			} else {
				graph.poolEdges.release(edge);
			}
		}

		// release objects
		graph.poolOffsets.release(offset);

		final CHEdge[] edges = new CHEdge[k];
		System.arraycopy(tmp, 0, edges, 0, k);

		return edges;
	}

	/**
	 * Reads and returns an edge of a vertex from the data at the given position.
	 * 
	 * @param offset
	 *            The position, from which the data has to be read.
	 * @return The edge.
	 */
	protected CHEdge readEdge(final Offset offset) {
		final CHEdge edge = graph.poolEdges.borrow();

		if (graph.debug) {
			// ignore the edge id and increase the offset
			Deserializer.readInt(data, offset);
		}

		// forward? / backward?
		edge.forward = Deserializer.readBit(data, offset);// outgoing
		edge.backward = Deserializer.readBit(data, offset);// ingoing

		// source and target will be resolved using the both vertices and the direction flags
		// the result will be different for undirected edges (fwd + bwd), where the highest one was the
		// source, but will stay a valid configuration

		// TODO: for internal edges, this information is already available
		// -> add a "external"-bit and add this information only for external edges? decreased file
		// size?

		// lowest vertex
		final int lowestVertexBlockId = Deserializer.readUInt(data, graph.bitsPerBlockId, offset);
		final int lowestVertexOffset = Deserializer.readUInt(data, graph.bitsPerVertexOffset, offset);
		edge.setLowestVertexId(graph.getVertexId(lowestVertexBlockId, lowestVertexOffset));

		// highest vertex
		final int highestVertexBlockId = Deserializer.readUInt(data, graph.bitsPerBlockId, offset);
		final int highestVertexOffset = Deserializer.readUInt(data, graph.bitsPerVertexOffset, offset);
		edge.setHighestVertexId(graph.getVertexId(highestVertexBlockId, highestVertexOffset));

		// generic properties
		edge.weight = Deserializer.readUInt(data, graph.bitsPerEdgeWeight, offset);
		edge.shortcut = Deserializer.readBit(data, offset);

		if (edge.shortcut) {
			resetSatelliteData(edge);
			edge.external = Deserializer.readBit(data, offset);

			if (edge.external) {
				edge.shortcutPathBitOffset = Deserializer.readUInt(data, bitsPerShortcutPathOffset,
						offset);
				edge.shortcutPathLength = readEscapableNum(offset, bitsPerShortcutPathLength);

				// remove the satellite data
				edge.bypassedVertexId = -1;

			} else {
				final int bypassedVertexBlockId = Deserializer.readUInt(
						data, graph.bitsPerBlockId, offset);
				final int bypassedVertexOffset = Deserializer.readUInt(
						data, graph.bitsPerVertexOffset, offset);
				edge.bypassedVertexId = graph.getVertexId(bypassedVertexBlockId, bypassedVertexOffset);

				// remove the satellite data
				edge.shortcutPathBitOffset = -1;
				edge.shortcutPathLength = -1;
			}

		} else {
			// "normal" edge properties
			edge.streetTypeId = Deserializer.readUInt(data, graph.bitsPerStreetType, offset);
			edge.roundabout = Deserializer.readBit(data, offset);

			// street name and ref
			final boolean hasStreetName = Deserializer.readBit(data, offset);
			final boolean hasRef = Deserializer.readBit(data, offset);
			edge.name = hasStreetName ? readStreetName(offset) : null;
			edge.ref = hasRef ? readStreetName(offset) : null;

			// waypoints
			edge.waypoints = readWaypoints(offset);
		}

		return edge;
	}

	/**
	 * Resets all the satellite data of an edge object, used for a shortcut edge.
	 * 
	 * @param edge
	 *            The pooled edge object, for which the satellite data has to be removed.
	 */
	protected void resetSatelliteData(final CHEdge edge) {
		edge.streetTypeId = -1;
		edge.roundabout = false;
		edge.name = null;
		edge.ref = null;
		edge.waypoints = new int[0];
	}

	/**
	 * Reads a street name from the data at the given position.
	 * 
	 * @param offset
	 *            The position, from which the street name has to be read.
	 * @return The street name.
	 */
	protected byte[] readStreetName(final Offset offset) {
		final int byteOffset = Deserializer.readUInt(data, bitsPerStreetNamesOffset, offset);

		return IOUtils.getZeroTerminatedString(data, streetNamesOffset + byteOffset);
	}

	/**
	 * Reads all waypoints of an edge from the data at the given offset. For each waypoint there is one
	 * entry for its latitude value and one for its longitude value.
	 * 
	 * @param offset
	 *            The position, from which the waypoint data has to be read.
	 * @return All waypoints as pairs of latitude, longitude values.
	 */
	protected int[] readWaypoints(final Offset offset) {
		final int num = readEscapableNum(offset, 16);
		final int[] waypoints = new int[2 * num];

		for (int i = 0; i < num; i++) {
			waypoints[2 * i] = minLatitude + Deserializer.readUInt(data, bitsPerCoordinate, offset);
			waypoints[2 * i + 1] = minLongitude
					+ Deserializer.readUInt(data, bitsPerCoordinate, offset);
		}

		return waypoints;
	}

	/**
	 * Reads and returns the number from the current position, which might be escaped, depending on
	 * itself.
	 * 
	 * @param offset
	 *            The current position, from which the number has to be read.
	 * @param bitsForEscapedVariant
	 *            The number of bits, used for the escaped variant.
	 * @return The number.
	 */
	protected int readEscapableNum(final Offset offset, final int bitsForEscapedVariant) {
		int num = Deserializer.readUInt(data, 4, offset);
		if (num == 15) { // escaped by 0xFF=15
			num = Deserializer.readUInt(data, bitsForEscapedVariant, offset);
		}

		return num;
	}

	/**
	 * Unpacks the given shortcut edge to a sequence of normal / real edges.
	 * 
	 * @param edge
	 *            The shortcut edge.
	 * @param startId
	 *            The path's start vertex' identifier.
	 * @return The sequence of normal edges.
	 */
	protected CHEdge[] unpackShortcut(final CHEdge edge, final int startId) {
		if (!edge.shortcut) {
			if (edge.getSourceId() != startId) {
				edge.switchSourceAndTarget();
			}

			return new CHEdge[] { edge };
		}

		final CHEdge[] unpacked;
		if (edge.external) {
			unpacked = unpackExternalShortcut(edge, startId);

		} else {
			unpacked = unpackInternalShortcut(edge, startId);
		}

		return unpacked;
	}

	/**
	 * Unpacks the given internal shortcut edge to a sequence of normal / real edges.
	 * 
	 * @param edge
	 *            The internal shortcut edge.
	 * @param startId
	 *            The path's start vertex' identifier.
	 * @return The sequence of normal edges.
	 */
	protected CHEdge[] unpackInternalShortcut(final CHEdge edge, final int startId) {
		if (edge == null) {
			return new CHEdge[0];
		}
		if (!edge.shortcut) {
			if (edge.getSourceId() != startId) {
				edge.switchSourceAndTarget();
			}

			return new CHEdge[] { edge };
		}

		// use the FWD variants for directed forward und undirected shortcuts
		// use the BWD variants for directed backward shortcuts
		return edge.forward ? unpackInternalFwdShortcut(edge, startId) : unpackInternalBwdShortcut(
				edge, startId);
	}

	/**
	 * Unpacks the given internal forward shortcut edge to a sequence of normal / real edges.
	 * 
	 * @param edge
	 *            The shortcut edge.
	 * @param startId
	 *            The path's start vertex' identifier.
	 * @return The sequence of normal edges.
	 */
	protected CHEdge[] unpackInternalFwdShortcut(final CHEdge edge, final int startId) {
		if (edge == null) {
			return new CHEdge[0];
		}

		if (edge.getSourceId() != startId) {
			edge.switchSourceAndTarget();
		}

		if (!edge.shortcut) {
			return new CHEdge[] { edge };
		}

		final int bypassedVertexBlockId = graph.getBlockId(edge.bypassedVertexId);
		final int bypassedVertexOffset = graph.getVertexOffset(edge.bypassedVertexId);
		CHEdge path1 = null, path2 = null;

		// bypassed vertex might be higher than the lowest one
		final CHEdge[] fromLowest = getOutgoingEdgesToHigherVertices(
				graph.getVertexOffset(edge.getLowestVertexId()));
		path1 = bestCandidate(path1, fromLowest, edge.bypassedVertexId);

		// bypassed vertex might be lower than the lowest one
		// (only, iff block(low)==block(bypassed)!!!, which is the current one)
		if (bypassedVertexBlockId == blockId) {
			final CHEdge[] toBypassed = getIngoingEdgesFromHigherVertices(bypassedVertexOffset);
			path1 = bestCandidate(path1, toBypassed, edge.getLowestVertexId());
		}

		// bypassed vertex might be lower than the highest one
		// (only, iff block(low)==block(bypassed)!!!, which is the current one)
		if (bypassedVertexBlockId == blockId) {
			final CHEdge[] fromBypassed = getOutgoingEdgesToHigherVertices(bypassedVertexOffset);
			path2 = bestCandidate(path2, fromBypassed, edge.getHighestVertexId());
		}

		// bypassed might be higher than the highest one
		// (only, iff block(low)==block(high)!!!, which is the current one)
		if (graph.getBlockId(edge.getHighestVertexId()) == blockId) {
			final CHEdge[] toHighest = getIngoingEdgesFromHigherVertices(
					graph.getVertexOffset(edge.getHighestVertexId()));
			path2 = bestCandidate(path2, toHighest, edge.bypassedVertexId);
		}

		if (path1.getSourceId() != startId && path1.getTargetId() != startId) {
			final CHEdge tmp = path1;
			path1 = path2;
			path2 = tmp;
		}

		// current path may still contain other internal shortcuts
		final CHEdge[] pathPart1 = unpackInternalShortcut(path1, startId);
		final CHEdge[] pathPart2 = unpackInternalShortcut(path2,
				pathPart1[pathPart1.length - 1].getTargetId());
		final CHEdge[] unpacked = ArrayUtils.combine(pathPart1, pathPart2);

		if (path1 != null && path1.shortcut) {
			graph.release(path1);
		}
		if (path2 != null && path2.shortcut) {
			graph.release(path2);
		}

		return unpacked;
	}

	/**
	 * Unpacks the given internal backward shortcut edge to a sequence of normal / real edges.
	 * 
	 * @param edge
	 *            The shortcut edge.
	 * @param startId
	 *            The path's start vertex' identifier.
	 * @return The sequence of normal edges.
	 */
	protected CHEdge[] unpackInternalBwdShortcut(final CHEdge edge, final int startId) {
		if (edge == null) {
			return new CHEdge[0];
		}

		if (edge.getSourceId() != startId) {
			edge.switchSourceAndTarget();
		}

		if (!edge.shortcut) {
			return new CHEdge[] { edge };
		}

		final int bypassedVertexBlockId = graph.getBlockId(edge.bypassedVertexId);
		final int bypassedVertexOffset = graph.getVertexOffset(edge.bypassedVertexId);
		CHEdge path1 = null, path2 = null;

		// bypassed vertex might be lower than the highest one
		// (only, iff block(low)==block(bypassed)!!!, which is the current one)
		if (bypassedVertexBlockId == blockId) {
			final CHEdge[] toBypassed = getIngoingEdgesFromHigherVertices(bypassedVertexOffset);
			path1 = bestCandidate(path1, toBypassed, edge.getHighestVertexId());
		}

		// bypassed might be higher than the highest one:
		// (only, iff block(low)==block(high)!!!, which is the current one)
		if (graph.getBlockId(edge.getHighestVertexId()) == blockId) {
			final CHEdge[] fromHighest = getOutgoingEdgesToHigherVertices(
					graph.getVertexOffset(edge.getHighestVertexId()));
			path1 = bestCandidate(path1, fromHighest, edge.bypassedVertexId);
		}

		// bypassed vertex might be lower than the lowest one:
		// (only, iff block(low)==block(bypassed)!!!, which is the current one)
		if (bypassedVertexBlockId == blockId) {
			final CHEdge[] fromBypassed = getOutgoingEdgesToHigherVertices(bypassedVertexOffset);
			path2 = bestCandidate(path2, fromBypassed, edge.getLowestVertexId());
		}

		// bypassed vertex might be higher than the lowest one
		final CHEdge[] toLowest = getIngoingEdgesFromHigherVertices(
				graph.getVertexOffset(edge.getLowestVertexId()));
		path2 = bestCandidate(path2, toLowest, edge.bypassedVertexId);

		// current path may still contain other internal shortcuts
		final CHEdge[] pathPart1 = unpackInternalShortcut(path1, startId);
		final CHEdge[] pathPart2 = unpackInternalShortcut(path2,
				pathPart1[pathPart1.length - 1].getTargetId());
		final CHEdge[] unpacked = ArrayUtils.combine(pathPart1, pathPart2);

		if (path1 != null && path1.shortcut) {
			graph.release(path1);
		}
		if (path2 != null && path2.shortcut) {
			graph.release(path2);
		}

		return unpacked;
	}

	/**
	 * Returns the best candidate out of a set of candidates, which has the given vertex as its highest
	 * vertex and the lowest weight of all of the candidates.
	 * 
	 * @param current
	 *            The previous (current) candidate. Might be {@code null}.
	 * @param candidates
	 *            The new candidates, which have to be checked.
	 * @param highVertexId
	 *            The identifier of the vertex, which should be the highest of a possible candidate.
	 * @return The best candidate out of all candidates.
	 */
	protected CHEdge bestCandidate(CHEdge current, final CHEdge[] candidates, final int highVertexId) {
		for (final CHEdge candidate : candidates) {
			if (candidate.getHighestVertexId() == highVertexId
					&& (current == null || candidate.weight < current.weight)) {
				if (current != null) {
					graph.release(current);
				}

				current = candidate;

			} else {
				graph.release(candidate);
			}
		}

		return current;
	}

	/**
	 * Unpacks the given external shortcut edge to a sequence of normal / real edges.
	 * 
	 * @param edge
	 *            The shortcut edge.
	 * @param startId
	 *            The path's start vertex' identifier.
	 * @return The sequence of normal edges.
	 */
	protected CHEdge[] unpackExternalShortcut(final CHEdge edge, int startId) {
		final CHEdge[] unpacked = new CHEdge[edge.shortcutPathLength];

		// get recycled offset object
		final Offset offsetEdges = graph.poolOffsets.borrow();
		final Offset offsetPath = graph.poolOffsets.borrow();
		// move the the path's edge offsets
		offsetPath.set(0, edge.shortcutPathBitOffset);

		for (int i = 0; i < edge.shortcutPathLength; i++) {
			final int edgeBitOffset = Deserializer.readUInt(data, bitsPerEdgeOffset, offsetPath);
			offsetEdges.set(0, edgeBitOffset);

			final CHEdge current = readEdge(offsetEdges);
			if (current.getSourceId() != startId) {
				current.switchSourceAndTarget();
			}

			unpacked[i] = current;
			startId = current.getTargetId();
		}

		graph.poolOffsets.release(offsetEdges);
		graph.poolOffsets.release(offsetPath);

		return unpacked;
	}

}
