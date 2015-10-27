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
package org.mapsforge.poi.searching.util;

import java.util.LinkedList;
import java.util.List;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.poi.searching.ExtendedVertex;
import org.mapsforge.routing.Edge;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.Vertex;

/**
 * Util for conversion operations to the route.
 */
public class RouteOperator {

	public static List<GeoPoint> convertEdgesToPoints(Edge[] routeAsEdges) {
		List<GeoPoint> points = new LinkedList<GeoPoint>();

		if (routeAsEdges.length > 0) {
			for (Edge edge : routeAsEdges) {
				GeoCoordinate edgeSource = edge.getSource().getCoordinate();
				points.add(new GeoPoint(edgeSource.getLatitude(), edgeSource
						.getLongitude()));
			}
			GeoCoordinate lastEdgeTarget = routeAsEdges[routeAsEdges.length - 1]
					.getTarget().getCoordinate();
			points.add(new GeoPoint(lastEdgeTarget.getLatitude(),
					lastEdgeTarget.getLongitude()));
		}
		return points;
	}

	public static List<Vertex> convertEdgesToVertices(Edge[] routeAsEdges) {
		List<Vertex> vertices = new LinkedList<Vertex>();

		if (routeAsEdges.length > 0) {
			for (Edge edge : routeAsEdges) {
				vertices.add(edge.getSource());
			}
			Vertex lastEdgeTarget = routeAsEdges[routeAsEdges.length - 1]
					.getTarget();
			vertices.add(lastEdgeTarget);
		}
		return vertices;
	}

	public static List<ExtendedVertex> convertEdgesToExtendedVertices(
			Edge[] routeAsEdges) {
		List<ExtendedVertex> vertices = new LinkedList<ExtendedVertex>();

		if (routeAsEdges.length > 0) {
			for (Edge edge : routeAsEdges) {
				vertices.add(new ExtendedVertex(edge.getSource(), 0, null, null));
			}
			Vertex lastEdgeTarget = routeAsEdges[routeAsEdges.length - 1]
					.getTarget();
			vertices.add(new ExtendedVertex(lastEdgeTarget, 0, null, null));
		}
		return vertices;
	}

	public static List<Edge> convertEdgeArrayToList(Edge[] routeAsEdges) {
		List<Edge> edgeList = new LinkedList<Edge>();
		for (Edge e : routeAsEdges) {
			edgeList.add(e);
		}
		return edgeList;
	}

	public static Edge[] convertEdgeListToArray(List<Edge> routeAsList) {
		Edge[] route = new Edge[routeAsList.size()];
		for (int i = 0; i < route.length; ++i) {
			route[i] = routeAsList.get(i);
		}
		return route;
	}

	public static int getIndexOfVertex(List<Edge> route, Vertex v) {
		int index = -1;
		int vId = v.getId();
		for (Edge e : route) {
			if (vId == e.getSource().getId()) {
				index = route.indexOf(e);
			}
		}

		// also check last vertex
		Edge lastEdge = route.get(route.size() - 1);
		if (vId == lastEdge.getTarget().getId()) {
			index = route.indexOf(lastEdge) + 1;
		}
		return index;
	}

}
