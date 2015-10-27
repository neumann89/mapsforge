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

import org.mapsforge.routing.Edge;
import org.mapsforge.routing.Vertex;

/**
 * Wrapper which contains a vertex with network distance and reference to its
 * shortest path edge.
 */
public class ExtendedVertex implements Comparable<ExtendedVertex> {
	private Vertex vertex;
	private Edge predecessorEdge;
	private ExtendedVertex predecessorVertex;
	private double distance = -1;

	public ExtendedVertex(Vertex vertex, double distance, Edge predecessorEdge,
			ExtendedVertex predecessorVertex) {
		this.vertex = vertex;
		this.predecessorEdge = predecessorEdge;
		this.predecessorVertex = predecessorVertex;
		this.distance = distance;
	}

	public double getDistance() {
		return this.distance;
	}

	public Vertex getVertex() {
		return this.vertex;
	}

	public Edge getPredecessorEdge() {
		return this.predecessorEdge;
	}

	public ExtendedVertex getPredecessorVertex() {
		return this.predecessorVertex;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public void setPredecessorEdge(Edge predecessorEdge) {
		this.predecessorEdge = predecessorEdge;
	}

	public int compareTo(ExtendedVertex anotherVertex) {
		if (this.distance < anotherVertex.getDistance())
			return -1;
		else if (this.distance > anotherVertex.getDistance())
			return 1;
		else
			return 0;
	}

	@Override
	public boolean equals(Object another) {
		if (this == another) {
			return true;
		} else if (!(another instanceof ExtendedVertex)) {
			return false;
		}
		ExtendedVertex anotherVertex = (ExtendedVertex) another;
		return this.vertex.getId() == anotherVertex.getVertex().getId();
	}

	@Override
	public int hashCode() {
		return vertex.getId();
	}

}
