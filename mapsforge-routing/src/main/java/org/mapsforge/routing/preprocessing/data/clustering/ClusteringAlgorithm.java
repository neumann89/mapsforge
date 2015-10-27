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
 * All selectable clustering algorithms.
 * 
 * @author Patrick Jungermann
 * @version $Id: ClusteringAlgorithm.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public enum ClusteringAlgorithm {

	/**
	 * Clustering based on the K-Center algorithm.
	 */
	K_CENTER("K-Center"),
	/**
	 * Clustering based on the Quad-Tree algorithm.
	 */
	QUAD_TREE("Quad-Tree"),
	/**
	 * Clustering based on the topological order.
	 */
	TOPOLOGICAL_ORDER("Topological Order");

	/**
	 * Label of this clustering algorithm.
	 */
	public final String label;// TODO: label really needed?

	/**
	 * Constructs a clustering algorithm entry.
	 * 
	 * @param label
	 *            The label of this clustering algorithm.
	 */
	ClusteringAlgorithm(String label) {
		this.label = label;
	}
}
