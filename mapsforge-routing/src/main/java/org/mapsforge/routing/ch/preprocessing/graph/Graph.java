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

import gnu.trove.set.hash.TIntHashSet;
import org.mapsforge.routing.ch.preprocessing.evaluation.Statistics;
import org.mapsforge.routing.graph.RgDAO;
import org.mapsforge.routing.graph.RgEdge;
import org.mapsforge.routing.graph.RgVertex;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A graph implementation, used for Contraction Hierarchies' preprocessing.
 * 
 * @author Patrick Jungermann
 * @version $Id: Graph.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class Graph {

	/**
	 * Logger, used for this class.
	 */
	private static final Logger LOGGER = Logger.getLogger(Graph.class.getName());

	/**
	 * Used as initial value and growing size for the growable arrays, used for the ingoing and outgoing
	 * edges of a vertex.
	 * <p/>
	 * Statistics for the OSM data for Berlin:
	 * <table>
	 * <thead>
	 * <tr>
	 * <th>#neighbors</th>
	 * <th>percent of vertices</th>
	 * </tr>
	 * </thead> <tbody>
	 * <tr>
	 * <td>0</td>
	 * <td>36 / 33496 = 0.00107475519465011</td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td>6076 / 33496 = 0.181394793408168</td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td>8835 / 33496 = 0.263762837353714</td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td>14466 / 33496 = 0.431872462383568</td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td>4056 / 33496 = 0.121089085263912</td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td>26 / 33496 = 0.000776212085025078</td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td>1 / 33496 = 2.9854310962503e-005</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 */
	private static final int GROW_SIZE_IN_OUT_EDGES = 2;
	/**
	 * Growing factor, used for the arrays related to edges.
	 */
	private static final float GROW_FACTOR_EDGES = 1.33f;

	/**
	 * The number of vertices.
	 */
	private final int numVertices;
	/**
	 * The number of (normal) edges.
	 */
	private int numEdges;
	/**
	 * The number of shortcut edges.
	 */
	private int numShortcuts;
	/**
	 * Holds the next edge id;
	 */
	private int nextEdgeId = 0;
	/**
	 * An array of outgoing edges for each vertex (= source).
	 */
	private final int[][] outgoingEdgesPerSource;
	/**
	 * An array of ingoing edges for each vertex (= target).
	 */
	private final int[][] ingoingEdgesPerTarget;
	/**
	 * The number of outgoing edges for each vertex (= source).
	 */
	private final int[] numOfOutgoingEdgesPerSource;
	/**
	 * The number of ingoing edges for each vertex (= target).
	 */
	private final int[] numOfIngoingEdgesPerTarget;
	/**
	 * The source of each edge.
	 */
	private int[] sourcePerEdge;
	/**
	 * The target of each edge.
	 */
	private int[] targetPerEdge;
	/**
	 * The weight of each edge.
	 */
	private int[] weightPerEdge;
	/**
	 * Whether the edge is undirected (forward + backward), or not (forward).
	 */
	private boolean[] undirectedPerEdge;
	/**
	 * Hierarchy depth of each vertex. The value of a vertex u will be updated during the contraction of
	 * each neighbor v.<br/>
	 * Definition: depth(v) := max(depth(v), depth(u))
	 */
	private int[] hierarchyDepthPerVertex;
	/**
	 * The number of original edges, represented by the related edge (original ones will only represent
	 * one, shortcut edges will represent more than one edge).<br/>
	 * Definition: o(u,v) := o(u,x) + o(x,v)
	 */
	private int[] originalEdgeCountPerEdge;
	/**
	 * The bypassed edges (with the bypassed vertex in their middle) per shortcut.
	 */
	private int[][] bypassedEdgesPerShortcut;
	/**
	 * Contains the layer of each vertex.
	 */
	private int[] layerPerVertex;

	/**
	 * The latitude value of each vertex.
	 */
	private double[] latitudePerVertex;
	/**
	 * The longitude value of each vertex.
	 */
	private double[] longitudePerVertex;
	/**
	 * The mapping from an edge to a source edge (edge of the loaded graph itself).
	 */
	private int[] sourceEdgePerEdge;

	/**
	 * It is assumed that all vertices are numerated from {@code 0} to {@code numVertices - 1}.
	 * 
	 * @param numVertices
	 *            Number of vertices of this graph.
	 * @param numEdges
	 *            Number of edges of this graph
	 */
	protected Graph(final int numVertices, final int numEdges) {
		this.numVertices = numVertices;
		this.numEdges = 0;
		this.numShortcuts = 0;

		// additional vertex data
		this.latitudePerVertex = new double[numVertices];
		this.longitudePerVertex = new double[numVertices];

		this.layerPerVertex = new int[numVertices];

		this.outgoingEdgesPerSource = new int[numVertices][];
		this.ingoingEdgesPerTarget = new int[numVertices][];
		this.numOfOutgoingEdgesPerSource = new int[numVertices];
		this.numOfIngoingEdgesPerTarget = new int[numVertices];

		this.hierarchyDepthPerVertex = new int[numVertices];
		Arrays.fill(this.hierarchyDepthPerVertex, 1);

		// assumed that there are some undirected edges -> apply grow factor
		final int numEdgesGrown = newEdgesArraySize(numEdges);
		this.sourcePerEdge = new int[numEdgesGrown];
		this.targetPerEdge = new int[numEdgesGrown];
		this.weightPerEdge = new int[numEdgesGrown];
		this.undirectedPerEdge = new boolean[numEdgesGrown];
		this.sourceEdgePerEdge = new int[numEdgesGrown];

		this.originalEdgeCountPerEdge = new int[numEdgesGrown];
		Arrays.fill(this.originalEdgeCountPerEdge, 1);

		// add / initialize vertices
		for (int i = 0; i < numVertices; i++) {
			this.outgoingEdgesPerSource[i] = new int[0];
			this.ingoingEdgesPerTarget[i] = new int[0];
			this.numOfOutgoingEdgesPerSource[i] = 0;
			this.numOfIngoingEdgesPerTarget[i] = 0;
		}

		// initialize shortcuts
		this.bypassedEdgesPerShortcut = new int[0][];
	}

	/**
	 * Loads the data from the given connection and creates a graph representing this data.<br/>
	 * <strong>Attention:</strong> This method will not close the connection. You have to close it by
	 * yourself.
	 * 
	 * @param connection
	 *            The database connection, which has to be used to retrieve the graph's data.
	 * @return The created {@link Graph} instance, representing the data.
	 * @throws SQLException
	 *             if there was any SQL related error.
	 */
	public static Graph loadGraph(final Connection connection) throws SQLException {
		final RgDAO dao = new RgDAO(connection);
		int sourceId, targetId, weight, id, edgeId;

		final Graph graph = new Graph(dao.getNumVertices(), dao.getNumEdges());

		// add additional vertex data
		for (RgVertex vertex : dao.getVertices()) {
			id = vertex.getId();
			graph.latitudePerVertex[id] = vertex.getLatitude();
			graph.longitudePerVertex[id] = vertex.getLongitude();
		}

		// add edges
		for (RgEdge edge : dao.getEdges()) {
			sourceId = edge.getSourceId();
			targetId = edge.getTargetId();
			weight = edge.getWeight();
			edgeId = edge.getId();

			// add edges
			id = graph.addEdge(sourceId, targetId, weight, edge.isUndirected());
			graph.sourceEdgePerEdge[id] = edgeId;
		}

		if (LOGGER.isLoggable(Level.INFO)) {
			LOGGER.info("graph loading finished");
		}

		return graph;
	}

	/**
	 * Saves the graph to a database, given by a connection to it.<br/>
	 * <strong>Attention:</strong> This method will not close the connection. You have to close it by
	 * yourself.
	 * 
	 * @param connection
	 *            The connection to a database.
	 * @return {@code true}, if the graph was saved successfully, otherwise {@code false}.
	 */
	public boolean saveGraph(final Connection connection) {
		try {
			final DatabaseWriter writer = new DatabaseWriter(connection);
			writer.createTables();

			for (int i = 0; i < numVertices; i++) {
				writer.insertVertex(i, layerPerVertex[i], hierarchyDepthPerVertex[i]);
			}
			writer.commit();

			final int diffEdgeToShortcutId = numEdges - numShortcuts;
			for (int i = 0; i < numEdges; i++) {
				final boolean isShortcut = originalEdgeCountPerEdge[i] > 1;
				final int sourceEdgeId = isShortcut ? -1 : sourceEdgePerEdge[i];
				final int bypassedEdgeId1 = isShortcut ? bypassedEdgesPerShortcut[i
						- diffEdgeToShortcutId][0] : -1;
				final int bypassedEdgeId2 = isShortcut ? bypassedEdgesPerShortcut[i
						- diffEdgeToShortcutId][1] : -1;

				writer.insertEdge(i, sourcePerEdge[i], targetPerEdge[i], weightPerEdge[i],
						undirectedPerEdge[i], sourceEdgeId, bypassedEdgeId1, bypassedEdgeId2,
						originalEdgeCountPerEdge[i]);
			}
			writer.commit();

			return true;

		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE,
					"Error at executing an SQL statement - " + e.getClass() + ": " + e.getMessage(), e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to read the SQL file with the create table statements - "
					+ e.getClass() + ": " + e.getMessage(), e);
		}

		return false;
	}

	/**
	 * Adds an edge to this graph.
	 * 
	 * @param sourceId
	 *            The identifier of the source vertex of this edge.
	 * @param targetId
	 *            The identifier of the target vertex of this edge.
	 * @param weight
	 *            The weight of this edge.
	 * @param undirected
	 *            Whether this edge should be undirected or not.
	 * @return The identifier, used for the new edge.
	 */
	protected int addEdge(final int sourceId, final int targetId, final int weight,
			final boolean undirected) {
		// add the edge's information
		final int newEdgeId = getNextEdgeId();

		ensureCapacityForEdge(newEdgeId);
		sourcePerEdge[newEdgeId] = sourceId;
		targetPerEdge[newEdgeId] = targetId;
		weightPerEdge[newEdgeId] = weight;
		undirectedPerEdge[newEdgeId] = undirected;
		originalEdgeCountPerEdge[newEdgeId] = 1;

		addOutgoingAndIngoingRelations(sourceId, targetId, newEdgeId);
		if (undirected) {
			addOutgoingAndIngoingRelations(targetId, sourceId, newEdgeId);
		}

		// update counter
		numEdges++;

		return newEdgeId;
	}

	// TODO: documentation
	private void addOutgoingAndIngoingRelations(final int sourceId, final int targetId, final int edgeId) {
		addEdgeToVertexRelation(edgeId, sourceId, numOfOutgoingEdgesPerSource, outgoingEdgesPerSource);
		addEdgeToVertexRelation(edgeId, targetId, numOfIngoingEdgesPerTarget, ingoingEdgesPerTarget);
	}

	// TODO: documentation
	private void addEdgeToVertexRelation(final int edgeId, final int vertexId,
			final int[] counterPerVertex, final int[][] edgesPerVertex) {
		// add the edge to the vertex's ones
		// increase the counter at the end!

		// get the state
		final int num = counterPerVertex[vertexId] + 1;
		int[] edges = edgesPerVertex[vertexId];

		// ensure capacity
		if (num > edges.length) {
			edges = increaseSizeTo(edges, edges.length + GROW_SIZE_IN_OUT_EDGES);
			edgesPerVertex[vertexId] = edges;
		}

		// add the relation
		edges[num - 1] = edgeId;
		counterPerVertex[vertexId] = num;
	}

	/**
	 * Ensures that all edge related arrays have enough space for the new edge.
	 * 
	 * @param newEdgeId
	 *            The new edge's identifier.
	 */
	private void ensureCapacityForEdge(final int newEdgeId) {
		if (newEdgeId > sourcePerEdge.length - 1) {
			final int newSize = newEdgesArraySize(sourcePerEdge.length);

			sourcePerEdge = increaseSizeTo(sourcePerEdge, newSize);
			targetPerEdge = increaseSizeTo(targetPerEdge, newSize);
			weightPerEdge = increaseSizeTo(weightPerEdge, newSize);
			undirectedPerEdge = increaseSizeTo(undirectedPerEdge, newSize);
			originalEdgeCountPerEdge = increaseSizeTo(originalEdgeCountPerEdge, newSize);
			sourceEdgePerEdge = increaseSizeTo(sourceEdgePerEdge, newSize);
		}
	}

	/**
	 * Returns the next edge identifier.
	 * 
	 * @return The next edge identifier.
	 */
	private int getNextEdgeId() {
		return nextEdgeId++;
	}

	/**
	 * Increases the size of the given array to the new size and returns the adjusted array.
	 * 
	 * @param array
	 *            The array, from which the size has to be increased.
	 * @param newSize
	 *            The new size for the array.
	 * @return The new array.
	 */
	private boolean[] increaseSizeTo(final boolean[] array, final int newSize) {
		final boolean[] grown = new boolean[newSize];
		System.arraycopy(array, 0, grown, 0, array.length);

		return grown;
	}

	/**
	 * Increases the size of the given array to the new size and returns the adjusted array.
	 * 
	 * @param array
	 *            The array, from which the size has to be increased.
	 * @param newSize
	 *            The new size for the array.
	 * @return The new array.
	 */
	private int[] increaseSizeTo(final int[] array, final int newSize) {
		final int[] grown = new int[newSize];
		System.arraycopy(array, 0, grown, 0, array.length);

		return grown;
	}

	/**
	 * Increases the size of the given array to the new size and returns the adjusted array.
	 * 
	 * @param array
	 *            The array, from which the size has to be increased.
	 * @param newSize
	 *            The new size for the array.
	 * @return The new array.
	 */
	private int[][] increaseSizeTo(final int[][] array, final int newSize) {
		final int[][] grown = new int[newSize][];
		System.arraycopy(array, 0, grown, 0, array.length);

		return grown;
	}

	/**
	 * Calculates the new size for all edges storing arrays.
	 * 
	 * @param size
	 *            The old size (= maximum number of edges).
	 * @return The new size.
	 */
	private int newEdgesArraySize(final int size) {
		return (int) (size * GROW_FACTOR_EDGES + 1);
	}

	/**
	 * Adds a shortcut edge for two edges, iff the first edge followed by the second edge is a way.<br/>
	 * Thread-safe.
	 * 
	 * @param edgeId1
	 *            Identifier of the first edge.
	 * @param edgeId2
	 *            Identifier of the second edge.
	 * @param undirected
	 *            Whether this shortcut should be undirected or not.
	 * @return The new shortcut's identifier.
	 */
	public synchronized int addShortcut(final int edgeId1, final int edgeId2, final boolean undirected) {
		// check, if they are valid edges
		if (!isValidEdgeId(edgeId1)) {
			throw new IllegalArgumentException("The first edge " + edgeId1 + " is unknown.");
		}
		if (!isValidEdgeId(edgeId2)) {
			throw new IllegalArgumentException("The second edge " + edgeId2 + " is unknown.");
		}
		// check, if this is a valid shortcut
		// if it is an undirected edge, each vertex could be the source or target in this situation
		// but there must be one common vertex for both edges to be a valid shortcut edge pair
		final boolean u1 = undirectedPerEdge[edgeId1];
		final boolean u2 = undirectedPerEdge[edgeId2];
		final int e1v1 = sourcePerEdge[edgeId1];
		final int e1v2 = targetPerEdge[edgeId1];
		final int e2v1 = sourcePerEdge[edgeId2];
		final int e2v2 = targetPerEdge[edgeId2];
		// both are undirected: 4 cases for the intermediate vertex
		boolean valid = u1 && u2 && (e1v1 == e2v1 || e1v1 == e2v2 || e1v2 == e2v1 || e1v2 == e2v2);
		// only the fst is undirected: 2 cases for the intermediate vertex
		valid = valid || (u1 && (e1v1 == e2v1 || e1v2 == e2v1));
		// only the snd is undirected: 2 cases for the intermediate vertex
		valid = valid || (u2 && (e1v2 == e2v1 || e1v2 == e2v2));
		// both are directed: only one possibility
		valid = valid || (e1v2 == e2v1);
		if (!valid) {
			throw new IllegalArgumentException(
					"The target of the first edge is not equal to the target of the second edge."
							+ "Only shortcuts of the scheme (u,x),(x,v)->(u,v) are supported.\n\n"
							+ "\tu1: " + u1 + " -- v1: " + e1v1 + " - v2: " + e1v2 + "\n"
							+ "\tu2: " + u2 + " -- v1: " + e2v1 + " - v2: " + e2v2 + "\n"
							+ "\t=> " + valid);
		}

		// resolve the source and target of the shortcut
		final int sourceId, targetId;
		if (u1 && u2) {
			// both are undirected: 4 cases for the intermediate vertex
			sourceId = e1v1 == e2v1 || e1v1 == e2v2 ? e1v2 : e1v1;
			targetId = e2v1 == e1v1 || e2v1 == e1v2 ? e2v2 : e2v1;

		} else if (u1) {
			// only the fst is undirected: 2 cases for the intermediate vertex
			sourceId = e1v1 == e2v1 ? e1v2 : e1v1;
			targetId = e2v2;

		} else if (u2) {
			// only the snd is undirected: 2 cases for the intermediate vertex
			sourceId = e1v1;
			targetId = e2v1 == e1v2 ? e2v2 : e2v1;

		} else {
			// both are directed: only one possibility
			sourceId = e1v1;
			targetId = e2v2;
		}

		final int weight1 = weightPerEdge[edgeId1], weight2 = weightPerEdge[edgeId2], weight = weight1
				+ weight2;

		// add edge
		final int shortcutId = addEdge(sourceId, targetId, weight, undirected);

		// shortcut related data
		originalEdgeCountPerEdge[shortcutId] = originalEdgeCountPerEdge[edgeId1]
				+ originalEdgeCountPerEdge[edgeId2];
		numShortcuts++;
		if (numShortcuts > bypassedEdgesPerShortcut.length) {
			bypassedEdgesPerShortcut = increaseSizeTo(bypassedEdgesPerShortcut,
					newEdgesArraySize(bypassedEdgesPerShortcut.length));
		}
		bypassedEdgesPerShortcut[numShortcuts - 1] = new int[] { edgeId1, edgeId2 };

		if (LOGGER.isLoggable(Level.FINER)) {
			LOGGER.finer("shortcut " + shortcutId + " added (" + sourceId + ", "
					+ targetPerEdge[edgeId1] + ", " + targetId + ") [" + Thread.currentThread() + "]");
		}

		return shortcutId;
	}

	/**
	 * Updates the hierarchy depth of all related vertices of the contracted vertex.
	 * 
	 * @param contractedVertexId
	 *            The contracted vertex' identifier.
	 */
	public synchronized void updateHierarchyDepths(final int contractedVertexId) {
		final TIntHashSet set = new TIntHashSet();
		for (final int edgeId : getOutgoingEdgesOfVertex(contractedVertexId)) {
			set.add(getOtherVertexOfEdge(edgeId, contractedVertexId));
		}
		for (final int edgeId : getIngoingEdgesOfVertex(contractedVertexId)) {
			set.add(getOtherVertexOfEdge(edgeId, contractedVertexId));
		}

		for (final int neighborId : set.toArray()) {
			updateHierarchyDepth(contractedVertexId, neighborId);
		}
	}

	/**
	 * Updates the hierarchy depth of the contracted vertex' neighbor.
	 * 
	 * @param contractedVertexId
	 *            The contracted vertex' identifier.
	 * @param neighborId
	 *            The contracted vertex' neighbor's identifier.
	 */
	protected void updateHierarchyDepth(final int contractedVertexId, final int neighborId) {
		hierarchyDepthPerVertex[neighborId] = Math.max(hierarchyDepthPerVertex[neighborId],
				hierarchyDepthPerVertex[contractedVertexId] + 1);
	}

	/**
	 * Sets the layer of all vertices to the given one.
	 * 
	 * @param ids
	 *            The vertex' identifiers.
	 * @param layer
	 *            The vertices' (new) layer.
	 */
	public void setVertexLayer(final int[] ids, final int layer) {
		for (final int id : ids) {
			setVertexLayer(id, layer);
		}
	}

	/**
	 * Sets the layer of a vertex to the given one.
	 * 
	 * @param id
	 *            The vertex' identifier.
	 * @param layer
	 *            The vertex' (new) layer.
	 */
	public void setVertexLayer(final int id, final int layer) {
		if (isValidVertexId(id)) {
			layerPerVertex[id] = layer;
		}
	}

	/**
	 * Return the number of vertices of this graph.
	 * 
	 * @return The number of vertices of this graph.
	 */
	public int getNumOfVertices() {
		return numVertices;
	}

	/**
	 * Return the number of edges of this graph (incl. shortcut edges).
	 * 
	 * @return The number of edges of this graph.
	 */
	public int getNumOfEdges() {
		return numEdges;
	}

	/**
	 * Returns the number of shortcuts of this graph.
	 * 
	 * @return The number of shortcuts of this graph.
	 */
	public int getNumOfShortcuts() {
		return numShortcuts;
	}

	/**
	 * Returns the hierarchy depth of the vertex.
	 * 
	 * @param id
	 *            The vertex' identifier.
	 * @return The hierarchy depth of the vertex.
	 */
	public int getHierarchyDepthOfVertex(int id) {
		return isValidVertexId(id) ? hierarchyDepthPerVertex[id] : -1;
	}

	/**
	 * Returns a snapshot of the outgoing edges of the vertex. The size of the array equals the current
	 * number of outgoing edges.
	 * 
	 * @param id
	 *            The vertex' identifier.
	 * @return All outgoing edges of the vertex. (Snapshot)
	 */
	public int[] getOutgoingEdgesOfVertex(int id) {
		final int[] edges;
		if (isValidVertexId(id)) {
			edges = new int[numOfOutgoingEdgesPerSource[id]];
			System.arraycopy(outgoingEdgesPerSource[id], 0, edges, 0, edges.length);

		} else {
			edges = new int[0];
		}

		return edges;
	}

	/**
	 * Returns a snapshot of the ingoing edges of the vertex. The size of the array equals the current
	 * number of ingoing edges.
	 * 
	 * @param id
	 *            The vertex' identifier.
	 * @return All ingoing edges of the vertex. (Snapshot)
	 */
	public int[] getIngoingEdgesOfVertex(int id) {
		final int[] edges;
		if (isValidVertexId(id)) {
			edges = new int[numOfIngoingEdgesPerTarget[id]];
			System.arraycopy(ingoingEdgesPerTarget[id], 0, edges, 0, edges.length);

		} else {
			edges = new int[0];
		}

		return edges;
	}

	/**
	 * Checks, if the given identifier is a valid vertex identifier.
	 * 
	 * @param id
	 *            The identifier, which has to be checked.
	 * @return {@code true}, if the identifier is valid, otherwise {@code false}.
	 */
	private boolean isValidVertexId(int id) {
		return id >= 0 && id < numVertices;
	}

	/**
	 * Returns the other vertex, related to the edge, which is different to the given vertex. This might
	 * be the source or the target of the edge, or only the other endpoint of the edge for undirected
	 * edges.
	 * 
	 * @param edgeId
	 *            The edge, for which the other endpoint is requested.
	 * @param vertexId
	 *            The related vertex' identifier.
	 * @return The other vertex, related to the edge.
	 */
	public int getOtherVertexOfEdge(final int edgeId, final int vertexId) {
		final int other;

		if (isValidEdgeId(edgeId)) {
			other = sourcePerEdge[edgeId] != vertexId ? sourcePerEdge[edgeId] : targetPerEdge[edgeId];

		} else {
			other = -1;
		}

		return other;
	}

	/**
	 * Returns the weight of the edge.
	 * 
	 * @param id
	 *            The edge's identifier.
	 * @return The weight of the edge, if the edge's identifier is valid, otherwise {@code -1}.
	 */
	public int getWeightOfEdge(int id) {
		return isValidEdgeId(id) ? weightPerEdge[id] : -1;
	}

	/**
	 * Returns the original edge count of the edge.
	 * 
	 * @param id
	 *            The edge's identifier.
	 * @return The original edge count of the edge, if the edge's identifier is valid, otherwise
	 *         {@code -1}.
	 */
	public int getOriginalEdgeCountOfEdge(int id) {
		return isValidEdgeId(id) ? originalEdgeCountPerEdge[id] : -1;
	}

	/**
	 * Checks, if the given identifier is a valid edge identifier.
	 * 
	 * @param id
	 *            The identifier, which has to be checked.
	 * @return {@code true}, if the identifier is valid, otherwise {@code false}.
	 */
	private boolean isValidEdgeId(int id) {
		return id >= 0 && id < numEdges;
	}

	/**
	 * Returns the minimal weight of all edges between the source and target or
	 * {@link Integer#MAX_VALUE}, if there is no edge between them.<br/>
	 * This method was created for testing purposes only and is <strong>not</strong> speed optimized.
	 * 
	 * @param sourceId
	 *            The identifier of the source vertex.
	 * @param targetId
	 *            The identifier of the source target.
	 * @return The minimal weight of all edges between the source and target or
	 *         {@link Integer#MAX_VALUE}, if there is no edge between them.
	 */
	protected int getMinEdgeWeight(final int sourceId, final int targetId) {
		final int[] outgoingEdges = outgoingEdgesPerSource[sourceId];
		final int numOutgoingEdges = numOfOutgoingEdgesPerSource[sourceId];

		int outgoingEdgeId, tmpWeight, weight = 0, edgeId = -1;
		for (int i = 0; i < numOutgoingEdges; i++) {
			outgoingEdgeId = outgoingEdges[i];

			if (getOtherVertexOfEdge(outgoingEdgeId, sourceId) == targetId) {
				tmpWeight = weightPerEdge[outgoingEdgeId];

				if (edgeId == -1) {
					edgeId = outgoingEdgeId;
					weight = tmpWeight;
				} else if (tmpWeight < weight) {
					edgeId = outgoingEdgeId;
					weight = tmpWeight;
				}
			}
		}

		return edgeId != -1 ? weight : Integer.MAX_VALUE;
	}

	/**
	 * Returns the identifiers of all vertices.
	 * 
	 * @return The identifiers of all vertices.
	 */
	public int[] getVertexIds() {
		final int[] ids = new int[numVertices];
		for (int id = 0; id < numVertices; id++) {
			ids[id] = id;
		}

		return ids;
	}

	/**
	 * Applies all graph related values to the {@link Statistics global statistics collector}.
	 */
	public void applyStatistics() {
		if (!Statistics.getInstance().isEnabled()) {
			return;
		}
		Statistics.Preprocessing statistics = Statistics.getInstance().preprocessing;

		statistics.numVertices = numVertices;
		statistics.numEdges = numEdges;
		statistics.numNormalEdges = numEdges - numShortcuts;
		statistics.numShortcutEdges = numShortcuts;

		// avg|max level
		// level should be one-based, not zero-based
		float sum = 0;
		int max = Integer.MIN_VALUE;
		for (final int level : layerPerVertex) {
			sum += level + 1;

			if (level > max) {
				max = level;
			}
		}
		statistics.avgLevel = sum / numVertices;
		statistics.maxLevel = max + 1;

		// avg|max hierarchyDepth
		sum = 0;
		max = Integer.MIN_VALUE;
		for (final int hierarchyDepth : hierarchyDepthPerVertex) {
			sum += hierarchyDepth;

			if (hierarchyDepth > max) {
				max = hierarchyDepth;
			}
		}
		statistics.avgHierarchyDepth = sum / numVertices;
		statistics.maxHierarchyDepth = max;

		// min|avg|max originalEdgeCount of shortcuts
		sum = 0;
		max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		for (int i = numEdges - numShortcuts; i < numEdges; i++) {
			final int originalEdgeCount = originalEdgeCountPerEdge[i];
			sum += originalEdgeCount;

			if (originalEdgeCount < min) {
				min = originalEdgeCount;
			}
			if (originalEdgeCount > max) {
				max = originalEdgeCount;
			}
		}
		statistics.minOriginalEdgeCountOfShortcuts = min;
		statistics.avgOriginalEdgeCountOfShortcuts = sum / numShortcuts;
		statistics.maxOriginalEdgeCountOfShortcuts = max;
	}
}