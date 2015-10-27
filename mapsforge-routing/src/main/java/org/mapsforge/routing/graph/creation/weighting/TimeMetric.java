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
package org.mapsforge.routing.graph.creation.weighting;

import gnu.trove.map.hash.TObjectShortHashMap;
import gnu.trove.set.hash.THashSet;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.graph.creation.extraction.CompleteEdge;
import org.mapsforge.routing.graph.creation.extraction.CompleteNode;
import org.mapsforge.routing.graph.creation.extraction.ConfigObject;
import org.mapsforge.routing.graph.creation.extraction.KeyValuePair;

import java.util.Iterator;

/**
 * This class is responsible to calculate the time costs for a specific edge all speed related
 * statements are calculated with m/s units and distance related statements in m
 * 
 * @author Robert Fels
 */
public class TimeMetric implements IWeightMetric {

	// TODO: single classes metrics for bicycle, foot and others
	int vehicleMaxspeed;
	TObjectShortHashMap<KeyValuePair> waytypeVelocities;
	TObjectShortHashMap<KeyValuePair> stopNodeTimes;
	TObjectShortHashMap<KeyValuePair> stopWayTimes;
	TObjectShortHashMap<KeyValuePair> speedReductNodes;
	TObjectShortHashMap<KeyValuePair> speedReductWays;

	THashSet<KeyValuePair> junctionNodes;
	int junctionDistance;
	int barrierDistance;

	// actually not useful
	THashSet<KeyValuePair> speedReductDynWays;

	/**
	 * @param confObj
	 *            configuration object with all information which is in the xml file
	 */
	public TimeMetric(ConfigObject confObj) {
		this.vehicleMaxspeed = confObj.getVehicleMaxspeed();
		this.waytypeVelocities = confObj.getWaytypeVelocities();
		this.stopNodeTimes = confObj.getStopNodeTimes();
		this.stopWayTimes = confObj.getStopWayTimes();
		this.speedReductNodes = confObj.getSpeedReductNodes();
		this.speedReductWays = confObj.getSpeedReductWays();
		this.speedReductDynWays = confObj.getSpeedReductDynWays();
		this.junctionNodes = confObj.getJunctionNodes();
		this.junctionDistance = confObj.getJunctionDistance();
		this.barrierDistance = confObj.getBarrierSpeedDistance();

	}

	@Override
	public double getCostDouble(CompleteEdge ce) {
		double time = 0;
		double maxWaySpeed = 0;
		double waySpeedReduction = Double.MAX_VALUE;

		// variable to substract the distance if needed
		int distanceReduction = 0;
		// get geocoordinates of source and target vertex
		GeoCoordinate[] allWayPointCoord = ce.getAllWaypoints();

		// coordinates of first and last node of edge
		GeoCoordinate sourceGeoCoord = allWayPointCoord[0];
		GeoCoordinate targetGeoCoord = allWayPointCoord[allWayPointCoord.length - 1];

		boolean isOneWay = ce.isOneWay();
		// go throug all way tags
		for (KeyValuePair kvp : ce.getAdditionalTags()) {
			// TODO include incline (doesn't matter for all motorized vehicles)
			// maxspeed options
			if (kvp.equals(new KeyValuePair("none", "maxspeed"))) {
				maxWaySpeed = vehicleMaxspeed / 3.6d;
			} else if (kvp.getKey() == "maxspeed") {
				// exception
				if (!kvp.getValue().equals("signals"))
					maxWaySpeed = Double.parseDouble(kvp.getValue()) / 3.6d;
			}

			// take account of speed reduction way tags
			if (speedReductWays.containsKey(kvp)) {
				if (speedReductWays.get(kvp) < waySpeedReduction) {
					waySpeedReduction = speedReductWays.get(kvp) / 3.6d;
				}
			}

			// way stop times
			if (stopWayTimes.containsKey(kvp)) {
				time += stopWayTimes.get(kvp);
			}
		}

		// no maxspeed tag found, take default speed for way type
		if (maxWaySpeed == 0)
			maxWaySpeed = waytypeVelocities.get(new KeyValuePair(ce.getType(), "highway")) / 3.6d;

		// set wayspeed (considering surface and maxspeed tags and default way type speed)
		double wayspeed = 0;
		if (maxWaySpeed < waySpeedReduction) {
			wayspeed = maxWaySpeed;
		} else
			wayspeed = waySpeedReduction;

		// go through all way points with tags
		int nodeAmount = ce.getAllUsedNodes().size();
		if (nodeAmount != 0) {
			for (Iterator<CompleteNode> it = ce.getAllUsedNodes().iterator(); it.hasNext();) {

				CompleteNode cNode = it.next();
				GeoCoordinate currGeo = cNode.getCoordinate();

				double distToLast = GeoCoordinate.sphericalDistance(currGeo, targetGeoCoord);
				double distToFirst = GeoCoordinate.sphericalDistance(sourceGeoCoord, currGeo);

				boolean equalsSource = cNode.getCoordinate().equals(sourceGeoCoord);
				boolean equalsTarget = cNode.getCoordinate().equals(targetGeoCoord);

				// go through all tags
				for (KeyValuePair kvp : cNode.getAdditionalTags()) {

					// node stop times
					if (stopNodeTimes.containsKey(kvp)) {
						double stopTime = stopNodeTimes.get(kvp);
						// if first or last node so that the nodes are vertices take only half of time
						if (equalsSource || equalsTarget) {
							time += stopTime / 2;
						} else

						// don't add time if the node is at the beginning of an edge and is a junction
						// node
						if (!(isOneWay && junctionNodes.contains(kvp) && distToFirst <= junctionDistance))
							time += stopTime;
					}

					// node speed reductions
					if (speedReductNodes.containsKey(kvp)) {

						// System.out.println("TIME WEIGHT CALCULATION - Wayspeed: " + wayspeed);

						// speed to cross the object(tagged node)
						double speed = speedReductNodes.get(kvp);

						// stick to allowed speed
						if (wayspeed < speed)
							speed = wayspeed;

						int distInfluence = barrierDistance;

						// take half of dist if node is vertex
						if (equalsSource || equalsTarget) {
							distInfluence = barrierDistance / 2;
						}

						// TODO maybe distance doesn't has to be cut of(TESTING)
						// (heuristic cuts of length if additional 15 meters are not on edge length
						// anymore)
						int halfDistInfl = distInfluence / 2;
						if (distToLast < halfDistInfl) {
							distanceReduction += distToLast;
							time += distToLast / (speed / 3.6d);
						} else {
							distanceReduction += halfDistInfl;
							time += halfDistInfl / (speed / 3.6d);
						}
						if (distToFirst < halfDistInfl) {
							distanceReduction += distToFirst;
							time += distToFirst / (speed / 3.6d);
						} else {
							distanceReduction += halfDistInfl;
							time += halfDistInfl / (speed / 3.6d);
						}
					}
				}
			}
		}

		// set distance of way
		double distance = new DistanceMetric().getCostDouble(ce);

		// update distance and take node reductions into account
		distance -= distanceReduction;

		// finish calculating time
		time += (distance / wayspeed);

		return time * 10;
	}

	@Override
	public int getCostInt(CompleteEdge ce) {
		return (int) Math.rint(getCostDouble(ce));
	}

}
