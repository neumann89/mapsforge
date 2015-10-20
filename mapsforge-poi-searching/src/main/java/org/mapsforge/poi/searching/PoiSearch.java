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
package org.mapsforge.poi.searching;

import java.util.Collection;
import org.mapsforge.routing.Edge;
import org.mapsforge.storage.poi.PoiCategoryFilter;

/**
 * Main interface for poi-searching which abstracts from the underlying search implementation.
 * It provides methods for getting the route's nearest poi and recalculating the route to some
 * choosen poi.
 * Usually an implementation needs a {@link Router} and a {@link PoiPersistenceManager} for 
 * getting poi and operating with the route.
 *   
 * Remember to set the route before you start searching.
 */
public interface PoiSearch {
	
	/**
	 * Determine the route at which can be searched for nearest poi.
	 * @param route The route to be set.
	 */
	void setRoute(Edge[] route);
	
	/**
	 * Optional method: Determine a {@link PoiCategoryFilter} 
	 * @param acceptedCategories Allowed poi categories.
	 */
	void setPoiFilter(PoiCategoryFilter acceptedCategories);
	
	/**
	 * Tries to find nearby poi of the route and return a collection of search results.
	 * Remember to set the route at first and then start searching.
	 * @param maxDistance The maximum range for searching.
	 * @param poiNumberLimit The maximum number of poi, that can be found.
	 * @return A collection of nearby poi.
	 */
	Collection<ExtendedPoi> searchNearRoute(int maxDistance, int poiNumberLimit);
	
	/**
	 * Gets a selection of poi the user wants to visit. The route has to recalculated and 
	 * add detours to these poi to it.
	 * @param choosenPoi A {@link Collection} of {@link ExtendedPoi} the user has selected.
	 * @return The new route which contains detours to the choosen poi.
	 */
	Edge[] recalculateRoute(Collection<ExtendedPoi> choosenPoi);

}
