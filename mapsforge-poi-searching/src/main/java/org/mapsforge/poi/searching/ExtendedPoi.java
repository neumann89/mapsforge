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
package org.mapsforge.poi.searching;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.routing.Vertex;
import org.mapsforge.storage.poi.PointOfInterest;

/**
 * Wrapper which contains a poi, his nearest graph vertices to itself and to the
 * route, and also the distance to the route.
 */
public class ExtendedPoi implements Comparable<ExtendedPoi> {
	private PointOfInterest poi;
	private double distance = -1;
	private Vertex nearestVertex;
	private Vertex nearestRouteVertex;

	public ExtendedPoi(PointOfInterest poi, double distance,
			Vertex nearestVertex, Vertex nearestRouteVertex) {
		this.poi = poi;
		this.distance = distance;
		this.nearestVertex = nearestVertex;
		this.nearestRouteVertex = nearestRouteVertex;
	}

	public GeoPoint getPosition() {
		return new GeoPoint(poi.getLatitude(), poi.getLongitude());
	}

	public PointOfInterest getPoi() {
		return poi;
	}

	public double getDistance() {
		return distance;
	}

	public Vertex getNearestVertex() {
		return this.nearestVertex;
	}

	public Vertex getNearestRouteVertex() {
		return this.nearestRouteVertex;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public void setNearestVertex(Vertex v) {
		this.nearestVertex = v;
	}

	public void setNearestRouteVertex(Vertex vertex) {
		this.nearestRouteVertex = vertex;
	}

	public int compareTo(ExtendedPoi anotherPoi) {
		if (this.distance < anotherPoi.getDistance())
			return -1;
		else if (this.distance > anotherPoi.getDistance())
			return 1;
		else
			return 0;
	}

	@Override
	public boolean equals(Object another) {
		if (this == another) {
			return true;
		} else if (!(another instanceof ExtendedPoi)) {
			return false;
		}
		ExtendedPoi anotherPoi = (ExtendedPoi) another;

		// TODO PointOfInterest does not support the equals method
		// only test of same position in this case possible
		GeoPoint thisPosition = getPosition();
		GeoPoint otherPosition = anotherPoi.getPosition();
		return thisPosition.equals(otherPosition);
	}

	@Override
	public int hashCode() {
		return getPosition().hashCode();
	}

	public String toString() {
		return poi.toString() + "\nDistance: " + distance + "m";
	}

}
