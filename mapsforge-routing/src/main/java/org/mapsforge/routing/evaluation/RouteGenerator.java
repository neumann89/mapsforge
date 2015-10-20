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
package org.mapsforge.routing.evaluation;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.graph.RgDAO;
import org.mapsforge.routing.graph.RgEdge;
import org.mapsforge.routing.graph.RgVertex;
import org.mapsforge.routing.preprocessing.sql.DBConnection;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * General generator for random test routes of a specified Dijkstra rank, which can be used for
 * comparing different router implementations (different routing algorithms).
 * 
 * @author Patrick Jungermann
 * @version $Id: RouteGenerator.java 2092 2012-08-06 00:05:51Z Patrick.Jungermann@googlemail.com $
 */
public class RouteGenerator {

	/**
	 * Each vertex' latitude values.
	 */
	private final double[] latitudePerVertex;
	/**
	 * Each vertex' longitude values.
	 */
	private final double[] longitudePerVertex;
	/**
	 * Each vertex' outgoing edges.
	 */
	private final TIntArrayList[] outgoingEdgesPerVertex;
	/**
	 * Each edge's target vertex.
	 */
	private final int[] targetPerEdge;
	/**
	 * Each edge's weight.
	 */
	private final int[] weightPerEdge;

	/**
	 * Creates a {@link RouteGenerator route generator} for the general routing graph, which data is
	 * accessible via the {@link Connection database connection}.
	 * 
	 * @param connection
	 *            The database connection, which provides access to the database, which contains the
	 *            routing graph's data.
	 * @throws SQLException
	 *             if there was a problem with reading the routing graph's data from the database.
	 */
	public RouteGenerator(final Connection connection) throws SQLException {
		this(new RgDAO(connection));
	}

	/**
	 * Creates a {@link RouteGenerator route generator} for the given general routing graph.
	 * 
	 * @param dao
	 *            The data access object, which provides access to the general routing graph.
	 */
	public RouteGenerator(final RgDAO dao) {

		// get all vertices
		int numVertices = dao.getNumVertices();
		latitudePerVertex = new double[numVertices];
		longitudePerVertex = new double[numVertices];
		outgoingEdgesPerVertex = new TIntArrayList[dao.getNumVertices()];

		for (RgVertex vertex : dao.getVertices()) {
			final int vertexId = vertex.getId();

			latitudePerVertex[vertexId] = vertex.getLatitude();
			longitudePerVertex[vertexId] = vertex.getLongitude();
			outgoingEdgesPerVertex[vertexId] = new TIntArrayList();
		}

		// get all edges
		final int numEdges = dao.getNumEdges();
		targetPerEdge = new int[numEdges];
		weightPerEdge = new int[numEdges];
		for (final RgEdge edge : dao.getEdges()) {
			final int edgeId = edge.getId();

			targetPerEdge[edgeId] = edge.getTargetId();
			weightPerEdge[edgeId] = edge.getWeight();
			outgoingEdgesPerVertex[edge.getSourceId()].add(edgeId);
		}
	}

	/**
	 * Generates the number of routes of the specified Dijkstra rank for the general routing graph, from
	 * which the data is accessible via the {@link Connection database connection}.
	 * 
	 * @param connection
	 *            The database connection.
	 * @param numRoutes
	 *            The number of routes, which have to be generated.
	 * @param dijkstraRank
	 *            The Dijkstra rank of all generated routes.
	 * @return The generated routes.
	 * @throws SQLException
	 *             if there was a problem with reading the routing graph's data from the database.
	 */
	public static Route[] generate(final Connection connection, final int numRoutes,
			final int dijkstraRank)
			throws SQLException {
		return new RouteGenerator(connection).generate(numRoutes, dijkstraRank);
	}

	/**
	 * Generates the number of routes of the specified Dijkstra rank.
	 * 
	 * @param numRoutes
	 *            The number of routes, which have to be generated.
	 * @param dijkstraRank
	 *            The Dijkstra rank of all generated routes.
	 * @return The generated routes.
	 */
	public Route[] generate(final int numRoutes, final int dijkstraRank) {
		Route[] routes = new Route[numRoutes];

		TIntArrayList startVertices = new TIntArrayList(numRoutes);
		Random rnd = new Random();

		// create all routes
		for (int j = 0; j < numRoutes; j++) {
			// choose the (random) source
			int sourceId = -1, targetId = -1;
			while (targetId == -1) {
				while (sourceId == -1 || startVertices.contains(sourceId)) {
					if (startVertices.size() == outgoingEdgesPerVertex.length) {
						throw new RuntimeException("not enough routes of the specified dijkstra rank");
					}

					sourceId = rnd.nextInt(outgoingEdgesPerVertex.length);
				}
				startVertices.add(sourceId);

				// find the target of the given rank
				targetId = findTarget(sourceId, dijkstraRank);
			}

			routes[j] = new Route(
					new GeoCoordinate(latitudePerVertex[sourceId], longitudePerVertex[sourceId]),
					new GeoCoordinate(latitudePerVertex[targetId], longitudePerVertex[targetId]),
					dijkstraRank
					);
		}

		return routes;
	}

	/**
	 * Searchs for a route of the specified Dijkstra rank, starting at the given source vertex.
	 * 
	 * @param sourceId
	 *            The source vertex' identifier.
	 * @param dijkstraRank
	 *            The Dijkstra rank of the related route.
	 * @return The target vertex' identifier.
	 */
	private int findTarget(final int sourceId, final int dijkstraRank) {
		PriorityQueue<QueueItem> queue = new PriorityQueue<QueueItem>();
		TIntObjectHashMap<QueueItem> discovered = new TIntObjectHashMap<QueueItem>(dijkstraRank * 2);

		// start
		int numSettled = 0;
		QueueItem sourceItem = new QueueItem(0, sourceId);
		queue.add(sourceItem);
		discovered.put(sourceItem.vertexId, sourceItem);

		while (!queue.isEmpty()) {
			final QueueItem item = queue.poll();
			numSettled++;

			if (numSettled == dijkstraRank) {
				return item.vertexId;
			}

			for (final int edgeId : outgoingEdgesPerVertex[item.vertexId].toArray()) {
				final int targetId = targetPerEdge[edgeId];
				final int weight = weightPerEdge[edgeId];

				// not discovered OR shorter distance
				final boolean isDiscovered = discovered.containsKey(targetId);
				if (!isDiscovered || discovered.get(targetId).distance > item.distance + weight) {
					if (isDiscovered) {
						queue.remove(discovered.get(targetId));
					}

					final QueueItem newItem = new QueueItem(item.distance + weight, targetId);
					queue.add(newItem);
					discovered.put(targetId, newItem);
				}
			}
		}

		return -1;
	}

	/**
	 * Item, used for the priority queue at the Dijkstra search.
	 * 
	 * @author Patrick Jungermann
	 * @version $Id: RouteGenerator.java 2092 2012-08-06 00:05:51Z Patrick.Jungermann@googlemail.com $
	 */
	private static class QueueItem implements Comparable<QueueItem> {
		final int distance;
		final int vertexId;

		public QueueItem(int distance, int vertexId) {
			this.distance = distance;
			this.vertexId = vertexId;
		}

		@Override
		public int compareTo(final QueueItem o) {
			if (o == null) {
				return -1;
			}

			return distance < o.distance ? -1 : (distance == o.distance ? 0 : 1);
		}
	}

	/**
	 * Test code, expecting the hard-coded database connection configuration.
	 * 
	 * @param args
	 *            Not used.
	 * @throws SQLException
	 *             if there was a problem with reading the required data from the database.
	 */
	public static void main(String[] args) throws SQLException {
		final Connection connection = DBConnection.getConnectionToPostgreSQL(
                "localhost", 5432, "osm", "osm", "osm");

		final Route[] routes = generate(connection, 1000, (int) Math.pow(2, 10));
		try {
			final File file = new File("data/evaluation/berlin.routes");
			System.out.println("File: " + file.getCanonicalPath());

			if (file.exists()) {
				// deserialize all routes
				final FileInputStream fileInputStream = new FileInputStream(file);
				final ObjectInputStream in = new ObjectInputStream(fileInputStream);
				final Route[] storedRoutes = (Route[]) in.readObject();

				System.out.println("last routes:");
				for (final Route route : storedRoutes) {
					System.out.println(route.source.toString() + "\tto\t" + route.target.toString());
				}
				System.out.println();
			}

			// serialize all routes
			final FileOutputStream fileOutputStream = new FileOutputStream(file);
			final ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
			out.writeObject(routes);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("new routes:");
		for (final Route route : routes) {
			System.out.println(route.source.toString() + "\tto\t" + route.target.toString());
		}
	}
}
