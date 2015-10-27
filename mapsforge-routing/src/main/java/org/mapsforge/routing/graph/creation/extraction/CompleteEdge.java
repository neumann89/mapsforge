/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.routing.graph.creation.extraction;

import gnu.trove.set.hash.THashSet;
import org.mapsforge.routing.GeoCoordinate;

import java.util.ArrayList;
import java.util.Set;

/**
 * An edge filled with maximal Data from OpenStreetMap.
 * 
 * @author Michael Bartel, Robert Fels
 * 
 */
public class CompleteEdge {

	long id;
	int sourceId;
	int targetId;
	GeoCoordinate[] allWaypoints;
	String name;
	String type;
	boolean roundabout;
	boolean isOneWay;
	String ref;
	String destination;
	int weight;
	THashSet<KeyValuePair> additionalTags;
	ArrayList<CompleteNode> allUsedNodes;

	/**
	 * The Constructor to create a CompleteEdge-instance
	 * 
	 * @param id
	 *            the OSM-ID
	 * @param source
	 *            The source-vertex of the edge.
	 * @param target
	 *            The target-vertex of the edge.
	 * @param allWaypoints
	 *            The waypoints including source and target.
	 * @param name
	 *            The name of the street.
	 * @param type
	 *            The type of the street.
	 * @param roundabout
	 *            Is this way a roundabout.
	 * @param isOneWay
	 *            Is the street oneway?
	 * @param ref
	 *            This reference means another description of a street e.g "B15".
	 * @param destination
	 *            The destination of motor-links e.g. "Leipzig München".
	 * @param weight
	 *            The weight for routing.
	 * @param additionalTags
	 *            the additional Tags that exist for this way
	 * @param allUsedNodes
	 *            All nodes with id, tag and coordinate.
	 */
	public CompleteEdge(long id, int source, int target,
			GeoCoordinate[] allWaypoints, String name, String type, boolean roundabout,
			boolean isOneWay, String ref,
			String destination, int weight, THashSet<KeyValuePair> additionalTags,
			ArrayList<CompleteNode> allUsedNodes) {
		super();
		this.id = id;
		this.sourceId = source;
		this.targetId = target;
		this.allWaypoints = allWaypoints;
		this.name = name;
		this.type = type;
		this.roundabout = roundabout;
		this.isOneWay = isOneWay;
		this.ref = ref;
		this.destination = destination;
		this.weight = weight;
		this.additionalTags = additionalTags;
		this.allUsedNodes = allUsedNodes;
	}

	/**
	 * set one way member
	 * 
	 * @param isOneWay
	 *            bool
	 */
	public void setOneWay(boolean isOneWay) {
		this.isOneWay = isOneWay;
	}

	/**
	 * Adds a new additional Tag key/value pair to this way
	 * 
	 * @param sp
	 *            the new pair to be added
	 */
	public void addAdditionalTags(KeyValuePair sp) {
		this.additionalTags.add(sp);
	}

	/**
	 * @return id of this edge.
	 */
	public long getId() {
		return id;
	}

	/**
	 * return Id of the source vertex
	 * 
	 * @return Id
	 */
	public int getSourceId() {
		return sourceId;
	}

	/**
	 * return Id of the target vertex
	 * 
	 * @return Id
	 */
	public int getTargetId() {
		return targetId;
	}

	/**
	 * 
	 * @return all geocoordinates of the waypoints of the edge
	 */
	public GeoCoordinate[] getWaypoints() {
		GeoCoordinate[] wp = new GeoCoordinate[allWaypoints.length - 2];
		System.arraycopy(allWaypoints, 1, wp, 0, allWaypoints.length - 2);
		return wp;
	}

	/**
	 * @return all Waypoints of the Edge (including source and target)
	 */
	public GeoCoordinate[] getAllWaypoints() {
		return allWaypoints;
	}

	/**
	 * 
	 * @return the name of the street(edge)
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return waytype of the edge
	 */
	public String getType() {
		return type;
	}

	/**
	 * 
	 * @return true if it is an roundabout
	 */
	public boolean isRoundabout() {
		return roundabout;
	}

	/**
	 * @return Returns the Ref of the street, this can be names of the streets which are higher within
	 *         the naming hierarchy, e.g. names of motorways.
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * @return Returns the Destination of the street, usually only available on motorways
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * @return The weight of this edge representing the costs to travel along this edge.
	 */
	public int getWeight() {
		return weight;
	}

	/**
	 * This function calculates the weight of an edge regarding to the chosen metric
	 * 
	 * @param weight
	 *            the weight
	 * 
	 * @return weight value as an int
	 */
	public int setWeight(double weight) {
		this.weight = (int) Math.rint(weight);
		return this.weight;
	}

	/**
	 * Returns the additional tags for this way
	 * 
	 * @return The set of restrictions for this way
	 */
	public Set<KeyValuePair> getAdditionalTags() {
		return additionalTags;
	}

	/**
	 * Returns the tagged and used nodes of a way
	 * 
	 * @return The set of nodes
	 */
	public ArrayList<CompleteNode> getAllUsedNodes() {
		return allUsedNodes;
	}

	/**
	 * Returns true if the street is a oneway street
	 * 
	 * @return true if the street is a oneway street
	 */
	public boolean isOneWay() {
		return isOneWay;
	}

	/**
	 * Sets the source vertex
	 * 
	 * @param sourceId
	 *            the vertex that represents the source
	 */
	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Sets the target vertex
	 * 
	 * @param targetId
	 *            the vertex that represents the target
	 */
	public void setTargetId(int targetId) {
		this.targetId = targetId;
	}

	/**
	 * Sets the waypoints of this edge
	 * 
	 * @param allWaypoints
	 *            the new waypoints to be set
	 */
	public void setAllWaypoints(GeoCoordinate[] allWaypoints) {
		this.allWaypoints = allWaypoints;
	}

	/**
	 * Sets the type of this edge
	 * 
	 * @param type
	 *            the new type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * sets the weight for this edge
	 * 
	 * @param weight
	 *            the new weight
	 */
	public void setWeight(int weight) {
		this.weight = weight;
	}

	/**
	 * Sets the used nodes of this edge
	 * 
	 * @param allUsedNodes
	 *            the hashset of new nodes
	 */
	public void setAllUsedNodes(ArrayList<CompleteNode> allUsedNodes) {
		this.allUsedNodes = allUsedNodes;
	}

	@Override
	public String toString() {
		String s = "[Way " + this.id;
		s += " source-ID: " + this.sourceId;
		s += " target-ID: " + this.targetId;
		s += " weight: " + this.weight;
		s += " type: " + this.type;
		s += " WAYPOINTS ";
		for (GeoCoordinate geo : this.allWaypoints)
			s += geo.getLatitude() + " " + geo.getLongitude() + ", ";
		s += " TAGS ";
		for (KeyValuePair kv : this.additionalTags) {
			s += kv.toString() + ", ";
		}
		s += " Nodes ";
		for (CompleteNode node : this.allUsedNodes) {
			s += node.toString() + ", ";
		}
		s += "]";
		return s;
	}

}
