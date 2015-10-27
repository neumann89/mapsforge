/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.storage.poi;

import java.util.Collection;

/**
 * This class generates a prepared SQL query for retrieving POIs filtered by a given {@link PoiCategoryFilter}.
 * 
 * @author Karsten Groll
 */
public class PoiCategoryRangeQueryGenerator {

	private static final String SELECT_QUERY_STRING = "SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
			+ "FROM poi_index "
			+ "JOIN poi_data ON poi_index.id = poi_data.id "
			+ "WHERE ("
			+ "minLat <= ? AND "
			+ "minLon <= ? AND " + "minLat >= ? AND " + "minLon >= ?)";

	// TODO limit?
	static String getSQLSelectString(PoiCategoryFilter filter) {
		StringBuilder sb = new StringBuilder(SELECT_QUERY_STRING);
		sb.append(getSQLWhereClauseString(filter));
		sb.append(' ').append("LIMIT ?;");
		return sb.toString();
	}

	/**
	 * Gets the WHERE clause for the SQL query that looks up POI entries.
	 * 
	 * @param filter
	 *            The filter object for determining all wanted categories.
	 * @return A string like <code>WHERE id BETWEEN 2 AND 5 OR BETWEEN 10 AND 12</code>.
	 */
	private static String getSQLWhereClauseString(PoiCategoryFilter filter) {
		int[] intervals = getCategoryIDIntervals(filter);

		if (intervals.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append(" AND (");
		// foreach interval
		for (int i = 0; i < intervals.length; i += 2) {

			// Element has two siblings
			sb.append("poi_data.category > ").append(intervals[i]).append(" AND poi_data.category <= ")
					.append(intervals[i + 1]);

			// append OR if it is not the last interval
			if (i != intervals.length - 2) {
				sb.append(" OR ");
			}
		}

		sb.append(')');

		return sb.toString();
	}

	//
	/**
	 * @param filter
	 *            The filter object for determining all wanted categories.
	 * @return Array with two elements as a half open interval: <code>(min ID, max ID]</code>
	 */
	private static int[] getCategoryIDIntervals(PoiCategoryFilter filter) {
		Collection<PoiCategory> superCategories = filter.getAcceptedSuperCategories();

		int[] ret = new int[superCategories.size() * 2];
		int i = 0;

		PoiCategory sibling = null;

		for (PoiCategory c : superCategories) {
			sibling = PoiCategoryRangeQueryGenerator.getLeftSibling(c);

			if (sibling == null) {
				ret[i] = -1;
				ret[i + 1] = c.getID();
			} else {
				ret[i] = sibling.getID();
				ret[i + 1] = c.getID();
			}

			i += 2;
		}

		return ret;
	}

	/**
	 * @param c1
	 *            Category whose nearest left-hand side sibling should be returned.
	 * @return The category's nearest left-hand side sibling or <code>null</code> if <code>c1</code> is the leftmost
	 *         category on its level.
	 */
	private static PoiCategory getLeftSibling(PoiCategory c1) {
		PoiCategory ret = null;

		if (c1.getParent() == null) {
			return null;
		}

		int maxID = -1;

		// TODO Modify algorithm for non-sorted sets
		for (PoiCategory c : c1.getParent().getChildren()) {
			// Found a left sibling
			if (c.getID() < c1.getID() && c.getID() > maxID) {
				maxID = c.getID();
				ret = c;
			}
		}

		return ret;
	}

	// public static void main(String[] args) throws UnknownPoiCategoryException {
	// PoiCategory root = CategoryTreeBuilder.createAndGetBalancedConfiguration();
	// PoiCategoryManager cm = new CategoryManagerTest.MockPoiCategoryManager(root);
	//
	// System.out.println(getLeftSibling(cm.getPoiCategoryByID(11)));
	// }

}
