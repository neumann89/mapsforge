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

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectShortHashMap;
import gnu.trove.set.hash.THashSet;

/**
 * This class bundles given Key/Value-Pairs for OSM-Extraction. And is also responsible for the
 * functions which return whether a specific Key-Value-Pair is in a particular set or not.
 * 
 * @author Michael Bartel and Robert Fels
 * 
 */
public class ConfigObject {

	// all tags in the config file
	THashSet<KeyValuePair> wayTagsSet;
	THashSet<KeyValuePair> nodeTagsSet;
	THashSet<KeyValuePair> relationTagsSet;

	// all usable ways in the config file (of the vehicle)
	TIntObjectHashMap<String> wayTypesMap;
	THashSet<KeyValuePair> usableWays;

	// all restriction tags
	THashSet<KeyValuePair> restrictionTagsSet;

	// all information to calculate the time (weight) to cross an edge
	int vehicleMaxspeed;
	TObjectShortHashMap<KeyValuePair> waytypeVelocities;
	TObjectShortHashMap<KeyValuePair> stopNodeTimes;
	TObjectShortHashMap<KeyValuePair> stopWayTimes;
	TObjectShortHashMap<KeyValuePair> speedReductNodes;
	TObjectShortHashMap<KeyValuePair> speedReductWays;
	THashSet<KeyValuePair> speedReductDynWays;

	// all information about the turn restrictions
	THashSet<KeyValuePair> turnRestrictions;

	// all tags which could be before or on a junction
	THashSet<KeyValuePair> junctionNodes;
	// distance to check traffic signs near by a junction
	int junctionDistance;
	// distance to consider barrier speed restrictions
	int barrierSpeedDistance;

	/**
	 * Get distance to check traffic signs near by a junction
	 * 
	 * @return distance as int (in m)
	 */
	public int getJunctionDistance() {
		return junctionDistance;
	}

	/**
	 * Get distance to consider barrier speed restrictions
	 * 
	 * @return distance as int (in m)
	 */
	public int getBarrierSpeedDistance() {
		return barrierSpeedDistance;
	}

	/**
	 * Constructor for Creation
	 * 
	 * @param wt
	 *            - The Set including all needed Key/Value-Pairs of OSM.ways
	 * @param nt
	 *            - The Set including all needed Key/Value-Pairs of OSM.nodes
	 * @param rt
	 *            - The Set including all needed Key/Value-Pairs of OSM.relations
	 * @param wayTypes
	 *            all used way types in a hashmap, e.g. motorway
	 * @param usableWays
	 *            all used ways ids
	 * @param restrTagsSet
	 *            all restriction tags in a set
	 * @param vehicleMaxspeed
	 *            max speed of vehicle
	 * @param waytypeVelocities
	 *            waytypes and appropiate speeds
	 * @param stopNodeTimes
	 *            stop node tags and appropriate stop times
	 * @param stopWayTimes
	 *            sop way tags and appropriate stop times
	 * @param speedReductNodes
	 *            speed reduction node tags and appropriate speed reductions
	 * @param speedReductWays
	 *            speed reduction way tags and appropriate speed reductions
	 * @param speedReductDynWays
	 *            speed reduction way tags and dynamic values
	 * @param turnRestrictions
	 *            all turn restrictions tags
	 * @param junctionNodes
	 *            all junction nodes tags
	 * @param junctionDistance
	 *            distance to check traffic signs near by a junction
	 * @param barrierSpeedDistance
	 *            slow down distance for barriers
	 */
	public ConfigObject(THashSet<KeyValuePair> wt,
			THashSet<KeyValuePair> nt,
			THashSet<KeyValuePair> rt,
			TIntObjectHashMap<String> wayTypes,
			THashSet<KeyValuePair> usableWays,
			THashSet<KeyValuePair> restrTagsSet,
			int vehicleMaxspeed,
			TObjectShortHashMap<KeyValuePair> waytypeVelocities,
			TObjectShortHashMap<KeyValuePair> stopNodeTimes,
			TObjectShortHashMap<KeyValuePair> stopWayTimes,
			TObjectShortHashMap<KeyValuePair> speedReductNodes,
			TObjectShortHashMap<KeyValuePair> speedReductWays,
			THashSet<KeyValuePair> speedReductDynWays,
			THashSet<KeyValuePair> turnRestrictions,
			THashSet<KeyValuePair> junctionNodes,
			int junctionDistance,
			int barrierSpeedDistance) {
		this.wayTagsSet = wt;
		this.nodeTagsSet = nt;
		this.relationTagsSet = rt;
		this.wayTypesMap = wayTypes;
		this.usableWays = usableWays;
		this.restrictionTagsSet = restrTagsSet;
		this.vehicleMaxspeed = vehicleMaxspeed;
		this.waytypeVelocities = waytypeVelocities;
		this.stopNodeTimes = stopNodeTimes;
		this.stopWayTimes = stopWayTimes;
		this.speedReductNodes = speedReductNodes;
		this.speedReductWays = speedReductWays;
		this.speedReductDynWays = speedReductDynWays;
		this.turnRestrictions = turnRestrictions;
		this.junctionNodes = junctionNodes;
		this.junctionDistance = junctionDistance;
		this.barrierSpeedDistance = barrierSpeedDistance;
	}

	/**
	 *
	 * @return getter
	 */
	public THashSet<KeyValuePair> getUsableWays() {
		return usableWays;
	}

	/**
	 * @return getter
	 */
	public THashSet<KeyValuePair> getWayTagsSet() {
		return wayTagsSet;
	}

	/**
	 * @return getter
	 */
	public THashSet<KeyValuePair> getNodeTagsSet() {
		return nodeTagsSet;
	}

	/**
	 * @return getter
	 */
	public THashSet<KeyValuePair> getRelationTagsSet() {
		return relationTagsSet;
	}

	/**
	 * @return getter
	 */
	public TIntObjectHashMap<String> getWayTypesMap() {
		return wayTypesMap;
	}

	/**
	 * @return getter
	 */
	public THashSet<KeyValuePair> getRestrictionTagsSet() {
		return restrictionTagsSet;
	}

	/**
	 * @return getter
	 */
	public int getVehicleMaxspeed() {
		return vehicleMaxspeed;
	}

	/**
	 * @return getter
	 */
	public TObjectShortHashMap<KeyValuePair> getWaytypeVelocities() {
		return waytypeVelocities;
	}

	/**
	 * @return getter
	 */
	public TObjectShortHashMap<KeyValuePair> getStopNodeTimes() {
		return stopNodeTimes;
	}

	/**
	 * @return getter
	 */
	public TObjectShortHashMap<KeyValuePair> getStopWayTimes() {
		return stopWayTimes;
	}

	/**
	 * @return getter
	 */
	public TObjectShortHashMap<KeyValuePair> getSpeedReductNodes() {
		return speedReductNodes;
	}

	/**
	 * @return getter
	 */
	public TObjectShortHashMap<KeyValuePair> getSpeedReductWays() {
		return speedReductWays;
	}

	/**
	 * @return getter
	 */
	public THashSet<KeyValuePair> getSpeedReductDynWays() {
		return speedReductDynWays;
	}

	/**
	 * @return getter
	 */
	public THashSet<KeyValuePair> getTurnRestrictions() {
		return turnRestrictions;
	}

	/**
	 * @return getter
	 */
	public THashSet<KeyValuePair> getJunctionNodes() {
		return junctionNodes;
	}

	/**
	 * Returns true if the key/value pair exists in the corresponding set
	 * 
	 * @param key
	 *            The key to be checked
	 * @param value
	 *            The value to be checked
	 * @return true, if the pair exists
	 */
	public boolean containsWayTag(String key, String value) {
		return (wayTagsSet.contains(new KeyValuePair(value, key)) || wayTagsSet
				.contains(new KeyValuePair(null, key)));
	}

	/**
	 * Returns true if the key/value pair exists in the corresponding set
	 * 
	 * @param key
	 *            The key to be checked
	 * @param value
	 *            The value to be checked
	 * @return true, if the pair exists
	 */
	public boolean containsRestrictionTag(String key, String value) {
		return (restrictionTagsSet.contains(new KeyValuePair(value, key)));
	}

	/**
	 * Returns true if the key/value pair exists in the corresponding set
	 * 
	 * @param key
	 *            The key to be checked
	 * @param value
	 *            The value to be checked
	 * @return true, if the pair exists
	 */
	public boolean containsRelationTag(String key, String value) {
		/*
		 * for (KeyValuePair sp : relationTagsSet) if (sp.value != null) if ((sp.key.equals(key)) &&
		 * sp.value.equals(value)) return true; return false;
		 */
		return (relationTagsSet.contains(new KeyValuePair(value, key)) || relationTagsSet
				.contains(new KeyValuePair(null, key)));
	}

	/**
	 * Returns true if the key/value pair exists in the corresponding set
	 * 
	 * @param key
	 *            The key to be checked
	 * @param value
	 *            The value to be checked
	 * @return true, if the pair exists
	 */
	public boolean containsNodeTag(String key, String value) {
		/*
		 * for (KeyValuePair sp : nodeTagsSet) if (sp.value != null) if ((sp.key.equals(key)) &&
		 * sp.value.equals(value)) return true; return false;
		 */
		return (nodeTagsSet.contains(new KeyValuePair(value, key)) || nodeTagsSet
				.contains(new KeyValuePair(null, key)));
	}

}
