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
package org.mapsforge.routing.preprocessing.data;

import java.sql.Array;
import java.sql.SQLException;

/**
 * Utility methods, related to database interactions.
 * 
 * @author Patrick Jungermann
 * @version $Id: DatabaseUtils.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class DatabaseUtils {

	/**
	 * Extracts a primitive double array out of an SQL array.
	 * 
	 * @param array
	 *            The SQL array. It must contain an array of type Double[].
	 * @return A primitive double array of the same order.
	 * @throws java.sql.SQLException
	 *             on error accessing the array.
	 */
	public static double[] toDoubleArray(Array array) throws SQLException {
		if (array == null) {
			return null;
		}

		Double[] tmp = (Double[]) array.getArray();
		double[] result = new double[tmp.length];
		for (int i = 0; i < tmp.length; i++) {
			result[i] = tmp[i];
		}

		return result;
	}
}
