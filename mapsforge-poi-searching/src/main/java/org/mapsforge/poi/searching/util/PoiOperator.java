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

import java.util.Collection;
import java.util.List;

import org.mapsforge.poi.searching.ExtendedPoi;

/**
 * Util for lists and collection of {@link ExtendedPoi}.
 */
public class PoiOperator {

	public static int getIndexExtendedPoi(List<ExtendedPoi> poiList,
			ExtendedPoi poi) {
		for (ExtendedPoi p : poiList) {
			if (p.getPoi().getId() == poi.getPoi().getId()) {
				return poiList.indexOf(p);
			}
		}
		return -1;
	}

	/*
	 * Ensures that path to poi is shortest, if a poi is found from multiple
	 * route vertices.
	 */
	public static void integrateNewPoiInList(
			Collection<ExtendedPoi> newPoiCollection,
			List<ExtendedPoi> poiList, int poiNumberLimit) {
		for (ExtendedPoi p : newPoiCollection) {
			int indexInList = getIndexExtendedPoi(poiList, p);
			if (indexInList >= 0) {
				ExtendedPoi poiInList = poiList.get(indexInList);
				if (p.getDistance() < poiInList.getDistance()) {
					poiInList.setDistance(p.getDistance());
					poiInList.setNearestRouteVertex(p.getNearestRouteVertex());
				}
			} else if (poiList.size() < poiNumberLimit) {
				poiList.add(p);
			}
		}
	}

}
