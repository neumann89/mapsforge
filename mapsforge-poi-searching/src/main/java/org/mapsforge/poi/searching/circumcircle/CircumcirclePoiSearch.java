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
package org.mapsforge.poi.searching.circumcircle;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.poi.searching.ExtendedPoi;
import org.mapsforge.poi.searching.PoiSearch;
import org.mapsforge.poi.searching.util.PoiOperator;
import org.mapsforge.poi.searching.util.RouteOperator;
import org.mapsforge.routing.Edge;
import org.mapsforge.routing.Router;
import org.mapsforge.routing.Vertex;
import org.mapsforge.storage.poi.PoiCategoryFilter;
import org.mapsforge.storage.poi.PoiPersistenceManager;
import org.mapsforge.storage.poi.PointOfInterest;

/**
 * Implements a bounding box search at every route's vertex.
 */
public class CircumcirclePoiSearch implements PoiSearch {
	private PoiPersistenceManager poiManager;
	private PoiCategoryFilter poiCategoryFilter;
	private Edge[] routeAsEdges;
	private Router router;

	public CircumcirclePoiSearch(Router router, PoiPersistenceManager poiManager) {
		this.poiManager = poiManager;
		this.poiCategoryFilter = null;
		this.router = router;
	}

	@Override
	public void setRoute(Edge[] route) {
		this.routeAsEdges = route;

	}

	@Override
	public void setPoiFilter(PoiCategoryFilter acceptedCategories) {
		this.poiCategoryFilter = acceptedCategories;
	}

	@Override
	public Collection<ExtendedPoi> searchNearRoute(int maxDistance,
			int poiNumberLimit) {
		List<ExtendedPoi> poiList = new LinkedList<ExtendedPoi>();

		// search at every source of an edge
		for (Edge e : routeAsEdges) {
			Vertex v = e.getSource();
			Collection<ExtendedPoi> vertexResults = searchAtVertex(v,
					maxDistance, poiNumberLimit);
			PoiOperator.integrateNewPoiInList(vertexResults, poiList,
					poiNumberLimit);
		}

		// same for last vertex of route
		Edge e = routeAsEdges[routeAsEdges.length - 1];
		Vertex v = e.getTarget();
		Collection<ExtendedPoi> vertexResults = searchAtVertex(v, maxDistance,
				poiNumberLimit);
		PoiOperator.integrateNewPoiInList(vertexResults, poiList,
				poiNumberLimit);

		return poiList;
	}

	@Override
	public Edge[] recalculateRoute(Collection<ExtendedPoi> choosenPoi) {
		List<Edge> routeAsList = RouteOperator
				.convertEdgeArrayToList(routeAsEdges);

		for (ExtendedPoi poi : choosenPoi) {
			org.mapsforge.routing.GeoCoordinate c = new org.mapsforge.routing.GeoCoordinate(
					poi.getPoi().getLatitude(), poi.getPoi().getLongitude());
			Vertex poiVertex = router.getNearestVertex(c);
			poi.setNearestVertex(poiVertex);

			Vertex nearestRouteVertex = poi.getNearestRouteVertex();

			if (poiVertex != null && nearestRouteVertex != null) {
				// detour paths
				List<Edge> detourToPoi = getPath(nearestRouteVertex, poiVertex);
				List<Edge> detourFromPoi = getPath(poiVertex,
						nearestRouteVertex);

				int index = RouteOperator.getIndexOfVertex(routeAsList,
						nearestRouteVertex);
				if (index >= 0) {
					routeAsList.addAll(index, detourFromPoi);
					routeAsList.addAll(index, detourToPoi);
				}
			}
		}

		this.routeAsEdges = RouteOperator.convertEdgeListToArray(routeAsList);
		return routeAsEdges;
	}

	private Collection<ExtendedPoi> searchAtVertex(Vertex v, int maxDistance,
			int poiNumberLimit) {
		Collection<ExtendedPoi> vertexSearchResults = new LinkedList<ExtendedPoi>();

		// FIXME duplicate implementations of GeoCoordinate
		org.mapsforge.routing.GeoCoordinate vCoordTemp = v.getCoordinate();
		GeoCoordinate vCoord = new GeoCoordinate(vCoordTemp.getLatitude(),
				vCoordTemp.getLongitude());

		Collection<PointOfInterest> results = null;
		if (poiCategoryFilter != null) {
			results = poiManager.findNearPositionWithFilter(vCoord,
					maxDistance, poiCategoryFilter, poiNumberLimit);
		} else {
			results = poiManager.findNearPosition(vCoord, maxDistance,
					poiNumberLimit);
		}

		if (results != null) {
			for (PointOfInterest poi : results) {
				double distance = vCoord.sphericalDistance(poi
						.getGeoCoordinate());

				// org.mapsforge.routing.GeoCoordinate poiCoord = new
				// org.mapsforge.routing.GeoCoordinate(poi.getLatitude(),
				// poi.getLongitude());
				// Vertex nearestVertex = router.getNearestVertex(poiCoord);
				// vertexSearchResults.add(new ExtendedPoi(poi, distance,
				// nearestVertex, v));

				vertexSearchResults
						.add(new ExtendedPoi(poi, distance, null, v));
			}
		}
		return vertexSearchResults;
	}

	private List<Edge> getPath(Vertex startVertex, Vertex targetVertex) {
		Edge[] edges = router.getShortestPath(startVertex.getId(),
				targetVertex.getId());
		return RouteOperator.convertEdgeArrayToList(edges);
	}

}
