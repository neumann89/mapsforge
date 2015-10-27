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

import gnu.trove.set.hash.THashSet;
import org.mapsforge.routing.graph.creation.extraction.KeyValuePair;

/**
 * This class represents a simple turn restriction with addidtional OSM-tags.
 *
 * @author Robert Fels
 * @see TurnRestriction
 */
public class CompleteTurnRestriction extends TurnRestriction {

	long osmFromId;
	long osmToId;
	long osmViaId;

	THashSet<KeyValuePair> tags;

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
	 * @param osmFromId
	 *            original osm from edge id
	 * @param osmToId
	 *            original osm to edge id
	 * @param osmViaId
	 *            original osm via node id
	 * @param tags
	 *            additional tags
	 */
	public CompleteTurnRestriction(int id, long osmId, int fromEdgeId, int viaNodeId, int toEdgeId,
			long osmFromId, long osmToId, long osmViaId,
			THashSet<KeyValuePair> tags) {
		super(id, osmId, fromEdgeId, viaNodeId, toEdgeId);
		this.osmFromId = osmFromId;
		this.osmToId = osmToId;
		this.osmViaId = osmViaId;
		this.tags = tags;
	}

	/**
	 *
	 * @return all tags in a THashSet
	 */
	public THashSet<KeyValuePair> getTags() {
		return tags;
	}

	/**
	 * set tags
	 *
	 * @param tags
	 *            tags
	 */
	public void setTags(THashSet<KeyValuePair> tags) {
		this.tags = tags;
	}

	/**
	 *
	 * @return osm from id
	 */
	public long getOsmfromId() {
		return osmFromId;
	}

	/**
	 *
	 * @return osm to id
	 */
	public long getOsmtoId() {
		return osmToId;
	}

	/**
	 *
	 * @return osm via id
	 */
	public long getOsmViaId() {
		return osmViaId;
	}

	@Override
	public String toString() {
		String s = "[TurnRestriction " + this.id;
		s += " osm-id: " + this.osmId;
		s += " from-id: " + this.fromEdgeId;
		s += " to-id: " + this.toEdgeId;
		s += " via-id: " + this.viaNodeId;
		s += " osm-from-id: " + this.osmFromId;
		s += " osm-to-id: " + this.osmToId;
		s += " osm-via-id: " + this.osmViaId;
		s += " TAGS ";
		for (KeyValuePair kv : this.tags) {
			s += kv.toString() + ", ";
		}
		s += "]";
		return s;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TurnRestriction || obj instanceof CompleteTurnRestriction) {
			CompleteTurnRestriction tr = (CompleteTurnRestriction) obj;
			if (this.toEdgeId == tr.getToEdgeId() && this.fromEdgeId == tr.getFromEdgeId()
					&& this.viaNodeId == tr.getViaNodeId() && this.osmFromId == tr.getOsmfromId()
					&& this.osmToId == tr.getOsmtoId() && this.osmViaId == tr.getOsmViaId())
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
		sb.append(String.valueOf(this.osmFromId));
		sb.append(String.valueOf(this.osmToId));
		sb.append(String.valueOf(this.osmViaId));
		return sb.toString().hashCode();
	}

}
