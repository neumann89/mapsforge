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
package org.mapsforge.routing.graph.creation.extraction;

import org.mapsforge.routing.GeoCoordinate;

import java.util.HashSet;

/**
 * Class for nodes which can belong to edges
 * 
 * @author rob
 * 
 */
public class CompleteNode {

	long id;
	GeoCoordinate coordinate;
	HashSet<KeyValuePair> additionalTags;

	/**
	 * The constructor for the completeVertex
	 * 
	 * the OSM-id
	 * 
	 * @param osmId
	 *            osm ID
	 * 
	 * @param coordinate
	 *            the geo-coordinate
	 * @param additionalTags
	 *            the hashset for additional tags
	 */
	public CompleteNode(long osmId, GeoCoordinate coordinate,
			HashSet<KeyValuePair> additionalTags) {
		this.id = osmId;
		this.coordinate = coordinate;
		this.additionalTags = additionalTags;
	}

	@Override
	public String toString() {
		String s = "[Node " + this.id + "";
		s += "(" + coordinate.getLatitude() + "," + coordinate.getLongitude() + ")";

		s += " TAGS ";
		for (KeyValuePair kv : this.additionalTags) {
			s += kv.toString() + ", ";
		}
		s += " ]";
		return s;
	}

	/**
	 * @return Unique identifier within the scope of the routing graph.
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return Returns the geographic position of this vertex.
	 */
	public GeoCoordinate getCoordinate() {
		return coordinate;
	}

	/**
	 * @return Returns all tags
	 */
	public HashSet<KeyValuePair> getAdditionalTags() {
		return additionalTags;
	}

	@Override
	public int hashCode() {
		return new Long(id).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CompleteNode) {
			CompleteNode o = (CompleteNode) obj;
			if (this.getId() == o.getId() && this.getCoordinate().equals(o.getCoordinate())
					&& ((this.getAdditionalTags() == null && o.getAdditionalTags() == null))
					|| this.getAdditionalTags().equals(o.getAdditionalTags())) {
				return true;
			}
		}
		return false;
	}
}
