package org.mapsforge.poi.searching.polygon;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.model.GeoPoint;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Searches for poi in a polygon area
 * around the route. The edges of this 
 * polygon are the maximum allowed distance
 * apart from the route.
 */
public class PolygonPoiSearch implements PoiSearch {
	private PoiPersistenceManager poiManager;
	private PoiCategoryFilter poiCategoryFilter;
	private Edge[] route;
	private Router router;
	private List<Vertex> routeAsVertexList;
	private CoordinateConverter converter;
	private List<GeoPoint> polygonPoints = new LinkedList<GeoPoint>();
	
	public PolygonPoiSearch(Router router, PoiPersistenceManager poiManager){
		this.poiManager = poiManager;
		this.poiCategoryFilter = null;
		this.router = router;
	}

	@Override
	public void setRoute(Edge[] route) {
		this.route = route;
		this.routeAsVertexList = RouteOperator.convertEdgesToVertices(route);
	}

	@Override
	public void setPoiFilter(PoiCategoryFilter acceptedCategories) {
		this.poiCategoryFilter = acceptedCategories;
	}
	
	@Override
	public Collection<ExtendedPoi> searchNearRoute(int maxDistance,
			int poiNumberLimit) {
		// find a roughly set of poi
		List<ExtendedPoi> poiList = new LinkedList<ExtendedPoi>();
		for(Edge e : this.route){
			Vertex v = e.getSource();
			// To have no holes between 2 bounding boxes use distance between 2 vertices,
			// if it is greater than maxDistance.
			org.mapsforge.routing.GeoCoordinate source = v.getCoordinate();
			org.mapsforge.routing.GeoCoordinate target = e.getTarget().getCoordinate();
			int distance = (int)Math.ceil(source.sphericalDistance(target));
			if(distance < maxDistance){
				distance = maxDistance;
			}

			Collection<ExtendedPoi> searchResults = searchAtVertex(v, distance, poiNumberLimit);
			PoiOperator.integrateNewPoiInList(searchResults, poiList, poiNumberLimit);
		}
		// search at last vertex with max distance
		Edge e = this.route[this.route.length-1];
		Vertex v = e.getTarget();
		Collection<ExtendedPoi> searchResults = searchAtVertex(v, maxDistance, poiNumberLimit);
		PoiOperator.integrateNewPoiInList(searchResults, poiList, poiNumberLimit);
		
		// create polygon around route
		Polygon polygon = createRouteBuffer(maxDistance);
		
		// convert polygon to geo points list
		Coordinate[] polygonCoordinates = polygon.getCoordinates();
		for(Coordinate c : polygonCoordinates){
			polygonPoints.add(new GeoPoint(c.x, c.y));
		}
		
		// filter out poi from the roughly set which are not in the polygon area
		validatePoi(poiList, polygon);
		
		return poiList;
	}

	@Override
	public Edge[] recalculateRoute(Collection<ExtendedPoi> choosenPoi) {
		List<Edge> routeAsList = RouteOperator.convertEdgeArrayToList(route);
		
		for(ExtendedPoi poi : choosenPoi){
			org.mapsforge.routing.GeoCoordinate c = new org.mapsforge.routing.GeoCoordinate(poi.getPoi().getLatitude(), poi.getPoi().getLongitude());
			Vertex poiVertex = router.getNearestVertex(c);
			poi.setNearestVertex(poiVertex);

			Vertex nearestRouteVertex = poi.getNearestRouteVertex();
			
			if(poiVertex != null && nearestRouteVertex != null){
				// detour paths
				List<Edge> detourToPoi = getPath(nearestRouteVertex, poiVertex);
				List<Edge> detourFromPoi = getPath(poiVertex, nearestRouteVertex);
				
				int index = RouteOperator.getIndexOfVertex(routeAsList, nearestRouteVertex);
				if(index >= 0){
					routeAsList.addAll(index, detourFromPoi);
					routeAsList.addAll(index, detourToPoi);
				}
			}
		}
		
		this.route = RouteOperator.convertEdgeListToArray(routeAsList);
		return route;
	}
	
	public List<GeoPoint> getPolygon() {
		return this.polygonPoints;
	}
	
	private Collection<ExtendedPoi> searchAtVertex(Vertex v, int maxDistance, int poiNumberLimit){
		Collection<ExtendedPoi> vertexSearchResults = new LinkedList<ExtendedPoi>();
		
		// FIXME duplicate implementations of GeoCoordinate
		org.mapsforge.routing.GeoCoordinate vCoordTemp = v.getCoordinate();
		GeoCoordinate vCoord = new GeoCoordinate(vCoordTemp.getLatitude(), vCoordTemp.getLongitude());
		
		Collection<PointOfInterest> results = null;
		if(poiCategoryFilter != null){
			results = poiManager.findNearPositionWithFilter(vCoord, maxDistance, poiCategoryFilter, poiNumberLimit);
		} else {
			results = poiManager.findNearPosition(vCoord, maxDistance, poiNumberLimit);
		}
		
		if(results != null){
			for(PointOfInterest poi : results){
				double distance = vCoord.sphericalDistance(poi.getGeoCoordinate());
				vertexSearchResults.add(new ExtendedPoi(poi, distance, null, v));
			}
		}
		return vertexSearchResults;
	}
	
	private Polygon createRouteBuffer(int maxDistance) {
		// convert route to JTS coordinates
		Coordinate[] coordArray = new Coordinate[routeAsVertexList.size()];
		for(int i=0; i<routeAsVertexList.size(); ++i){
			Vertex v = routeAsVertexList.get(i);
			coordArray[i] = new Coordinate(v.getCoordinate().getLatitude(), v.getCoordinate().getLongitude());
		}
		
		// convert lon/lat to cartesian
		Coordinate first = coordArray[0]; //avoid Index Out Of Bounds
		Coordinate last = coordArray[coordArray.length-1];
		this.converter = new CoordinateConverter(first, last);
		
		Coordinate[] coordCartesianArray = this.converter.geoToCartesian(coordArray);
		
		// route as geometry line string
		GeometryFactory geometryFactory = new GeometryFactory();
		Geometry geometry = geometryFactory.createLineString(coordCartesianArray);
		
		// create buffer around route
		Geometry buffer = geometry.buffer(GeoCoordinate.latitudeDistance(maxDistance));
		Coordinate[] bufferCoordArrayCartesian = buffer.getCoordinates();
		
		// convert buffer coordinates to geo coordinates
		Coordinate[] bufferCoordArray = this.converter.cartesianToGeo(bufferCoordArrayCartesian);
		
		// buffer as closed line string (ring)
		LinearRing ring = geometryFactory.createLinearRing(bufferCoordArray);
		Polygon polygon = geometryFactory.createPolygon(ring, null);
		
		return polygon;
	}
	
	// removes all poi, which are not in the polygon area
	private void validatePoi(List<ExtendedPoi> poiList, Polygon polygon){		
		GeometryFactory geometryFactory = new GeometryFactory();
		List<ExtendedPoi> removeList = new LinkedList<ExtendedPoi>();
		for(ExtendedPoi p : poiList){
			Coordinate coordinate = new Coordinate(p.getPoi().getLatitude(), p.getPoi().getLongitude());
			Point point = geometryFactory.createPoint(coordinate);

			if(!polygon.contains(point)){
				removeList.add(p);
			} 
		}
		for(ExtendedPoi p : removeList){
			poiList.remove(p);
		}
	}
	
	// generates a shortest path between two vertices
	private List<Edge> getPath(Vertex startVertex, Vertex targetVertex){
		Edge[] edges = router.getShortestPath(startVertex.getId(), targetVertex.getId());
		return RouteOperator.convertEdgeArrayToList(edges);
	}
	
}
