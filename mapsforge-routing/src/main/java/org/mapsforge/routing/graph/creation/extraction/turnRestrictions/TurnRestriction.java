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
package org.mapsforge.routing.graph.creation.extraction.turnRestrictions;

/**
 * This class represents a simple turn restriction.
 * 
 * @author Robert Fels
 * 
 */
public class TurnRestriction {

	int id;
	long osmId;
	int fromEdgeId;

	int viaNodeId;
	int toEdgeId;

	/**
	 * 
	 * @param id
	 *            id of turn restriction
	 * @param osmId
	 *            osm id
	 * @param fromEdgeId
	 *            from edge (edge where you come from)
	 * @param viaNodeId
	 *            via node (node which you pass)
	 * @param toEdgeId
	 *            to edge (edge where you are not suppose to go)
	 */
	public TurnRestriction(int id, long osmId, int fromEdgeId, int viaNodeId, int toEdgeId) {
		this.id = id;
		this.osmId = osmId;
		this.fromEdgeId = fromEdgeId;
		this.viaNodeId = viaNodeId;
		this.toEdgeId = toEdgeId;
	}

	/**
	 * Getter for TR id
	 * 
	 * @return id of TR
	 */
	public int getId() {
		return id;
	}

	/**
	 * getter for osm id
	 * 
	 * @return osm id
	 */
	public long getOsmId() {
		return osmId;
	}

	/**
	 * 
	 * @return id of from edge
	 */
	public int getFromEdgeId() {
		return fromEdgeId;
	}

	/**
	 * 
	 * @return id of via node
	 */
	public int getViaNodeId() {
		return viaNodeId;
	}

	/**
	 *
	 * @param id
	 *            id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 *
	 * @param fromEdgeId
	 *            from edge
	 */
	public void setFromEdgeId(int fromEdgeId) {
		this.fromEdgeId = fromEdgeId;
	}

	/**
	 *
	 * @param viaNodeId
	 *            via node
	 */
	public void setViaNodeId(int viaNodeId) {
		this.viaNodeId = viaNodeId;
	}

	/**
	 *
	 * @param toEdgeId
	 *            to edge id
	 */
	public void setToEdgeId(int toEdgeId) {
		this.toEdgeId = toEdgeId;
	}

	/**
	 * 
	 * @return id of to edge
	 */
	public int getToEdgeId() {
		return toEdgeId;
	}

	@Override
	public String toString() {
		String s = "[TurnRestriction " + this.id;
		s += " osm-id: " + this.osmId;
		s += " from-id: " + this.fromEdgeId;
		s += " to-id: " + this.toEdgeId;
		s += " via-id: " + this.viaNodeId;
		s += "]";
		return s;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TurnRestriction || obj instanceof CompleteTurnRestriction) {
			TurnRestriction tr = (TurnRestriction) obj;
			if (this.toEdgeId == tr.getToEdgeId() && this.fromEdgeId == tr.getFromEdgeId()
					&& this.viaNodeId == tr.getViaNodeId())
				return true;
			return false;
		}
		return false;
	}

	@Override
	public int hashCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.valueOf(this.fromEdgeId));
		sb.append(String.valueOf(this.toEdgeId));
		sb.append(String.valueOf(this.viaNodeId));
		return sb.toString().hashCode();
	}

}
