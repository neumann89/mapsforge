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
package org.mapsforge.applications.android.samples;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.applications.android.samples.overlay.OverlayManager;
import org.mapsforge.applications.android.samples.settings.Settings;
import org.mapsforge.applications.android.samples.views.PoiSearchHandler;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.poi.searching.ExtendedPoi;
import org.mapsforge.poi.searching.PoiSearch;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Simple Activity which demonstrates the different implementations of {@link PoiSearch}.
 */
public class NearbyPoiMapViewer extends MapActivity {

	private Collection<ExtendedPoi> choosenPoi = new LinkedList<ExtendedPoi>();
	private MapView mapView;
	private Menu menu;
	private int poiListMenuItemId = -1;
	private PoiSearchHandler poiSearchHandler;
	private OverlayManager overlayManager;
	private Settings settings = new Settings();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mapView = new MapView(this);
		this.mapView.setClickable(true);
		this.mapView.setBuiltInZoomControls(true);
		FileOpenResult fileOpenResult = this.mapView.setMapFile(this.settings.MAP_FILE);
		if (!fileOpenResult.isSuccess()) {
			Toast.makeText(this, fileOpenResult.getErrorMessage(), Toast.LENGTH_LONG).show();
			finish();
		}

		this.overlayManager = new OverlayManager(this, this.mapView);

		setStartView();
	}

	/**
	 * Initial view with map and route.
	 */
	public void setStartView() {
		List<Overlay> mapViewOverlay = this.mapView.getOverlays();
		mapViewOverlay.clear();

		this.overlayManager.setRouteOverlay(this.settings.route);
		this.overlayManager.setPoiOverlay(this.choosenPoi);
		setContentView(this.mapView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu1) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.poi_search_menu, menu1);
		this.menu = menu1;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		// Handle item selection
		if (id == R.id.find_poi) {
			this.poiSearchHandler = new PoiSearchHandler(this);
			if (this.settings.verbose) {
				this.poiSearchHandler.findPoiNearRouteVerbose();
			} else {
				this.poiSearchHandler.findPoiNearRoute();
			}
			return true;
		} else if (id == this.poiListMenuItemId) {
			this.poiSearchHandler.choosePoiFromListView(this.poiListMenuItemId);
			return true;
		} else if (id == R.id.circumcircle || id == R.id.polygon) {
			// || id == R.id.extended_circumcircle || id == R.id.adjusting_circumcircle
			// || id == R.id.best_first_network_expansion || id == R.id.sp_DB) {
			item.setChecked(true);
			this.settings.changePoiSearchMode(id);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Gives access to the activity's menu.
	 * 
	 * @return {@link Menu} menu
	 */
	public Menu getActivityMenu() {
		return this.menu;
	}

	/**
	 * Add a new menu item for showing the found poi in a list.
	 */
	public void addPoiListMenuItem() {
		if (this.poiListMenuItemId == -1) {
			MenuItem item = this.menu.add("Show Poi List");
			this.poiListMenuItemId = item.getItemId();
		}
	}

	/**
	 * Removes the poi list menu item.
	 * 
	 * @param id
	 *            The identifier of the menu item.
	 */
	public void removePoiListMenuItem(int id) {
		this.menu.removeItem(id);
		this.poiListMenuItemId = -1;
	}

	/**
	 * Gives access to the map view.
	 * 
	 * @return {@link MapView} mapView
	 */
	public MapView getMapView() {
		return this.mapView;
	}

	/**
	 * Puts a poi to the list of selected poi.
	 * 
	 * @param poi
	 *            {@link Collection} of {@link ExtendedPoi}
	 */
	public void addAllPoi(Collection<ExtendedPoi> poi) {
		this.choosenPoi.addAll(poi);
	}

	/**
	 * Gives access to settings.
	 * 
	 * @return {@link Settings} settings
	 */
	public Settings getSettings() {
		return this.settings;
	}

	/**
	 * Gives access to the activity's overlaymanager.
	 * 
	 * @return {@link OverlayManager} overlayManager
	 */
	public OverlayManager getOverlayManager() {
		return this.overlayManager;
	}

}
