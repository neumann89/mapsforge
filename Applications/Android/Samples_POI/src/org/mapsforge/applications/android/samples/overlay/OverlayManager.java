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
package org.mapsforge.applications.android.samples.overlay;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.Circle;
import org.mapsforge.android.maps.overlay.ListOverlay;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.PolygonalChain;
import org.mapsforge.android.maps.overlay.Polyline;
import org.mapsforge.applications.android.samples.R;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.poi.searching.ExtendedPoi;
import org.mapsforge.poi.searching.ExtendedVertex;
import org.mapsforge.poi.searching.util.RouteOperator;
import org.mapsforge.routing.Edge;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.storage.poi.PointOfInterest;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class OverlayManager {
	private MapActivity activity;
	private MapView mapView;
	private Paint bluePaint;
	private Paint redPaint;
	private Paint greenPaint;
	private Paint greenCircleBorderPaint;

	public OverlayManager(MapActivity activity, MapView mapView) {
		this.activity = activity;
		this.mapView = mapView;

		bluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		bluePaint.setARGB(128, 0, 0, 255);
		bluePaint.setStyle(Paint.Style.STROKE);
		bluePaint.setStrokeWidth(7);

		redPaint = new Paint(bluePaint);
		redPaint.setARGB(128, 255, 0, 0);

		greenPaint = new Paint(bluePaint);
		greenPaint.setARGB(128, 0, 255, 0);

		greenCircleBorderPaint = new Paint(bluePaint);
		greenCircleBorderPaint.setARGB(40, 0, 255, 0);
		greenCircleBorderPaint.setStrokeWidth(3);
	}

	private Marker createMarker(int resourceIdentifier, GeoPoint geoPoint) {
		Drawable drawable = activity.getResources().getDrawable(resourceIdentifier);
		return new Marker(geoPoint, Marker.boundCenterBottom(drawable));
	}

	public void setPoiOverlay(Collection<ExtendedPoi> poiResults) {
		Collection<OverlayItem> items = null;
		if (poiResults != null) {
			items = new LinkedList<OverlayItem>();
			for (ExtendedPoi extPoi : poiResults) {
				PointOfInterest p = extPoi.getPoi();
				OverlayItem item = createMarker(R.drawable.marker_green,
						new GeoPoint(p.getLatitude(), p.getLongitude()));
				items.add(item);
			}
		}
		ListOverlay listOverlayPoi = new ListOverlay();
		listOverlayPoi.getOverlayItems().addAll(items);

		this.mapView.getOverlays().add(listOverlayPoi);
	}

	public void setRouteCircumcircleOverlay(Edge[] routeAsEdges, int maxDistance) {
		List<GeoPoint> route = RouteOperator.convertEdgesToPoints(routeAsEdges);
		Collection<OverlayItem> circles = new LinkedList<OverlayItem>();
		if (route != null) {

			Paint greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			greenPaint.setARGB(0, 0, 255, 0);
			greenPaint.setStyle(Paint.Style.FILL);

			for (GeoPoint node : route) {
				OverlayItem circle = new Circle(node, maxDistance, greenPaint, greenCircleBorderPaint);
				circles.add(circle);
			}
		}
		ListOverlay listOverlayCircles = new ListOverlay();
		listOverlayCircles.getOverlayItems().addAll(circles);
		this.mapView.getOverlays().add(listOverlayCircles);
	}

	public void setAdjustedCircumcircleOverlay(Edge[] routeAsEdges, List<Integer> distanceList) {
		List<GeoPoint> route = RouteOperator.convertEdgesToPoints(routeAsEdges);
		Collection<OverlayItem> circles = new LinkedList<OverlayItem>();
		if (route.size() == distanceList.size()) {
			for (int i = 0; i < distanceList.size(); ++i) {
				int distance = distanceList.get(i);
				GeoPoint p = route.get(i);
				OverlayItem circle = new Circle(p, distance, greenPaint, greenCircleBorderPaint);
				circles.add(circle);
			}
			ListOverlay listOverlayCircles = new ListOverlay();
			listOverlayCircles.getOverlayItems().addAll(circles);
			this.mapView.getOverlays().add(listOverlayCircles);
		}
	}

	public void setRouteOverlay(Edge[] routeAsEdges) {
		List<GeoPoint> route = RouteOperator.convertEdgesToPoints(routeAsEdges);
		PolygonalChain polyChain = new PolygonalChain(route);

		OverlayItem overlayRouteItem = new Polyline(polyChain, bluePaint);

		ListOverlay listOverlayRoute = new ListOverlay();
		List<OverlayItem> overlayItems = listOverlayRoute.getOverlayItems();
		overlayItems.add(overlayRouteItem);

		this.mapView.getOverlays().add(listOverlayRoute);
	}

	public void setPolygonOverlay(List<GeoPoint> polygon) {
		PolygonalChain polyChain = new PolygonalChain(polygon);

		OverlayItem overlayPolygonItem = new Polyline(polyChain, greenPaint);

		ListOverlay listOverlayPolygon = new ListOverlay();
		List<OverlayItem> overlayItems = listOverlayPolygon.getOverlayItems();
		overlayItems.add(overlayPolygonItem);

		this.mapView.getOverlays().add(listOverlayPolygon);
	}

	public void setGraphOverlay(Collection<ExtendedVertex> vertices) {
		ListOverlay listOverlayGraph = new ListOverlay();
		List<OverlayItem> overlayItemsList = listOverlayGraph.getOverlayItems();

		for (ExtendedVertex v : vertices) {
			GeoCoordinate vCoord = v.getVertex().getCoordinate();
			OverlayItem vertexItem = new Circle(new GeoPoint(vCoord.getLatitude(), vCoord.getLongitude()), 5, redPaint,
					redPaint);
			overlayItemsList.add(vertexItem);

			GeoCoordinate vPredCoord = v.getPredecessorEdge().getSource().getCoordinate();
			List<GeoPoint> edgePoints = new LinkedList<GeoPoint>();
			edgePoints.add(new GeoPoint(vCoord.getLatitude(), vCoord.getLongitude()));
			edgePoints.add(new GeoPoint(vPredCoord.getLatitude(), vPredCoord.getLongitude()));
			PolygonalChain polyChain = new PolygonalChain(edgePoints);

			OverlayItem edgeItem = new Polyline(polyChain, redPaint);
			overlayItemsList.add(edgeItem);
		}
		this.mapView.getOverlays().add(listOverlayGraph);
	}

	public void setGraphOverlayGreen(Collection<ExtendedVertex> vertices) {
		ListOverlay listOverlayGraph = new ListOverlay();
		List<OverlayItem> overlayItemsList = listOverlayGraph.getOverlayItems();

		for (ExtendedVertex v : vertices) {
			GeoCoordinate vCoord = v.getVertex().getCoordinate();
			OverlayItem vertexItem = new Circle(new GeoPoint(vCoord.getLatitude(), vCoord.getLongitude()), 5,
					greenPaint, greenPaint);
			overlayItemsList.add(vertexItem);

			GeoCoordinate vPredCoord = v.getPredecessorEdge().getSource().getCoordinate();
			List<GeoPoint> edgePoints = new LinkedList<GeoPoint>();
			edgePoints.add(new GeoPoint(vCoord.getLatitude(), vCoord.getLongitude()));
			edgePoints.add(new GeoPoint(vPredCoord.getLatitude(), vPredCoord.getLongitude()));
			PolygonalChain polyChain = new PolygonalChain(edgePoints);

			OverlayItem edgeItem = new Polyline(polyChain, greenPaint);
			overlayItemsList.add(edgeItem);
		}
		this.mapView.getOverlays().add(listOverlayGraph);
	}
}
