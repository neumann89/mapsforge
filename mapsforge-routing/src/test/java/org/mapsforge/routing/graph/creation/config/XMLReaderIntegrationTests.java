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
package org.mapsforge.routing.graph.creation.config;

import gnu.trove.map.hash.TObjectShortHashMap;
import gnu.trove.set.hash.THashSet;
import org.junit.Ignore;
import org.junit.Test;
import org.mapsforge.routing.graph.creation.extraction.KeyValuePair;
import org.w3c.dom.Document;

import java.util.HashMap;

/**
 * Integration tests for {@link XMLReader}.
 * 
 * @author Robert Fels
 * 
 */
public class XMLReaderIntegrationTests {

    // TODO: refactoring
    @Ignore("Path to config.xml is invalid and not environment independent, should be provided as test resource.")
	@Test
	public void testXmlFetching() {
		// TODO: make path relative / make the test independent
		String path = "/media/sda6/Uni/8.Semester/cleanBA/workspace/mapsforge/src/org/mapsforge/preprocessing/routingGraph/graphCreation/config/config.xml";
		Document doc = XMLReader.getXmlDocument(path);
		String vehicle = "motorcar";
		String metric = "fastest-motorcar";

		int speed = XMLReader.getMaxSpeedbyVehicle(doc, vehicle);
		System.out.println(String.format("maxspeed:\n %s", "------------"));
		System.out.println(speed);

		TObjectShortHashMap<KeyValuePair> usableWays = XMLReader.getUsableWaysVelocities(doc,
				vehicle);
		System.out.println(String.format("all usable Ways:\n %s", "------------"));
		System.out.println(XMLReader.keyValAndAttributetoString(usableWays));

		THashSet<KeyValuePair> useWays = XMLReader.getRestrictedWays(doc,
				vehicle);
		System.out.println(String.format("all USABLE WAYS:\n %s", "------------"));
		for (KeyValuePair keyValuePair : useWays) {
			System.out.println(keyValuePair);
		}

		THashSet<KeyValuePair> wayRestrictions = XMLReader.getRestrictedWays(doc,
				vehicle);
		System.out.println(String.format("all way restrictions:\n %s", "------------"));
		for (KeyValuePair keyValuePair : wayRestrictions) {
			System.out.println(keyValuePair);
		}

		THashSet<KeyValuePair> turnRestrictions = XMLReader.getTurnRestrictions(doc, vehicle);
		System.out.println(String.format("all relation restrictions:\n %s", "------------"));
		for (KeyValuePair keyValuePair : turnRestrictions) {
			System.out.println(keyValuePair);
		}

		TObjectShortHashMap<KeyValuePair> stopNodes = XMLReader.getStopNodeTags(doc, metric);
		System.out.println(String.format("all stop nodes:\n %s", "------------"));
		System.out.println(XMLReader.keyValAndAttributetoString(stopNodes));

		TObjectShortHashMap<KeyValuePair> stopWays = XMLReader.getStopWayTags(doc, metric);
		System.out.println(String.format("all stop ways:\n %s", "------------"));
		System.out.println(XMLReader.keyValAndAttributetoString(stopWays));

		TObjectShortHashMap<KeyValuePair> redNodes = XMLReader.getSpeedRedNodeTags(doc, metric);
		System.out.println(String.format("all speed reduction nodes:\n %s", "------------"));
		System.out.println(XMLReader.keyValAndAttributetoString(redNodes));

		TObjectShortHashMap<KeyValuePair> redWays = XMLReader.getSpeedRedWayTags(doc, metric);
		System.out.println(String.format("all spedd reduction ways:\n %s", "------------"));
		System.out.println(XMLReader.keyValAndAttributetoString(redWays));

		THashSet<KeyValuePair> dynWays = XMLReader.getSpeedRedDynWayTags(doc, metric);
		System.out.println(String.format("all dynamic ways:\n %s", "------------"));
		for (KeyValuePair keyValuePair : dynWays) {
			System.out.println(keyValuePair);
		}

		HashMap<String, String> noOsm = XMLReader.getNoOSMTags(doc, metric);
		System.out.println(String.format("all no OSM:\n %s", "------------"));
		for (String s : noOsm.keySet()) {
			System.out.println(s + " " + noOsm.get(s));
		}

		THashSet<KeyValuePair> junctionNodes = XMLReader.getJunctionNodeTags(doc, metric);
		System.out.println(String.format("all before junction nodes:\n %s", "------------"));
		for (KeyValuePair keyValuePair : junctionNodes) {
			System.out.println(keyValuePair);
		}
	}

}
