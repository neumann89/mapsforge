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
package org.mapsforge.applications.android.samples.settings;

import java.io.File;

import org.mapsforge.applications.android.samples.R;
import org.mapsforge.poi.searching.PoiSearch;
import org.mapsforge.poi.searching.circumcircle.CircumcirclePoiSearch;
import org.mapsforge.poi.searching.polygon.PolygonPoiSearch;
import org.mapsforge.routing.Edge;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.Router;
import org.mapsforge.routing.Vertex;
import org.mapsforge.routing.hh.android.HHRouter;
import org.mapsforge.storage.poi.ExactMatchPoiCategoryFilter;
import org.mapsforge.storage.poi.PoiCategoryFilter;
import org.mapsforge.storage.poi.PoiCategoryManager;
import org.mapsforge.storage.poi.PoiPersistenceManager;
import org.mapsforge.storage.poi.PoiPersistenceManagerFactory;

import android.os.Environment;

public class Settings {
	// needed values, the user is free to change them
	public File MAP_FILE = new File(Environment.getExternalStorageDirectory().getPath(), "berlin.map");
	public File ROUTING_GRAPH_FILE = new File(Environment.getExternalStorageDirectory().getPath(), "berlin.mobileHH");
	public String POI_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/berlin.poi";
	// public String SHORTEST_PATH_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/berlin.poiSP";

	public GeoCoordinate start = new GeoCoordinate(52.516272, 13.377722); // Brandenburg Gate
	public GeoCoordinate target = new GeoCoordinate(52.486272, 13.447722);

	public boolean isPoiFilterActive = true;
	public int maxDistance = 300;
	public int poiLimit = 10;
	public int vertexPoiRadius = 30;
	public boolean verbose = true;

	// internal
	public Router router = null;
	public Edge[] route = null;
	public PoiPersistenceManager poiManager = null;
	public PoiCategoryFilter poiFilter = null;
	public PoiSearch poiSearch = null;

	public Settings() {
		try {
			this.router = new HHRouter(this.ROUTING_GRAPH_FILE, 1024);
		} catch (Exception e) {
			// TODO android error handling
		}
		this.poiManager = PoiPersistenceManagerFactory.getSQLitePoiPersistenceManager(this.POI_FILE_PATH);

		// default Poi-Search Implementation
		this.poiSearch = new CircumcirclePoiSearch(this.router, this.poiManager);

		this.createRoute();

		// poi filter
		PoiCategoryManager cm = this.poiManager.getCategoryManager();
		PoiCategoryFilter filter = new ExactMatchPoiCategoryFilter();
		try {
			// filter.addCategory(cm.getPoiCategoryByTitle("Restaurants"));
			// filter.addCategory(cm.getPoiCategoryByTitle("Taxi stands"));
			// filter.addCategory(cm.getPoiCategoryByTitle("Cafes"));
			filter.addCategory(cm.getPoiCategoryByTitle("Banks"));

			if (isPoiFilterActive) {
				this.poiFilter = filter;
				this.poiSearch.setPoiFilter(this.poiFilter);
			}
		} catch (Exception e) {
			// TODO android error handling
		}
	}

	public void createRoute() {
		Vertex startVertex = this.router.getNearestVertex(this.start);
		Vertex targetVertex = this.router.getNearestVertex(this.target);
		Edge[] route = this.router.getShortestPath(startVertex.getId(), targetVertex.getId());
		this.route = route;
		this.poiSearch.setRoute(route);
	}

	public void changePoiSearchMode(int id) {
		if (id == R.id.circumcircle) {
			this.poiSearch = new CircumcirclePoiSearch(this.router, this.poiManager);
			// } else if (id == R.id.extended_circumcircle) {
			// this.poiSearch = new ExtendedCircumcirclePoiSearch(this.router, this.poiManager, this.vertexPoiRadius);
			// } else if (id == R.id.adjusting_circumcircle) {
			// this.poiSearch = new AdjustingCircumcirclePoiSearch(this.router, this.poiManager, this.vertexPoiRadius);
			// } else if (id == R.id.best_first_network_expansion) {
			// this.poiSearch = new BestFirstNetworkExpansionPoiSearch(this.router, this.poiManager,
			// this.vertexPoiRadius);
		} else if (id == R.id.polygon) {
			this.poiSearch = new PolygonPoiSearch(this.router, this.poiManager);
		}
		// } else if (id == R.id.sp_DB) {
		// this.poiSearch = new FloydPoiSearch(this.router, this.poiManager, this.SHORTEST_PATH_FILE_PATH);
		// }
		this.poiSearch.setRoute(this.route);
		this.poiSearch.setPoiFilter(this.poiFilter);
	}
}
