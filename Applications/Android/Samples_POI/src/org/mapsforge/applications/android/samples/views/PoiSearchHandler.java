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
package org.mapsforge.applications.android.samples.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mapsforge.applications.android.samples.NearbyPoiMapViewer;
import org.mapsforge.applications.android.samples.R;
import org.mapsforge.applications.android.samples.overlay.OverlayManager;
import org.mapsforge.applications.android.samples.settings.Settings;
import org.mapsforge.poi.searching.ExtendedPoi;
import org.mapsforge.poi.searching.PoiSearch;
import org.mapsforge.poi.searching.circumcircle.CircumcirclePoiSearch;
import org.mapsforge.poi.searching.polygon.PolygonPoiSearch;
import org.mapsforge.routing.Edge;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class PoiSearchHandler {
	NearbyPoiMapViewer activity;
	PoiSearch poiSearch;
	List<ExtendedPoi> poiList = new ArrayList<ExtendedPoi>();

	public PoiSearchHandler(NearbyPoiMapViewer activity) {
		this.activity = activity;
	}

	// search poi and show marker an the map for results
	public void findPoiNearRoute() {
		Settings settings = this.activity.getSettings();
		int maxDistance = settings.maxDistance;
		int poiLimit = settings.poiLimit;
		this.poiSearch = settings.poiSearch;

		Collection<ExtendedPoi> poi = this.poiSearch.searchNearRoute(maxDistance, poiLimit);

		// refresh poi list
		poiList.clear();
		poiList.addAll(poi);
		Collections.sort(poiList);

		OverlayManager overlayManager = this.activity.getOverlayManager();
		overlayManager.setPoiOverlay(poi);

		// add poilist MenuItem
		activity.addPoiListMenuItem();

	}

	// show specific overlays for each poi-search method
	public void findPoiNearRouteVerbose() {
		Log.d("PoSearchHandler: ", "Verbose");
		Settings settings = this.activity.getSettings();

		int maxDistance = settings.maxDistance;
		int poiLimit = settings.poiLimit;
		Edge[] route = settings.route;
		this.poiSearch = settings.poiSearch;

		Collection<ExtendedPoi> poi = poiSearch.searchNearRoute(maxDistance, poiLimit);
		// refresh poi list
		poiList.clear();
		poiList.addAll(poi);
		Collections.sort(poiList);

		// show poi
		OverlayManager overlayManager = this.activity.getOverlayManager();
		overlayManager.setPoiOverlay(poi);

		if (this.poiSearch instanceof CircumcirclePoiSearch) {
			// Log.d("PoSearchHandler: ", "Verbose Circumcircle");
			// show circumcircles
			overlayManager.setRouteCircumcircleOverlay(route, maxDistance);

			// } else if (this.poiSearch instanceof ExtendedCircumcirclePoiSearch) {
			// // Log.d("PoSearchHandler: ", "Verbose ExtendedCircumcircle");
			// ExtendedCircumcirclePoiSearch poiSearchExtCircumCircle = (ExtendedCircumcirclePoiSearch) this.poiSearch;
			//
			// // show circumcircles
			// overlayManager.setRouteCircumcircleOverlay(route, maxDistance);
			//
			// // show search graph
			// Collection<ExtendedVertex> vertices = poiSearchExtCircumCircle.getNearRouteVertices();
			// overlayManager.setGraphOverlay(vertices);
			//
			// } else if (this.poiSearch instanceof AdjustingCircumcirclePoiSearch) {
			// AdjustingCircumcirclePoiSearch poiSearchAdjusted = (AdjustingCircumcirclePoiSearch) this.poiSearch;
			//
			// overlayManager.setAdjustedCircumcircleOverlay(route, poiSearchAdjusted.getDistanceList());
			//
			// // show search graph
			// Collection<ExtendedVertex> vertices = poiSearchAdjusted.getNearRouteVertices();
			// overlayManager.setGraphOverlay(vertices);
			//
		} else if (this.poiSearch instanceof PolygonPoiSearch) {
			// Log.d("PoSearchHandler: ", "Verbose Polygon");
			PolygonPoiSearch poiSearchPolygon = (PolygonPoiSearch) this.poiSearch;

			// show polygon
			overlayManager.setPolygonOverlay(poiSearchPolygon.getPolygon());
		}
		// } else if (this.poiSearch instanceof BestFirstNetworkExpansionPoiSearch) {
		// BestFirstNetworkExpansionPoiSearch poiSearchBNE = (BestFirstNetworkExpansionPoiSearch) this.poiSearch;
		//
		// // show search graph
		// overlayManager.setGraphOverlay(poiSearchBNE.getNearestStartVertices());
		// overlayManager.setGraphOverlayGreen(poiSearchBNE.getNearestTargetVertices());
		//
		// } else if (this.poiSearch instanceof FloydPoiSearch) {
		// FloydPoiSearch poiSearchFloyd = (FloydPoiSearch) this.poiSearch;
		// // TODO maybe draw euclidean lines between route and poi
		// }

		// add poilist MenuItem
		activity.addPoiListMenuItem();

	}

	// TODO better R.id names
	public void choosePoiFromListView(final int poiListMenuItemId) {

		final View poilistView = activity.getLayoutInflater().inflate(R.layout.poilist, null);

		// activity.getWindow().addContentView(poilistView,
		// new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
		// activity.getWindowManager().addView(
		// poilistView,
		// new WindowManager.LayoutParams(WindowManager.LayoutParams.FILL_PARENT,
		// WindowManager.LayoutParams.FILL_PARENT));
		final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
		rootView.addView(poilistView);

		ListView listView = (ListView) poilistView.findViewById(R.id.poilistview);
		final PoiListAdapter adapter = new PoiListAdapter(activity, R.id.label, poiList);
		listView.setAdapter(adapter);

		Button buttonOkLeft = (Button) poilistView.findViewById(R.id.poilist_button);
		buttonOkLeft.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Collection<ExtendedPoi> choosenPoi = adapter.getSelectedPoi();
				Edge[] newRoute = poiSearch.recalculateRoute(choosenPoi);

				rootView.removeAllViews();
				activity.getSettings().route = newRoute;
				activity.addAllPoi(choosenPoi);
				activity.removePoiListMenuItem(poiListMenuItemId);
				activity.setStartView();
			}
		});

		Button buttonCancelRight = (Button) poilistView.findViewById(R.id.poilist_button2);
		buttonCancelRight.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				adapter.getSelectedPoi().clear();
				rootView.removeView(poilistView);
			}
		});

	}

}
