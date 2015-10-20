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
package org.mapsforge.routing.preprocessing.data.clustering;

/**
 * Exception, which could be thrown, if one of the selectable {@link ClusteringAlgorithm}s aren't
 * supported (at the moment).
 * 
 * @author Patrick Jungermann
 * @version $Id: UnsupportedClusteringAlgorithmException.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class UnsupportedClusteringAlgorithmException extends Exception {

	/**
	 * The serial version UID of this class.
	 */
	private static final long serialVersionUID = 8337641770730403623L;

	/**
	 * Constructs an instance of this exception type.
	 * 
	 * @param algorithm
	 *            The selected, but unsupported {@link ClusteringAlgorithm}.
	 */
	public UnsupportedClusteringAlgorithmException(ClusteringAlgorithm algorithm) {
		super(new StringBuilder().append("The clustering algorithm ")
				.append(algorithm != null ? algorithm.label : "null")
				.append(" is not supported").toString());
	}
}
