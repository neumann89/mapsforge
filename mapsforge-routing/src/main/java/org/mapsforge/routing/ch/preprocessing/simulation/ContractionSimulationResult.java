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
package org.mapsforge.routing.ch.preprocessing.simulation;

/**
 * Result of a contraction simulation, which contains some information needed for the node ordering, and
 * could also be used for the (real) contraction itself.
 * 
 * @author Patrick Jungermann
 * @version $Id: ContractionSimulationResult.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class ContractionSimulationResult {

	/**
	 * The number of introduced shortcuts.
	 */
	public int numOfShortcuts;

	/**
	 * The number of "removed" edges.
	 */
	public int numOfRemovedEdges;

	/**
	 * The edge pairs for each shortcut.
	 */
	public int[][] shortcutEdgePairs;

	/**
	 * The sum of the original edge count values of all new edges (shortcuts).
	 */
	public int originalEdgeCountAdded;

	/**
	 * The sum of the original edge count value of all removed edges.
	 */
	public int originalEdgeCountRemoved;
}
