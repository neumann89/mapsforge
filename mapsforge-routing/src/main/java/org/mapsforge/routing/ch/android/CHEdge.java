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
package org.mapsforge.routing.ch.android;

import org.mapsforge.routing.android.data.ObjectPool;

/**
 * A Contraction Hierarchies graph's edge.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHEdge.java 1746 2012-01-16 22:38:34Z Patrick.Jungermann@googlemail.com $
 */
class CHEdge implements ObjectPool.Poolable {

	/**
	 * Whether this edge was released or not.
	 */
	private boolean released;

	/**
	 * The edge's source vertex' identifier.
	 */
	private int sourceId;
	/**
	 * The edge's target vertex' identifier.
	 */
	private int targetId;
	/**
	 * Whether source and target have been switched or not.
	 */
	private boolean switchedSourceAndTarget = false;
	/**
	 * The edge's lowest vertex' identifier.
	 */
	private int lowestVertexId;
	/**
	 * The edge's highest vertex' identifier, based on their levels.
	 */
	private int highestVertexId;
	/**
	 * The edge's weight.
	 */
	public int weight;
	/**
	 * Whether this edge is a forward edge or not.
	 */
	public boolean forward;
	/**
	 * Whether this edge is a backward edge or not.
	 */
	public boolean backward;
	/**
	 * Pairs of latitude and longitude values (both in E6 format) of its waypoints. The number of
	 * waypoints is the half of its length.<br/>
	 * This field contains a valid value only for normal edges ({@see #shortcut}).
	 */
	public int[] waypoints;
	/**
	 * The edge's name's data.<br/>
	 * This field contains a valid value only for normal edges ({@see #shortcut}).
	 */
	public byte[] name;
	/**
	 * The edge's reference's data.<br/>
	 * This field contains a valid value only for normal edges ({@see #shortcut}).
	 */
	public byte[] ref;
	/**
	 * The edge's street type's identifier.<br/>
	 * This field contains a valid value only for normal edges ({@see #shortcut}).
	 */
	public int streetTypeId;
	/**
	 * Whether this edge is a roundabout or not.<br/>
	 * This field contains a valid value only for normal edges ({@see #shortcut}).
	 */
	public boolean roundabout;
	/**
	 * Whether this edge is a shortcut or not.
	 */
	public boolean shortcut;
	/**
	 * Whether this shortcut edge is an external shortcut or not.<br/>
	 * This field contains a valid value only for shortcut edges ({@see #shortcut}).
	 */
	public boolean external;
	/**
	 * The shortcut edge's path's <strong>bit</strong> offset.<br/>
	 * The is field only contains a valid value only for external shortcuts ({@see #shortcut}, {@see
	 * #external}).
	 */
	public int shortcutPathBitOffset;
	/**
	 * The shortcut edge's path's length.<br/>
	 * The is field only contains a valid value only for external shortcuts ({@see #shortcut}, {@see
	 * #external}).
	 */
	public int shortcutPathLength;
	/**
	 * The shortcut's bypassed (middle) vertex' offset, needed to unpack internal shortcuts.<br/>
	 * This field contains a valid value only for internal shortcuts ({@see #shortcut}, {@see #external}
	 * ).
	 */
	public int bypassedVertexId;

	@Override
	public boolean isReleased() {
		return released;
	}

	@Override
	public void setReleased(boolean released) {
		this.released = released;
	}

	/**
	 * Returns the edge's lowest vertex' identifier.
	 * 
	 * @return The edge's lowest vertex' identifier.
	 */
	public int getLowestVertexId() {
		return lowestVertexId;
	}

	/**
	 * Sets the edge's lowest vertex' identifier.
	 * 
	 * @param lowestVertexId
	 *            The edge's lowest vertex' identifier.
	 */
	public void setLowestVertexId(int lowestVertexId) {
		this.lowestVertexId = lowestVertexId;

		if (forward) {
			sourceId = lowestVertexId;

		} else {
			targetId = lowestVertexId;
		}
		switchedSourceAndTarget = false;
	}

	/**
	 * Returns the edge's highest vertex' identifier, based on their levels.
	 * 
	 * @return The edge's highest vertex' identifier, based on their levels.
	 */
	public int getHighestVertexId() {
		return highestVertexId;
	}

	/**
	 * Sets the edge's highest vertex' identifier, based on their levels.
	 * 
	 * @param highestVertexId
	 *            The edge's highest vertex' identifier, based on their levels.
	 */
	public void setHighestVertexId(int highestVertexId) {
		this.highestVertexId = highestVertexId;

		if (forward) {
			targetId = highestVertexId;

		} else {
			sourceId = highestVertexId;
		}
		switchedSourceAndTarget = false;
	}

	/**
	 * Returns the edge's source vertex' identifier.
	 * 
	 * @return The edge's source vertex' identifier.
	 */
	public int getSourceId() {
		return sourceId;
	}

	/**
	 * Returns the edge's target vertex' identifier.
	 * 
	 * @return The edge's target vertex' identifier.
	 */
	public int getTargetId() {
		return targetId;
	}

	/**
	 * Switches the source and the target of this edge. This might be needed for undirected edges inside
	 * of a path.
	 */
	public void switchSourceAndTarget() {
		final int tmp = sourceId;
		sourceId = targetId;
		targetId = tmp;

		// toggle flag
		switchedSourceAndTarget = !switchedSourceAndTarget;
	}

	/**
	 * Returns the waypoints, respecting the current source/target setting.
	 * 
	 * @return The waypoints with respect to the current source/target setting.
	 */
	public int[] getWaypoints() {
		return switchedSourceAndTarget ? reverseWaypoints() : waypoints;
	}

	/**
	 * Returns a reversed version of the waypoints (pairs of latitude and longitude values).
	 * 
	 * @return The reversed waypoints.
	 */
	protected int[] reverseWaypoints() {
		int[] reversed = new int[waypoints.length];
		int numPairs = waypoints.length / 2;
		for (int i = 0; i < numPairs; i++) {
			reversed[i * 2] = waypoints[(numPairs - i) * 2 - 2];
			reversed[i * 2 + 1] = waypoints[(numPairs - i) * 2 - 1];
		}

		return reversed;
	}

}
