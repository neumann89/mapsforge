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
package org.mapsforge.routing.ch.preprocessing.graph;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.Rect;

/**
 * Contraction Hierarchies graph.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHGraphImpl.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class CHGraphImpl implements CHGraph {
	// TODO: all fields needed, here?
	// TODO: documentation for all fields

	private final int numVertices;
	private final int numEdges;
	private final int numShortcuts;
	private final String[] osmStreetTypes;

	private final int minLongitudeE6;
	private final int maxLongitudeE6;
	private final int minLatitudeE6;
	private final int maxLatitudeE6;

	private final int[] vertexLevels;
	private final int[] longitudesE6;
	private final int[] latitudesE6;
	private final int[][] outgoingEdgesPerVertex;
	private final int[][] edgesPerVertex;

	private final String[] namePerOriginalEdge;
	private final String[] refPerOriginalEdge;
	private final boolean[] roundaboutPerOriginalEdge;
	private final int[][] longitudesPerOriginalEdge;
	private final int[][] latitudesPerOriginalEdge;
	private final int[] streetTypePerOriginalEdge;

	private final int[] originalEdgePerEdge;
	private final int[] sourcePerEdge;
	private final int[] targetPerEdge;
	private final int[] weightPerEdge;
	private final boolean[] undirectedPerEdge;
	private final int[] bypassedEdge1ByShortcut;
	private final int[] bypassedEdge2ByShortcut;

	/**
	 * Reads the data from the given database connection and constructs a Contraction Hierarchies graph
	 * from it.
	 * 
	 * @param connection
	 *            The database connection, from which the data can be retrieved.
	 * @throws SQLException
	 *             if there was a problem with reading the data.
	 */
	public CHGraphImpl(final Connection connection) throws SQLException {
		final DatabaseReader reader = new DatabaseReader(connection);

		final Integer[] boundingBoxCalc = new Integer[4];

		// read all OSM street types
		osmStreetTypes = reader.getOsmStreetTypes();

		// prepare vertex data
		numVertices = reader.getNumVertices();
		vertexLevels = new int[numVertices];
		longitudesE6 = new int[numVertices];
		latitudesE6 = new int[numVertices];

		outgoingEdgesPerVertex = new int[numVertices][];
		TIntArrayList[] outgoingEdgesLists = new TIntArrayList[numVertices];

		edgesPerVertex = new int[numVertices][];
		TIntSet[] edgesLists = new TIntSet[numVertices];

		// read all vertices
		Iterator<DatabaseReader.Vertex> vertexIterator = reader.getVertices();
		while (vertexIterator.hasNext()) {
			addVertex(vertexIterator.next(), boundingBoxCalc, outgoingEdgesLists, edgesLists);
		}

		// prepare original edge data
		final int numOriginalEdges = reader.getNumOriginalEdges();
		namePerOriginalEdge = new String[numOriginalEdges];
		refPerOriginalEdge = new String[numOriginalEdges];
		roundaboutPerOriginalEdge = new boolean[numOriginalEdges];
		streetTypePerOriginalEdge = new int[numOriginalEdges];
		longitudesPerOriginalEdge = new int[numOriginalEdges][];
		latitudesPerOriginalEdge = new int[numOriginalEdges][];

		// read all original edges
		Iterator<DatabaseReader.OriginalEdge> originalEdgeIterator = reader.getOriginalEdges();
		while (originalEdgeIterator.hasNext()) {
			addOriginalEdge(originalEdgeIterator.next(), boundingBoxCalc);
		}

		// set the bounding box values (min/max of longitude/latitude)
		this.minLongitudeE6 = boundingBoxCalc[0];
		this.maxLongitudeE6 = boundingBoxCalc[1];
		this.minLatitudeE6 = boundingBoxCalc[2];
		this.maxLatitudeE6 = boundingBoxCalc[3];

		// prepare for edge data
		numEdges = reader.getNumEdges();
		numShortcuts = reader.getNumShortcuts();
		originalEdgePerEdge = new int[numEdges];
		sourcePerEdge = new int[numEdges];
		targetPerEdge = new int[numEdges];
		weightPerEdge = new int[numEdges];
		undirectedPerEdge = new boolean[numEdges];
		bypassedEdge1ByShortcut = new int[numShortcuts];
		bypassedEdge2ByShortcut = new int[numShortcuts];

		// read all edges
		Iterator<DatabaseReader.Edge> edgeIterator = reader.getEdges();
		while (edgeIterator.hasNext()) {
			addEdge(edgeIterator.next(), outgoingEdgesLists, edgesLists);
		}

		// convert edge lists to arrays
		for (int i = 0; i < numVertices; i++) {
			outgoingEdgesPerVertex[i] = outgoingEdgesLists[i].toArray();
			edgesPerVertex[i] = edgesLists[i].toArray();
		}
	}

	/**
	 * Adds a vertex to this graph.
	 * 
	 * @param vertex
	 *            The database' vertex, which has to be added.
	 * @param boundingBox
	 *            The data collector for the bounding box data, which will be updated.
	 * @param outgoingEdgesLists
	 *            The lists of outgoing edges per vertex.
	 * @param edgesLists
	 *            The lists of edges per vertex.
	 */
	private void addVertex(final DatabaseReader.Vertex vertex, final Integer[] boundingBox,
			final TIntArrayList[] outgoingEdgesLists, final TIntSet[] edgesLists) {
		final int longitudeE6 = GeoCoordinate.doubleToInt(vertex.longitude);
		final int latitudeE6 = GeoCoordinate.doubleToInt(vertex.latitude);

		longitudesE6[vertex.id] = longitudeE6;
		latitudesE6[vertex.id] = latitudeE6;
		vertexLevels[vertex.id] = vertex.level;

		// find the min/max for longitude/latitude
		if (boundingBox[0] == null || boundingBox[0] > longitudeE6) {
			boundingBox[0] = longitudeE6;
		}
		if (boundingBox[1] == null || boundingBox[1] < longitudeE6) {
			boundingBox[1] = longitudeE6;
		}

		if (boundingBox[2] == null || boundingBox[2] > latitudeE6) {
			boundingBox[2] = latitudeE6;
		}
		if (boundingBox[3] == null || boundingBox[3] < latitudeE6) {
			boundingBox[3] = latitudeE6;
		}

		outgoingEdgesLists[vertex.id] = new TIntArrayList(12);
		edgesLists[vertex.id] = new TIntHashSet(24);
	}

	/**
	 * Adds the data of an original edge to this graph.
	 * 
	 * @param originalEdge
	 *            The database' original edge, which has to be added.
	 * @param boundingBox
	 *            The data collector for the bounding box data, which will be updated.
	 */
	private void addOriginalEdge(final DatabaseReader.OriginalEdge originalEdge,
			final Integer[] boundingBox) {
		namePerOriginalEdge[originalEdge.id] = originalEdge.name;
		refPerOriginalEdge[originalEdge.id] = originalEdge.ref;
		roundaboutPerOriginalEdge[originalEdge.id] = originalEdge.roundabout;
		streetTypePerOriginalEdge[originalEdge.id] = originalEdge.osmStreetType;

		longitudesPerOriginalEdge[originalEdge.id] = new int[originalEdge.longitudes.length];
		for (int i = 0; i < originalEdge.longitudes.length; i++) {
			final int longitudeE6 = GeoCoordinate.doubleToInt(originalEdge.longitudes[i]);
			longitudesPerOriginalEdge[originalEdge.id][i] = longitudeE6;

			// find the min/max for longitude
			if (boundingBox[0] > longitudeE6) {
				boundingBox[0] = longitudeE6;
			}
			if (boundingBox[1] < longitudeE6) {
				boundingBox[1] = longitudeE6;
			}
		}

		latitudesPerOriginalEdge[originalEdge.id] = new int[originalEdge.latitudes.length];
		for (int i = 0; i < originalEdge.latitudes.length; i++) {
			final int latitudeE6 = GeoCoordinate.doubleToInt(originalEdge.latitudes[i]);
			latitudesPerOriginalEdge[originalEdge.id][i] = latitudeE6;

			// find the min/max for latitude
			if (boundingBox[2] > latitudeE6) {
				boundingBox[2] = latitudeE6;
			}
			if (boundingBox[3] < latitudeE6) {
				boundingBox[3] = latitudeE6;
			}
		}
	}

	/**
	 * Adds an edge to this graph.
	 * 
	 * @param edge
	 *            The database' edge, which has to be added.
	 * @param outgoingEdgesLists
	 *            The lists of outgoing edges per vertex.
	 * @param edgesLists
	 *            The lists of edges per vertex.
	 */
	private void addEdge(final DatabaseReader.Edge edge, final TIntArrayList[] outgoingEdgesLists,
			final TIntSet[] edgesLists) {
		originalEdgePerEdge[edge.id] = edge.originalEdgeId;
		sourcePerEdge[edge.id] = edge.sourceId;
		targetPerEdge[edge.id] = edge.targetId;
		weightPerEdge[edge.id] = edge.weight;
		undirectedPerEdge[edge.id] = edge.undirected;

		if (edge.originalEdgeId == -1) {
			final int shortcutId = fromEdgeIdToShortcutId(edge.id);
			bypassedEdge1ByShortcut[shortcutId] = edge.bypassedEdgeId1;
			bypassedEdge2ByShortcut[shortcutId] = edge.bypassedEdgeId2;
		}

		outgoingEdgesLists[edge.sourceId].add(edge.id);
		if (edge.undirected) {
			outgoingEdgesLists[edge.targetId].add(edge.id);
		}

		edgesLists[edge.sourceId].add(edge.id);
		edgesLists[edge.targetId].add(edge.id);
	}

	/**
	 * Converts an edge's identifier to a shortcut identifier.
	 * 
	 * @param edgeId
	 *            The edge's identifier.
	 * @return The shortcut identifier.
	 */
	private int fromEdgeIdToShortcutId(final int edgeId) {
		return edgeId - numEdges + numShortcuts;
	}

	/**
	 * Returns the longitude values (E6 format) of all vertices.
	 * 
	 * @return The longitude values (E6 format) of all vertices.
	 */
	public int[] getVertexLongitudesE6() {
		return longitudesE6;
	}

	/**
	 * Returns the latitudes values (E6 format) of all vertices.
	 * 
	 * @return The latitudes values (E6 format) of all vertices.
	 */
	public int[] getVertexLatitudesE6() {
		return latitudesE6;
	}

	/**
	 * Returns the graph's bounding box, containing all vertices and edge waypoints.
	 * 
	 * @return The graph's bounding box, containing all vertices and edge waypoints.
	 */
	public Rect getBoundingBox() {
		return new Rect(minLongitudeE6, maxLongitudeE6, minLatitudeE6, maxLatitudeE6);
	}

	@Override
	public String[] getOsmStreetTypes() {
		return osmStreetTypes.clone();
	}

	@Override
	public Iterator<CHVertexImpl> getVertices() {
		return new Iterator<CHVertexImpl>() {

			/**
			 * Next position.
			 */
			private int pos = 0;

			@Override
			public boolean hasNext() {
				return pos < numVertices;
			}

			@Override
			public CHVertexImpl next() {
				return new CHVertexImpl(pos++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove() is not supported.");
			}
		};
	}

	@Override
	public int numVertices() {
		return numVertices;
	}

	@Override
	public int numEdges() {
		return numEdges;
	}

	@Override
	public int numShortcuts() {
		return numShortcuts;
	}

	@Override
	public CHVertexImpl getVertex(final int id) {
		return new CHVertexImpl(id);
	}

	/**
	 * Returns the edge's other endpoint, which might be its source or target.
	 * 
	 * @param edgeId
	 *            The edge's identifier.
	 * @param vertexId
	 *            The related vertex' identifier (the edge's "source" or "target").
	 * @return The edge's other endpoint.
	 */
	public int getOtherVertexId(final int edgeId, final int vertexId) {// TODO: remove?
		final int sourceId = sourcePerEdge[edgeId];
		final int targetId = targetPerEdge[edgeId];

		return sourceId != vertexId ? sourceId : targetId;
	}

	/**
	 * Vertex of this Contraction Hierarchies graph.
	 */
	class CHVertexImpl implements CHVertex {

		/**
		 * Its identifier.
		 */
		private final int id;

		/**
		 * Constructs a vertex instance for the given identifier.
		 * 
		 * @param id
		 *            The vertex' identifier.
		 */
		public CHVertexImpl(final int id) {
			this.id = id;
		}

		@Override
		public int getId() {
			return id;
		}

		@Override
		public CHEdgeImpl[] getOutboundEdges() {
			final int numOutgoingEdges = outgoingEdgesPerVertex[id].length;
			final CHEdgeImpl[] edges = new CHEdgeImpl[numOutgoingEdges];
			for (int i = 0; i < numOutgoingEdges; i++) {
				edges[i] = new CHEdgeImpl(outgoingEdgesPerVertex[id][i]);
			}

			return edges;
		}

		@Override
		public CHEdge[] getEdgesFromOrToHigherVertices() {
			final int ownLevel = vertexLevels[id];
			final int numTotal = edgesPerVertex[id].length;
			final CHEdgeImpl[] tmp = new CHEdgeImpl[numTotal];

			int numToOrFromHigher = 0;
			for (final int edgeId : edgesPerVertex[id]) {
				final int sourceId = sourcePerEdge[edgeId];
				final int targetId = targetPerEdge[edgeId];
				final int otherId = sourceId != id ? sourceId : targetId;
				final int otherLevel = vertexLevels[otherId];

				// both could be at the same level, use the own id as further criteria
				if (otherLevel > ownLevel || (otherLevel == ownLevel && otherId > id)) {
					// TODO: if (otherLevel > ownLevel) {
					tmp[numToOrFromHigher++] = new CHEdgeImpl(edgeId);
				}
			}

			final CHEdgeImpl[] edges = new CHEdgeImpl[numToOrFromHigher];
			System.arraycopy(tmp, 0, edges, 0, numToOrFromHigher);

			return edges;
		}

		@Override
		public int getLevel() {
			return vertexLevels[id];
		}

		@Override
		public GeoCoordinate getCoordinate() {
			return new GeoCoordinate(latitudesE6[id], longitudesE6[id]);
		}
	}

	/**
	 * Edge of this Contraction Hierarchies graph.
	 */
	class CHEdgeImpl implements CHEdge {

		/**
		 * Its identifier.
		 */
		private final int id;

		/**
		 * Constructs an edge instance for the given identifier.
		 * 
		 * @param id
		 *            The edge's identifier.
		 */
		public CHEdgeImpl(final int id) {
			this.id = id;
		}

		@Override
		public int getId() {
			return id;
		}

		@Override
		public CHVertex getSource() {
			return new CHVertexImpl(sourcePerEdge[id]);
		}

		@Override
		public CHVertex getTarget() {
			return new CHVertexImpl(targetPerEdge[id]);
		}

		@Override
		public int getSourceId() {
			return sourcePerEdge[id];
		}

		@Override
		public int getTargetId() {
			return targetPerEdge[id];
		}

		@Override
		public int getHighestVertexId() {
			final int source = sourcePerEdge[id];
			final int target = targetPerEdge[id];
			final int sourceLevel = vertexLevels[source];
			final int targetLevel = vertexLevels[target];

			// both could be at the same level, use the own id as further criteria
			return sourceLevel > targetLevel || (sourceLevel == targetLevel && source > target) ? source
					: target;
		}

		@Override
		public int getLowestVertexId() {
			final int source = sourcePerEdge[id];
			final int target = targetPerEdge[id];
			final int sourceLevel = vertexLevels[source];
			final int targetLevel = vertexLevels[target];

			// both could be at the same level, use the own id as further criteria
			return sourceLevel < targetLevel || (sourceLevel == targetLevel && source < target) ? source
					: target;
		}

		@Override
		public boolean isUndirected() {
			return undirectedPerEdge[id];
		}

		@Override
		public int getWeight() {
			return weightPerEdge[id];
		}

		@Override
		public GeoCoordinate[] getWaypoints() {
			final int oeId = originalEdgePerEdge[id];
			if (oeId == -1 || latitudesPerOriginalEdge[oeId] == null) {
				return new GeoCoordinate[0];
			}

			final GeoCoordinate[] waypoints = new GeoCoordinate[latitudesPerOriginalEdge[oeId].length];
			for (int i = 0; i < latitudesPerOriginalEdge[oeId].length; i++) {
				waypoints[i] = new GeoCoordinate(latitudesPerOriginalEdge[oeId][i],
						longitudesPerOriginalEdge[oeId][i]);
			}

			return waypoints;
		}

		@Override
		public String getName() {
			final int oeId = originalEdgePerEdge[id];

			return oeId != -1 ? namePerOriginalEdge[oeId] : null;
		}

		@Override
		public String getRef() {
			final int oeId = originalEdgePerEdge[id];

			return oeId != -1 ? refPerOriginalEdge[oeId] : null;
		}

		@Override
		public int getType() {
			final int oeId = originalEdgePerEdge[id];

			return oeId != -1 ? streetTypePerOriginalEdge[oeId] : -1;
		}

		@Override
		public boolean isRoundabout() {
			final int oeId = originalEdgePerEdge[id];

			return oeId != -1 && roundaboutPerOriginalEdge[oeId];
		}

		@Override
		public boolean isShortcut() {
			return isShortcutId(id);
		}

		@Override
		public int getBypassedVertexId() {
			int vertexId = -1;
			if (isShortcutId(id)) {
				final int shortcutId = fromEdgeIdToShortcutId(id);

				final int bypassedEdgeId1 = bypassedEdge1ByShortcut[shortcutId];
				final int bypassedEdgeId2 = bypassedEdge2ByShortcut[shortcutId];

				final int source1 = sourcePerEdge[bypassedEdgeId1];
				final int target1 = targetPerEdge[bypassedEdgeId1];
				final int source2 = sourcePerEdge[bypassedEdgeId2];
				final int target2 = targetPerEdge[bypassedEdgeId2];

				vertexId = (source1 == source2 || source1 == target2) ? source1 : target1;
			}

			return vertexId;
		}

        @Override
        public ShortcutPath unpack(final int startVertexId) {
            if (isShortcutId(id)) {
                final ArrayList<CHEdge> edgeList = new ArrayList<CHEdge>();
                final ArrayList<CHEdge> shortcutList = new ArrayList<CHEdge>();
                unpack(startVertexId, edgeList, shortcutList, id);

                return new ShortcutPathImpl(
                        edgeList.toArray(new CHEdge[edgeList.size()]),
                        shortcutList.toArray(new CHEdge[shortcutList.size()]));
            }

            return new ShortcutPathImpl(new CHEdge[] { this }, new CHEdge[0]);
        }

		/**
		 * Unpacks the path related to an shortcut edge or uses the edge itself as the path. The given
		 * source will be used as the path's start. This method is also aware of undirected edges, but
		 * the added path's edges are not totally aware of it by themselves. The source of an edge might
		 * actually be the target with regards to this path.
		 * 
		 * @param startVertexId
		 *            The path's start.
		 * @param edgeList
		 *            The path's edges will be added to that list.
		 * @param shortcutList
         *            All used shortcut edges will be added to that list.
         * @param edgeId
		 *            The edge's identifier.
		 */
		private void unpack(final int startVertexId, final ArrayList<CHEdge> edgeList, final ArrayList<CHEdge> shortcutList, final int edgeId) {
			if (sourcePerEdge[edgeId] != startVertexId
					&& (!undirectedPerEdge[edgeId] || targetPerEdge[edgeId] != startVertexId)) {
				throw new IllegalArgumentException(
						String.format(
								"[startVertexId] has to be the source of the edge for directed edges or one of the endpoints of undirected edges.\n"
										+ "\t{start: %d, edge: %d, undirected: %b, source: %d, target: %d}",
								startVertexId, edgeId, undirectedPerEdge[edgeId],
								sourcePerEdge[edgeId], targetPerEdge[edgeId]
								));
			}

			if (isShortcutId(edgeId)) {
                shortcutList.add(new CHEdgeImpl(edgeId));

				final int shortcutId = fromEdgeIdToShortcutId(edgeId);
				final int edge1 = bypassedEdge1ByShortcut[shortcutId];
				final int edge2 = bypassedEdge2ByShortcut[shortcutId];
				final int e1Source = sourcePerEdge[edge1];
				final int e1Target = targetPerEdge[edge1];

				if (e1Source == startVertexId || e1Target == startVertexId) {
					unpack(e1Source == startVertexId ? e1Source : e1Target, edgeList, shortcutList, edge1);
					unpack(e1Source == startVertexId ? e1Target : e1Source, edgeList, shortcutList, edge2);

				} else {
					final int e2Source = sourcePerEdge[edge2];
					final int e2Target = targetPerEdge[edge2];
					unpack(e2Source == startVertexId ? e2Source : e2Target, edgeList, shortcutList, edge2);
					unpack(e2Source == startVertexId ? e2Target : e2Source, edgeList, shortcutList, edge1);

				}

			} else {
				edgeList.add(new CHEdgeImpl(edgeId));
			}
		}

		/**
		 * Checks, if the edge's identifier is a shortcut identifier and therefore, if the edge itself
		 * is a shortcut.
		 * 
		 * @param edgeId
		 *            The edge's identifier.
		 * @return Whether the edge's identifier is a shortcut identifier or not.
		 */
		private boolean isShortcutId(final int edgeId) {
			return edgeId >= numEdges - numShortcuts;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof CHEdgeImpl))
				return false;

			CHEdgeImpl chEdge = (CHEdgeImpl) o;

			return id == chEdge.id;
		}

		@Override
		public int hashCode() {
			return id;
		}
	}

    class ShortcutPathImpl implements ShortcutPath {
        private final CHEdge[] path;
        private final CHEdge[] usedShortcuts;

        public ShortcutPathImpl(final CHEdge[] path, final CHEdge[] usedShortcuts) {
            this.path = path;
            this.usedShortcuts = usedShortcuts;
        }

        public CHEdge[] getPath() {
            return path;
        }

        public CHEdge[] getUsedShortcuts() {
            return usedShortcuts;
        }
    }

}
