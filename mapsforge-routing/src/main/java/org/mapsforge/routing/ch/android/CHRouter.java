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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.mapsforge.routing.Edge;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.Rect;
import org.mapsforge.routing.Router;
import org.mapsforge.routing.Vertex;
import org.mapsforge.routing.hh.preprocessing.hierarchyComputation.util.renderer.RouteViewer;

/**
 * Router based on the Contraction Hierarchies algorithm.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHRouter.java 1746 2012-01-16 22:38:34Z Patrick.Jungermann@googlemail.com $
 */
public class CHRouter implements Router {

	/**
	 * Name of the underlying routing algorithm.
	 */
	private static final String ALGORITHM_NAME = "Mobile Contraction Hierarchies";
	/**
	 * The max. search radius, used for the searches inside of the R-tree.
	 */
	private static final int MAX_RTREE_SEARCH_RADIUS = 2000;

	/**
	 * Underlying routing graph.
	 */
	private final CHGraph graph;

	/**
	 * The used routing algorithm.
	 */
	private final CHAlgorithm algorithm;

	/**
	 * Creates a router based on the Contraction Hierarchies algorithm for the given binary routing data
	 * file.
	 * 
	 * @param mchFile
	 *            The binary routing data file, related to the Contraction Hierarchies algorithm.
	 * @param cacheSizeInBytes
	 *            The size of the underlying cache in bytes.
	 * @throws IOException
	 *             if there was any problem, while creating the routing graph for the given binary file.
	 */
	public CHRouter(final File mchFile, final int cacheSizeInBytes) throws IOException {
		graph = new CHGraph(mchFile, cacheSizeInBytes);
		algorithm = new CHAlgorithm(graph);
	}

	@Override
	public Edge[] getShortestPath(int sourceId, int targetId) {
		try {
			final LinkedList<CHEdge> shortestPath = algorithm.getShortestPath(sourceId, targetId);

			final EdgeImpl[] edges = new EdgeImpl[shortestPath.size()];
			int i = 0;
			for (final CHEdge edge : shortestPath) {
				edges[i++] = new EdgeImpl(edge);
			}

			return edges;

		} catch (IOException e) {
			return new EdgeImpl[0];
		}
	}

	@Override
	public Edge[] getShortestPathDebug(final int sourceId, final int targetId,
			final Collection<Edge> searchSpaceBuffer) {
		return getShortestPath(sourceId, targetId);
	}

	@Override
	public Vertex getNearestVertex(final GeoCoordinate coordinate) {
		CHVertex vertex = null;
		try {
			vertex = graph.getNearestVertex(coordinate, MAX_RTREE_SEARCH_RADIUS);

		} catch (IOException e) {
			// nothing to do here
		}

		return vertex != null ? new VertexImpl(vertex) : null;
	}

	@Override
	public Vertex getVertex(final int id) {
		CHVertex vertex = null;
		try {
			vertex = graph.getVertex(id);

		} catch (IOException e) {
			// nothing to do here
		}

		return vertex != null ? new VertexImpl(vertex) : null;
	}

	@Override
	public Iterator<? extends Vertex> getVerticesWithinBox(final Rect boundingBox) {
		try {
			final LinkedList<CHVertex> vertices = graph.getVerticesWithinBoundingBox(boundingBox);
			return new Iterator<Vertex>() {

				@Override
				public boolean hasNext() {
					return vertices.size() > 0;
				}

				@Override
				public Vertex next() {
					return new VertexImpl(vertices.removeFirst());
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};

		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public Edge getNearestEdge(final GeoCoordinate coordinate) {
		// TODO: implement this
		throw new UnsupportedOperationException("Not implemented, yet.");
	}

	@Override
	public Edge[] getNearestEdges(final GeoCoordinate coordinate) {
		// TODO: implement this
		throw new UnsupportedOperationException("Not implemented, yet.");
	}

	@Override
	public String getAlgorithmName() {
		return ALGORITHM_NAME;
	}

	@Override
	public Rect getBoundingBox() {
		return graph.getBoundingBox();
	}

	/**
	 * Closes the access to the routing graph. No routing will be possible anymore after that.
	 * 
	 * @throws IOException
	 *             if there was a problem with closing the access.
	 */
	public void close() throws IOException {
		graph.close();
	}

	/**
	 * A Sample.
	 * 
	 * @param args
	 *            not used.
	 * @throws IOException
	 *             if there is something wrong reading the file.
	 */
	public static void main(String[] args) throws IOException {
		// this code is required for android routing:
		final int cacheSize = 1024 * 1024 * 2;
		final CHRouter router = new CHRouter(new File("data/binary/berlin.mch"), cacheSize);

		final Vertex source = router.getNearestVertex(new GeoCoordinate(52.60818, 13.48487));
		final Vertex target = router.getNearestVertex(new GeoCoordinate(52.4556941, 13.2918805));
		final Edge[] shortestPath = router.getShortestPath(source.getId(), target.getId());

		// print the route
		System.out.println(String.format(
				"source -> target: %d -> %d\nroute: %d steps",
				source.getId(), target.getId(), shortestPath.length
				));
		for (final Edge e : shortestPath) {
			System.out.println(String.format(
					"%d -> %d %s %s %s",
					e.getSource().getId(),
					e.getTarget().getId(),
					e.getName(), e.getRef(), e.getType()
					));
		}

		// TODO: move the renderer to an own package, so that it is more usable for other algorithms
		// than HH
		// this is just for viewing the route - testing code only
		// final RendererV2 renderer = new RendererV2(800, 600, router, Color.BLACK, Color.WHITE);
		// renderer.setRenderParam(new GeoCoordinate(52.50818, 13.28487), 4);
		// renderer.addRoute(shortestPath, Color.RED);

		RouteViewer viewer = new RouteViewer(router, 52.4, 13.4);
		viewer.drawEdges(shortestPath, Color.RED, 2);
	}

	/**
	 * Implementation of the global vertex specification.
	 */
	private class VertexImpl implements Vertex {

		/**
		 * The graph's vertex.
		 */
		protected final CHVertex vertex;

		/**
		 * Creates a vertex, which is conforming to the global vertex specifications.
		 * 
		 * @param vertex
		 *            The graph's vertex.
		 */
		public VertexImpl(final CHVertex vertex) {
			this.vertex = vertex;
		}

		@Override
		public int getId() {
			return vertex.id;
		}

		/**
		 * Returns the vertex' <strong>all</strong> known edges (outbound and inbound). Each of the
		 * underlying graph's vertices doesn't know all of its outbound nor inbound edges.<br/>
		 * For rendering purposes, this method will return all of its known edges. It is not
		 * recommended, to use this for other purposes, too.
		 * 
		 * @return The vertex' all known edges (outbound and inbound).
		 */
		@Override
		public Edge[] getOutboundEdges() {
			Edge[] edges;
			try {
				CHEdge[] outgoing = graph.getOutgoingEdgesToHigherVertices(vertex.id);
				CHEdge[] ingoing = graph.getIngoingEdgesFromHigherVertices(vertex.id);
				Edge[] tmp = new Edge[outgoing.length + ingoing.length];

				int numEdges = 0;
				for (final CHEdge chEdge : outgoing) {
					if (!chEdge.shortcut) {
						tmp[numEdges++] = new EdgeImpl(chEdge);

					} else {
						graph.release(chEdge);
					}
				}
				for (final CHEdge chEdge : ingoing) {
					if (!chEdge.shortcut) {
						tmp[numEdges++] = new EdgeImpl(chEdge);

					} else {
						graph.release(chEdge);
					}
				}

				edges = new Edge[numEdges];
				System.arraycopy(tmp, 0, edges, 0, edges.length);

			} catch (IOException e) {
				edges = new Edge[0];
			}

			// TODO: use the exception, not all outbound edges are known here..
			// throw new
			// UnsupportedOperationException("The returning of outbound edges for any vertex is not supported by "
			// + getAlgorithmName() + ".");

			return edges;
		}

		@Override
		public GeoCoordinate getCoordinate() {
			return new GeoCoordinate(vertex.latitudeE6, vertex.longitudeE6);
		}
	}

	/**
	 * Implementation of the global edge specification.
	 */
	private class EdgeImpl implements Edge {

		/**
		 * The graph's edge.
		 */
		protected CHEdge edge;

		/**
		 * Creates an edge, which is conforming to the global edge specification.
		 * 
		 * @param edge
		 *            The graph's edge.
		 */
		public EdgeImpl(final CHEdge edge) {
			this.edge = edge;
		}

		@Override
		public int getId() {
			return -1;
		}

		@Override
		public Vertex getSource() {
			return getVertex(edge.getSourceId());
		}

		@Override
		public Vertex getTarget() {
			return getVertex(edge.getTargetId());
		}

		@Override
		public GeoCoordinate[] getWaypoints() {
			final int[] intWaypoints = edge.getWaypoints();
			final GeoCoordinate[] waypoints = new GeoCoordinate[intWaypoints.length / 2];

			for (int i = 0; i < waypoints.length; i++) {
				waypoints[i] = new GeoCoordinate(intWaypoints[i * 2], intWaypoints[i * 2 + 1]);
			}

			return waypoints;
		}

		@Override
		public GeoCoordinate[] getAllWaypoints() {
			final GeoCoordinate[] waypoints = getWaypoints();

			final GeoCoordinate sourceCoordinate = getSource().getCoordinate();
			final GeoCoordinate targetCoordinate = getTarget().getCoordinate();

			final GeoCoordinate[] allWaypoints = new GeoCoordinate[waypoints.length + 2];
			allWaypoints[0] = sourceCoordinate;
			System.arraycopy(waypoints, 0, allWaypoints, 1, waypoints.length);
			allWaypoints[allWaypoints.length - 1] = targetCoordinate;

			return allWaypoints;
		}

		@Override
		public String getName() {
			try {
				return edge.name != null ? new String(edge.name, "UTF-8") : null;

			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}

		@Override
		public String getType() {
			return (edge.streetTypeId >= 0 && edge.streetTypeId < graph.streetTypes.length) ? graph.streetTypes[edge.streetTypeId]
					: null;
		}

		@Override
		public boolean isRoundabout() {
			return edge.roundabout;
		}

		@Override
		public String getRef() {
			try {
				return edge.ref != null ? new String(edge.ref, "UTF-8") : null;

			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}

		@Override
		public String getDestination() {
			return null;
		}

		@Override
		public int getWeight() {
			return edge.weight;
		}
	}
}
