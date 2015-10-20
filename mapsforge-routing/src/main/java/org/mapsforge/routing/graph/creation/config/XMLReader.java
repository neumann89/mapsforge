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
package org.mapsforge.routing.graph.creation.config;

import gnu.trove.iterator.TObjectShortIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectShortHashMap;
import gnu.trove.set.hash.THashSet;
import org.mapsforge.routing.graph.creation.extraction.ConfigObject;
import org.mapsforge.routing.graph.creation.extraction.KeyValuePair;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

/**
 * This class parses a given file (in XML-Format) to determine which tags will be needed for the graph
 * when extracting data from the pbf-file.
 * 
 * @author Michael Bartel and Robert Fels
 * 
 */
public class XMLReader {

	// private boolean isNeededType(String[] neededTypes, String currentType) {
	// if (neededTypes == null)
	// return true;
	// for (String s : neededTypes) {
	// if (s.equals(currentType))
	// return true;
	// }
	// return false;
	// }

	private static KeyValuePair getKeyValue(Node n) {

		// no attributes exist
		if (!(n.hasAttributes()))
			return null;

		String val = null;
		String key = null;

		// Check all attributes for key/value
		for (int j = 0; j < n.getAttributes().getLength(); j++) {
			Attr tmpatt = ((Attr) n.getAttributes().item(j));
			if (tmpatt.getName().equals("v"))
				val = tmpatt.getValue();
			if (tmpatt.getName().equals("k"))
				key = tmpatt.getValue();
		}
		return new KeyValuePair(val, key);
	}

	// private Node getChildNodeByName(NodeList nl, String name) {
	// for (int i = 0; i < nl.getLength(); i++) {
	// if (nl.item(i).getNodeName().equals(name))
	// return nl.item(i);
	// }
	// return null;
	// }

	/**
	 * Put the way tags of the xml config into the particular Set
	 * 
	 * @param doc
	 *            the XML dodument which should be parsed
	 * @param vehicle
	 *            define the vehicle you need (e.g. "motorcar")
	 * @param metric
	 *            define the metric which you want to use (e.g. "fastest-motorcar")
	 * @return Set with all way tags included in the xml file (for the defined metric and vehicle)
	 */
	public static THashSet<KeyValuePair> getWayTags(Document doc, String vehicle, String metric) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		THashSet<KeyValuePair> result = new THashSet<KeyValuePair>();
		try {
			// Check routingTags at the beginning
			XPathExpression xe = xpath
					.compile("/config/routingTags/wayTags");

			// apply xpath expression to the document
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);
				result.add(newWayTag);
			}

			// Check metric

			xe = xpath
					.compile("/config/metrics/weightMetric[@name='" + metric
							+ "']/stopTags/stopWayTags/*"
							+
							"| /config/metrics/weightMetric[@name='" + metric
							+ "']/speedreductions/wayTags/*" +
							"| /config/metrics/weightMetric[@name='" + metric
							+ "']/speedreductions/dynamicWayTags/*"
						);
			// "| /vehicles/vehicle[@name='" + vehicle + "']/restrictions" --> cut that out in osmosis
			nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);
				result.add(newWayTag);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Put the node tags of the xml config into the particular Set
	 * 
	 * @param doc
	 *            the XML dodument which should be parsed
	 * @param vehicle
	 *            define the vehicle you need (e.g. "motorcar")
	 * @param metric
	 *            define the metric which you want to use (e.g. "fastest-motorcar")
	 * @return Set with all node tags included in the xml file (for the defined metric and vehicle)
	 */
	public static THashSet<KeyValuePair> getNodeTags(Document doc, String vehicle, String metric) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		THashSet<KeyValuePair> result = new THashSet<KeyValuePair>();
		try {
			// Check routingTags at the beginning
			XPathExpression xe = xpath
					.compile("/config/routingTags/nodeTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);
				result.add(newWayTag);
			}

			// Check metric

			xe = xpath
					.compile("/config/metrics/weightMetric[@name='" + metric
							+ "']/stopTags/stopNodeTags/*" +
							"| /config/metrics/weightMetric[@name='" + metric
							+ "']/speedreductions/nodeTags/*"
							);
			nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newNodeTag = getKeyValue(node);
				result.add(newNodeTag);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Put the relation tags of the xml config into the particular Set
	 * 
	 * @param doc
	 *            the XML dodument which should be parsed
	 * @param vehicle
	 *            define the vehicle you need (e.g. "motorcar")
	 * @param metric
	 *            define the metric which you want to use (e.g. "fastest-motorcar")
	 * @return Set with all relation tags included in the xml file (for the defined metric and vehicle)
	 */
	public static THashSet<KeyValuePair> getRelationTags(Document doc, String vehicle, String metric) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		THashSet<KeyValuePair> result = new THashSet<KeyValuePair>();
		try {
			// Check routingTags at the beginning
			XPathExpression xe = xpath
					.compile("/config/routingTags/relationTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);
				result.add(newWayTag);
			}

			// Check vehicle
			xe = xpath.compile("/config/vehicles/vehicle[@name='" + vehicle
					+ "']/restrictions/relationTags/*");
			nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);
				result.add(newWayTag);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns all usable ways in a hash map, mapped from an integer
	 * 
	 * @param doc
	 *            document
	 * @param vehicle
	 *            defined vehicle
	 * @return hash map
	 */
	public static TIntObjectHashMap<String> getWayTypes(Document doc, String vehicle) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		TIntObjectHashMap<String> result = new TIntObjectHashMap<String>();
		try {
			// Check vehicle
			XPathExpression xe = xpath.compile("/config/vehicles/vehicle[@name='" + vehicle
					+ "']/usableWayTags/*");
			// "| /config/vehicles/vehicle[@name='" + vehicle + "']/restrictions/wayTags/*" --> cut that
			// out in osmosis
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);
				result.put(i, newWayTag.getValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns all usable ways in a hash map, mapped from an integer
	 * 
	 * @param doc
	 *            document
	 * @param vehicle
	 *            defined vehicle
	 * @return hash map
	 */
	public static THashSet<KeyValuePair> getUsableWays(Document doc, String vehicle) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		THashSet<KeyValuePair> result = new THashSet<KeyValuePair>();
		try {
			// Check vehicle
			XPathExpression xe = xpath.compile("/config/vehicles/vehicle[@name='" + vehicle
					+ "']/usableWayTags/*");
			// "| /config/vehicles/vehicle[@name='" + vehicle + "']/restrictions/wayTags/*" --> cut that
			// out in osmosis
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);
				result.add(newWayTag);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns all restricted way tags
	 * 
	 * @param doc
	 *            document
	 * @param vehicle
	 *            defined vehicle
	 * @return hash set
	 */
	public static THashSet<KeyValuePair> getRestrictedWays(Document doc, String vehicle) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		THashSet<KeyValuePair> result = new THashSet<KeyValuePair>();
		try {
			// Check vehicle
			XPathExpression xe = xpath.compile("/config/vehicles/vehicle[@name='" + vehicle
					+ "']/restrictions/wayTags/*");
			// "| /config/vehicles/vehicle[@name='" + vehicle + "']/restrictions/wayTags/*" --> cut that
			// out in osmosis
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);
				result.add(newWayTag);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * The method to parse the XML config file
	 * 
	 * @param file
	 *            the config to define the parsing
	 * @param vehicle
	 *            A String that contains the needed vehicle type e.g. {"motorcar", "foot"} default (if
	 *            null) is "motorcar"
	 * @param metric
	 *            the metric from the xml config file which should be used
	 * @return an Instance of ConfigObject which contains all necessary data
	 */
	public static ConfigObject parseXMLFromFile(String file, String vehicle, String metric) {

		String s = "[RGC - XML processing] XML-Parse started with File: " + file;

		System.out.println(s);
		System.out.println("[RGC - XML processing] vehicle: " + vehicle);
		System.out.println("[RGC - XML processing] metric: " + metric);

		// generally usefull Tag sets
		THashSet<KeyValuePair> wayTagsSet;
		THashSet<KeyValuePair> nodeTagsSet;
		THashSet<KeyValuePair> relationTagsSet;
		TIntObjectHashMap<String> wayTypes;
		THashSet<KeyValuePair> usableWays;
		THashSet<KeyValuePair> restrWayTagsSet;

		// all for the calculation of time weight
		int vehicleMaxspeed;
		TObjectShortHashMap<KeyValuePair> waytypeVelocities;
		TObjectShortHashMap<KeyValuePair> stopNodeTimes;
		TObjectShortHashMap<KeyValuePair> stopWayTimes;
		TObjectShortHashMap<KeyValuePair> speedReductNodes;
		TObjectShortHashMap<KeyValuePair> speedReductWays;
		THashSet<KeyValuePair> speedReductDynWays;

		// all for the calculations of the turn restrictoins
		THashSet<KeyValuePair> turnRestrictions;

		THashSet<KeyValuePair> junctionNodes;
		// initialize all
		Document doc = getXmlDocument(file);
		doc.getDocumentElement().normalize();

		wayTagsSet = getWayTags(doc, vehicle, metric);
		nodeTagsSet = getNodeTags(doc, vehicle, metric);
		relationTagsSet = getRelationTags(doc, vehicle, metric);
		wayTypes = getWayTypes(doc, vehicle);
		usableWays = getUsableWays(doc, vehicle);

		restrWayTagsSet = getRestrictedWays(doc, vehicle);

		vehicleMaxspeed = getMaxSpeedbyVehicle(doc, vehicle);
		waytypeVelocities = getUsableWaysVelocities(doc, vehicle);
		stopNodeTimes = getStopNodeTags(doc, metric);
		stopWayTimes = getStopWayTags(doc, metric);
		speedReductNodes = getSpeedRedNodeTags(doc, metric);
		speedReductWays = getSpeedRedWayTags(doc, metric);
		speedReductDynWays = getSpeedRedDynWayTags(doc, metric);
		turnRestrictions = getTurnRestrictions(doc, vehicle);
		junctionNodes = getJunctionNodeTags(doc, metric);

		HashMap<String, String> noOSMTags = getNoOSMTags(doc, metric);
		int junctionSignDistance = Integer.valueOf(noOSMTags.get("crossSignDistance"));
		int barrierSpeedDistance = Integer.valueOf(noOSMTags.get("barrierSpeedDistance"));

		System.out.println("[RGC - XML processing] finished...");
		System.out.println("[RGC - XML processing] vehicle processed: " + vehicle + " maxspeed: "
				+ vehicleMaxspeed);
		System.out.println("[RGC - XML processing] metric processed: " + metric);
		System.out.println("[RGC - XML processing] wayTags found: " + wayTagsSet.size());
		System.out.println("[RGC - XML processing] nodeTags found: " + nodeTagsSet.size());
		System.out.println("[RGC - XML processing] relationsTags found: " + relationTagsSet.size());
		System.out.println("[RGC - XML processing] usable way tags found: " + wayTypes.size());
		System.out.println("[RGC - XML processing] restricted way tags found: "
				+ restrWayTagsSet.size());
		System.out.println("[RGC - XML processing] way type velocities found: "
				+ waytypeVelocities.size());
		System.out.println("[RGC - XML processing] stop nodes times found: " + stopNodeTimes.size());
		System.out.println("[RGC - XML processing] stop way times found found: "
				+ stopWayTimes.size());
		System.out.println("[RGC - XML processing] node speed reductions found: "
				+ speedReductNodes.size());
		System.out.println("[RGC - XML processing] way speed reductions found: "
				+ speedReductWays.size());
		System.out.println("[RGC - XML processing] dynamic way tags found: "
				+ speedReductDynWays.size());
		System.out.println("[RGC - XML processing] turn restriction tags found: "
				+ turnRestrictions.size());

		return new ConfigObject(wayTagsSet, nodeTagsSet, relationTagsSet, wayTypes, usableWays,
				restrWayTagsSet,
				vehicleMaxspeed, waytypeVelocities, stopNodeTimes, stopWayTimes, speedReductNodes,
				speedReductWays, speedReductDynWays, turnRestrictions, junctionNodes,
				junctionSignDistance, barrierSpeedDistance);
	}

	/**
	 * The method to parse the XML config file
	 * 
	 * @param is
	 *            input stream
	 * 
	 * @param vehicle
	 *            A String that contains the needed vehicle type e.g. {"motorcar", "foot"} default (if
	 *            null) is "motorcar"
	 * @param metric
	 *            the metric from the xml config file which should be used
	 * @return an Instance of ConfigObject which contains all necessary data
	 */
	public static ConfigObject parseXMLFromStream(InputStream is, String vehicle, String metric) {

		String s = "[RGC - XML processing] XML-Parse started from file stream";

		System.out.println(s);
		System.out.println("[RGC - XML processing] vehicle: " + vehicle);
		System.out.println("[RGC - XML processing] metric: " + metric);

		// generally usefull Tag sets
		THashSet<KeyValuePair> wayTagsSet;
		THashSet<KeyValuePair> nodeTagsSet;
		THashSet<KeyValuePair> relationTagsSet;
		TIntObjectHashMap<String> wayTypes;
		THashSet<KeyValuePair> usableWays;
		THashSet<KeyValuePair> restrWayTagsSet;

		// all for the calculation of time weight
		int vehicleMaxspeed;
		TObjectShortHashMap<KeyValuePair> waytypeVelocities;
		TObjectShortHashMap<KeyValuePair> stopNodeTimes;
		TObjectShortHashMap<KeyValuePair> stopWayTimes;
		TObjectShortHashMap<KeyValuePair> speedReductNodes;
		TObjectShortHashMap<KeyValuePair> speedReductWays;
		THashSet<KeyValuePair> speedReductDynWays;

		// all for the calculations of the turn restrictoins
		THashSet<KeyValuePair> turnRestrictions;

		THashSet<KeyValuePair> junctionNodes;
		// initialize all
		Document doc = getXmlDocument(is);
		doc.getDocumentElement().normalize();

		wayTagsSet = getWayTags(doc, vehicle, metric);
		nodeTagsSet = getNodeTags(doc, vehicle, metric);
		relationTagsSet = getRelationTags(doc, vehicle, metric);
		wayTypes = getWayTypes(doc, vehicle);
		usableWays = getUsableWays(doc, vehicle);
		restrWayTagsSet = getRestrictedWays(doc, vehicle);

		vehicleMaxspeed = getMaxSpeedbyVehicle(doc, vehicle);
		waytypeVelocities = getUsableWaysVelocities(doc, vehicle);
		stopNodeTimes = getStopNodeTags(doc, metric);
		stopWayTimes = getStopWayTags(doc, metric);
		speedReductNodes = getSpeedRedNodeTags(doc, metric);
		speedReductWays = getSpeedRedWayTags(doc, metric);
		speedReductDynWays = getSpeedRedDynWayTags(doc, metric);
		turnRestrictions = getTurnRestrictions(doc, vehicle);
		junctionNodes = getJunctionNodeTags(doc, metric);

		HashMap<String, String> noOSMTags = getNoOSMTags(doc, metric);
		int junctionSignDistance = Integer.valueOf(noOSMTags.get("crossSignDistance"));
		int barrierSpeedDistance = Integer.valueOf(noOSMTags.get("barrierSpeedDistance"));

		System.out.println("[RGC - XML processing] finished...");
		System.out.println("[RGC - XML processing] vehicle processed: " + vehicle + " maxspeed: "
				+ vehicleMaxspeed);
		System.out.println("[RGC - XML processing] metric processed: " + metric);
		System.out.println("[RGC - XML processing] wayTags found: " + wayTagsSet.size());
		System.out.println("[RGC - XML processing] nodeTags found: " + nodeTagsSet.size());
		System.out.println("[RGC - XML processing] relationsTags found: " + relationTagsSet.size());
		System.out.println("[RGC - XML processing] usable way tags found: " + wayTypes.size());
		System.out.println("[RGC - XML processing] restricted way tags found: "
				+ restrWayTagsSet.size());
		System.out.println("[RGC - XML processing] way type velocities found: "
				+ waytypeVelocities.size());
		System.out.println("[RGC - XML processing] stop nodes times found: " + stopNodeTimes.size());
		System.out.println("[RGC - XML processing] stop way times found found: "
				+ stopWayTimes.size());
		System.out.println("[RGC - XML processing] node speed reductions found: "
				+ speedReductNodes.size());
		System.out.println("[RGC - XML processing] way speed reductions found: "
				+ speedReductWays.size());
		System.out.println("[RGC - XML processing] dynamic way tags found: "
				+ speedReductDynWays.size());
		System.out.println("[RGC - XML processing] turn restriction tags found: "
				+ turnRestrictions.size());

		return new ConfigObject(wayTagsSet, nodeTagsSet, relationTagsSet, wayTypes, usableWays,
				restrWayTagsSet,
				vehicleMaxspeed, waytypeVelocities, stopNodeTimes, stopWayTimes, speedReductNodes,
				speedReductWays, speedReductDynWays, turnRestrictions, junctionNodes,
				junctionSignDistance, barrierSpeedDistance);
	}

	/**
	 * @param doc
	 *            xml doc
	 * @param vehicle
	 *            vehicle type you want
	 * @return maxspeed
	 */
	public static Integer getMaxSpeedbyVehicle(Document doc, String vehicle) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath
					.compile("/config/vehicles/vehicle[@name='" + vehicle
							+ "']/@maxspeed");

			Node node = (Node) xe.evaluate(doc, XPathConstants.NODE);
			return Integer.parseInt(node.getTextContent());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * @param doc
	 *            xml doc
	 * @param vehicle
	 *            vehicle type you want
	 * @return a list of tags represented as XML-Nodes
	 */
	public static TObjectShortHashMap<KeyValuePair> getUsableWaysVelocities(Document doc, String vehicle) {
		TObjectShortHashMap<KeyValuePair> result = new TObjectShortHashMap<KeyValuePair>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("/config/vehicles/vehicle[@name='" + vehicle
					+ "']/usableWayTags/*");

			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);

				result.put(newWayTag,
						Short.parseShort(node.getAttributes().getNamedItem("maxspeed").getTextContent()));
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param doc
	 *            xml doc
	 * @return a list of tags represented as XML-Nodes
	 */
	public static THashSet<KeyValuePair> getTurnRestrictions(Document doc, String vehicle) {
		THashSet<KeyValuePair> result = new THashSet<KeyValuePair>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("/config/vehicles/vehicle[@name='" + vehicle
					+ "']/restrictions/relationTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair kvp = getKeyValue(node);
				result.add(kvp);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param metric
	 *            metric type you want
	 * @param doc
	 *            xml doc
	 * 
	 * @return a list of tags represented as XML-Nodes
	 */
	public static TObjectShortHashMap<KeyValuePair> getStopNodeTags(Document doc, String metric) {
		TObjectShortHashMap<KeyValuePair> result = new TObjectShortHashMap<KeyValuePair>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath
					.compile("/config/metrics/weightMetric[@name='" + metric
							+ "']/stopTags/stopNodeTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);

				result.put(newWayTag,
						Short.parseShort(node.getAttributes().getNamedItem("time").getTextContent()));
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param metric
	 *            metric type you want
	 * @param doc
	 *            xml doc
	 * 
	 * @return a list of tags represented as XML-Nodes
	 */
	public static TObjectShortHashMap<KeyValuePair> getStopWayTags(Document doc, String metric) {
		TObjectShortHashMap<KeyValuePair> result = new TObjectShortHashMap<KeyValuePair>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath
					.compile("/config/metrics/weightMetric[@name='" + metric
							+ "']/stopTags/stopWayTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);

				result.put(newWayTag,
						Short.parseShort(node.getAttributes().getNamedItem("time").getTextContent()));
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param metric
	 *            metric type you want
	 * @param doc
	 *            xml doc
	 * 
	 * @return a list of tags represented as XML-Nodes
	 */
	public static TObjectShortHashMap<KeyValuePair> getSpeedRedWayTags(Document doc, String metric) {
		TObjectShortHashMap<KeyValuePair> result = new TObjectShortHashMap<KeyValuePair>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath
					.compile("/config/metrics/weightMetric[@name='" + metric
							+ "']/speedreductions/wayTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);

				result.put(
						newWayTag,
						Short.parseShort(node.getAttributes().getNamedItem("maxspeed").getTextContent()));
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param metric
	 *            metric type you want
	 * @param doc
	 *            xml doc
	 * 
	 * @return a list of tags represented as XML-Nodes
	 */
	public static TObjectShortHashMap<KeyValuePair> getSpeedRedNodeTags(Document doc, String metric) {
		TObjectShortHashMap<KeyValuePair> result = new TObjectShortHashMap<KeyValuePair>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath
					.compile("/config/metrics/weightMetric[@name='" + metric
							+ "']/speedreductions/nodeTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);

			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);

				result.put(
						newWayTag,
						Short.parseShort(node.getAttributes().getNamedItem("maxspeed").getTextContent()));
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param metric
	 *            metric type you want
	 * @param doc
	 *            xml doc
	 * 
	 * @return a list of tags represented as XML-Nodes
	 */
	public static THashSet<KeyValuePair> getSpeedRedDynWayTags(Document doc, String metric) {
		THashSet<KeyValuePair> result = new THashSet<KeyValuePair>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath
					.compile("/config/metrics/weightMetric[@name='" + metric
							+ "']/speedreductions/dynamicWayTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			// add to set
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair newWayTag = getKeyValue(node);

				result.add(newWayTag);
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param metric
	 *            metric type you want
	 * @param doc
	 *            xml doc
	 * @return a list of tags represented as XML-Nodes
	 */
	public static HashMap<String, String> getNoOSMTags(Document doc, String metric) {

		HashMap<String, String> result = new HashMap<String, String>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("/config/metrics/weightMetric[@name='" + metric
							+ "']/noOSMTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair kvp = getKeyValue(node);
				result.put(kvp.getKey(), kvp.getValue());
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param metric
	 *            metric type you want
	 * @param doc
	 *            xml doc
	 * @return a list of tags represented as XML-Nodes
	 */
	public static THashSet<KeyValuePair> getJunctionNodeTags(Document doc, String metric) {

		THashSet<KeyValuePair> result = new THashSet<KeyValuePair>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("/config/metrics/weightMetric[@name='" + metric
					+ "']/stopTags/junctionNodeTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < nList.getLength(); i++) {
				Node node = nList.item(i);
				KeyValuePair kvp = getKeyValue(node);
				result.add(kvp);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * 
	 * @param xmlFilePath
	 *            file path of xml config file
	 * @return return the xml document to modify or just read it
	 */
	public static Document getXmlDocument(String xmlFilePath) {
		try {
			File xmlFile = new File(xmlFilePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * 
	 * @param is
	 *            input stream of xml file
	 * @return return the xml document to modify or just read it
	 */
	public static Document getXmlDocument(InputStream is) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param hm
	 *            hashmap with kvp and another value
	 * @return String with all nodes in readable format
	 */
	public static String keyValAndAttributetoString(TObjectShortHashMap<KeyValuePair> hm) {
		String result = null;
		if (hm != null) {
			for (TObjectShortIterator<KeyValuePair> it = hm.iterator(); it.hasNext();) {
				it.advance();
				KeyValuePair kvp = it.key();
				short attr = it.value();
				String line;
				line = "<" + kvp + " attr: " + attr + "/>";
				if (result == null)
					result = line + "\n";
				else
					result += line + "\n";
			}

		} else
			System.out.println("Hash map ist null!");
		return result;

	}

}
