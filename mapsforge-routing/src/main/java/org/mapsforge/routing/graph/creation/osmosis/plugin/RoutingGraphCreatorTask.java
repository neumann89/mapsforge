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
package org.mapsforge.routing.graph.creation.osmosis.plugin;

import gnu.trove.function.TIntFunction;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.THashSet;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.graph.creation.sql.SqlWriter;
import org.mapsforge.routing.graph.creation.config.XMLReader;
import org.mapsforge.routing.graph.creation.extraction.*;
import org.mapsforge.routing.graph.creation.extraction.turnRestrictions.CompleteTurnRestriction;
import org.mapsforge.routing.graph.creation.extraction.turnRestrictions.RestrictionExtractor;
import org.mapsforge.routing.graph.creation.statistics.ProtobufCreator;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This task writes a sql file (tested for postgresql 8.4) which represents a routing graph and all turn
 * restrictions which shuld be included into that graph. Executing the resulting file bulk-loads the
 * graph to the database.
 * 
 * The task also create a pbf file with all edges and statistical data for the edges inside As parameter
 * this plugin requires the xmlConfigPath, vehicle and metric type out of the config file, the output
 * path for the sql file, true/false for the cration of statistics in a pbf and the path of the pbf.
 * 
 * @author Frank Viernau, Michael Bartel, Robert Fels
 */
class RoutingGraphCreatorTask implements Sink {
	// temp values to identify if node is vertex or way point
	private static final int NODE_TYPE_VERTEX = 2;
	private static final int NODE_TYPE_WAYPOINT = 1;

	// --- amounting ---
	private int amountOfNodesProcessed = 0;
	private int amountOfWaysProcessed = 0;
	private int amountOfRelationsProcessed = 0;
	private int amountOfVerticesWritten = 0;
	private int turnRestrictionsWrittenAmount = 0;

	// counter used to assign ids for vertices
	int numVertices = 0;

	// ---store all osm objects temporarily here ---
	private SimpleObjectStore<Node> nodes;
	private SimpleObjectStore<Way> ways;
	private SimpleObjectStore<Relation> relations;

	// ---lot of data stored in ram ---

	// indexes of osm-relations
	TLongIntHashMap indexRelations;

	private TIntObjectHashMap<CompleteTurnRestriction> turnRestrictions;
	// mapping from via vertex node to outgoing edges
	private TIntObjectHashMap<THashSet<Integer>> outgoingEdges;

	// TODO: all used way id's are loaded into RAM, could be avoided
	private THashSet<Long> usedWays;

	private TLongObjectHashMap<ArrayList<Integer>> fromRestrictions;
	private TLongObjectHashMap<ArrayList<Integer>> toRestrictions;
	private TIntObjectHashMap<ArrayList<Integer>> viaRestrictions;

	// Lists long to vertexID and long to way point id
	TLongIntHashMap usedNodes;
	// List with all needed Nodes (usually on ways), they have the tags according to the used metric
	TLongObjectHashMap<CompleteNode> neededTagNodes;

    EdgeExtractor edgeExtractor;
	// extractor to extract the turn restrictions
	RestrictionExtractor restExtractor;
	// the config file (contains all information from the xml file
	ConfigObject configObject;

	// --- options of graph creation ---
	// save to pbf parameter
	private boolean saveStatsToPbf;

	// options of including turn restrictions and special weight treatment
	boolean useUnidirGraph = true; // transform into unidirectional graph if necessary
	boolean includeTurnRestrictions = true;

	// --- other ---
	// List of used highway levels (get this from the config file)
	private TIntObjectHashMap<String> hwyLevels;

	// Path to store the complete graph as PBF
	private String pbfPath = null;
	private String sqlPath = null;

	RoutingGraphCreatorTask(String xmlConfigPath, String neededVehicle, String usedMetric,
			String outputSqlPath,
			String pbf_creation,
			String outputPbfPath) {

		System.out
				.println("[RGC - extracting RG] -------initializing COMPLETE RG extraction --------");

		// initialize all parameters
		pbfPath = outputPbfPath;
		sqlPath = outputSqlPath;
		saveStatsToPbf = Boolean.valueOf(pbf_creation);

		if (saveStatsToPbf)
			System.out.println("[RGC - extracting RG] CREATES THE PBF WITH STATS INSIDE TOO");
		System.out.println("[RGC - extracting RG] IS CREATING THE SQL FILE AS OUTPUT");


        String vehicle = neededVehicle != null ? neededVehicle : "motorcar";
        String metric = usedMetric != null ? usedMetric : "fastest-motorcar";

		// initialize all required data structures
		configObject = XMLReader.parseXMLFromFile(xmlConfigPath, vehicle, metric);

		this.usedNodes = new TLongIntHashMap();
		this.neededTagNodes = new TLongObjectHashMap<CompleteNode>();

		indexRelations = new TLongIntHashMap();
		restExtractor = new RestrictionExtractor(configObject);

		usedWays = new THashSet<Long>();

		fromRestrictions = new TLongObjectHashMap<ArrayList<Integer>>();
		toRestrictions = new TLongObjectHashMap<ArrayList<Integer>>();
		viaRestrictions = new TIntObjectHashMap<ArrayList<Integer>>();
		turnRestrictions = new TIntObjectHashMap<CompleteTurnRestriction>();

		outgoingEdges = new TIntObjectHashMap<THashSet<Integer>>();

		hwyLevels = configObject.getWayTypesMap();
		// for (int i = 0; i < hwyLevels.size(); i++) {
		// System.out.println("[RGC - extracting RG] used way type: " + hwyLevels.get(i) + " ("
		// + i + ")");
		// }

		// initialize stores where nodes an ways are temporarily written to :
		this.nodes = new SimpleObjectStore<Node>(new SingleClassObjectSerializationFactory(
				Node.class), "nodes", true);

		this.ways = new SimpleObjectStore<Way>(new SingleClassObjectSerializationFactory(
				Way.class),
				"ways", true);

		this.relations = new SimpleObjectStore<Relation>(new SingleClassObjectSerializationFactory(
				Relation.class),
				"relations", true);

	}

	@Override
	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();
		switch (entity.getType()) {
			case Bound:
				break;
			case Node:
				Node node = (Node) entity;

				// check if node is important and put into hash table
				if (isOnWhiteList(node)) {
					HashSet<KeyValuePair> tags = new HashSet<KeyValuePair>();
					addPairsForTagToHashSet(node, tags);

					neededTagNodes.put(
							node.getId(),
							new CompleteNode(node.getId(), new GeoCoordinate(GeoCoordinate
									.doubleToInt(node
											.getLatitude()), GeoCoordinate.doubleToInt(node
									.getLongitude())), tags));
				}

				// add to store
				nodes.add(node);
				amountOfNodesProcessed++;
				break;
			case Way:
				Way way = (Way) entity;
				if (!isOnBlackList(way) && isOnWhiteList(way) && way.getWayNodes().size() > 1) {
					List<WayNode> waynodes = way.getWayNodes();

					// set start node type to vertex
					WayNode wayNode = waynodes.get(0);
					usedNodes.remove(wayNode.getNodeId());
					usedNodes.put(wayNode.getNodeId(), NODE_TYPE_VERTEX);

					// set end node type to vertex
					wayNode = waynodes.get(waynodes.size() - 1);
					usedNodes.remove(wayNode.getNodeId());
					usedNodes.put(wayNode.getNodeId(), NODE_TYPE_VERTEX);

					// set intermediate node types
					for (int i = 1; i < waynodes.size() - 1; i++) {
						wayNode = waynodes.get(i);
						if (usedNodes.containsKey(wayNode.getNodeId())) {
							// node has been referenced by a different way
							usedNodes.remove(wayNode.getNodeId());
							usedNodes.put(wayNode.getNodeId(), NODE_TYPE_VERTEX);
						} else {
							// node has not been referenced by a different way
							usedNodes.put(wayNode.getNodeId(), NODE_TYPE_WAYPOINT);
						}
					}
					// add to store
					usedWays.add(way.getId());
					ways.add(way);
				}
				amountOfWaysProcessed++;
				break;
			case Relation:
				Relation rel = (Relation) entity;
				if (isOnWhiteList(rel))
					relations.add(rel);
				amountOfRelationsProcessed++;
				break;
		}
	}

	@Override
	public void complete() {

		// count number of vertices :
		usedNodes.forEachValue(new TIntProcedure() {
			@Override
			public boolean execute(int v) {
				if (v == NODE_TYPE_VERTEX) {
					numVertices++;
				}
				return true;
			}
		});

		// assign ids to vertices and waypoints :
		usedNodes.transformValues(new TIntFunction() {
			int nextVertexId = 0;
			int nextWaypointId = usedNodes.size() - 1;

			@Override
			public int execute(int v) {
				if (v == NODE_TYPE_WAYPOINT) {
					return nextWaypointId--;
				}
				return nextVertexId++;
			}
		});

		// WRITE : all nodes (either in sql file or in hash map)

		// initialize SQLWriter object
        SqlWriter sqlWriter = new SqlWriter(hwyLevels, sqlPath);

		int[] latitudes = new int[usedNodes.size()];
		int[] longitudes = new int[usedNodes.size()];

		ReleasableIterator<Node> iterNodes = nodes.iterate();
		while (iterNodes.hasNext()) {
			Node node = iterNodes.next();
			if (!usedNodes.containsKey(node.getId())) {
				continue;
			}
			// set Id of vertex
			int idx = usedNodes.get(node.getId());
			latitudes[idx] = GeoCoordinate.doubleToInt(node.getLatitude());
			longitudes[idx] = GeoCoordinate.doubleToInt(node.getLongitude());
			if (idx < numVertices) {

				// if it is a vertex (not a waypoint) write it to the graph
				// needed tags as keyValuePairs of node
				HashSet<KeyValuePair> tags = new HashSet<KeyValuePair>();

				// Check for tags
				addPairsForTagToHashSet(node, tags);

				// add new vertex
				long osmId = node.getId();

				GeoCoordinate coordinate = new GeoCoordinate(
						GeoCoordinate.intToDouble(latitudes[idx]),
						GeoCoordinate.intToDouble(longitudes[idx]));

				sqlWriter.addCompleteVertex(new CompleteVertex(idx, osmId, null, coordinate, tags));

				amountOfVerticesWritten++;
			}
		}
		iterNodes.release();

		// Transform : all relations into turn restrictions

		// for now it's just for turn restrictions with 1 node as via member, OTHER RELATIONS ARE
		// NOT SUPPORTED FOR NOW!!!!
		// TODO: include complex turn restrictions
		if (includeTurnRestrictions) {
			ReleasableIterator<Relation> iterRelations = relations.iterate();
			while (iterRelations.hasNext()) {
				Relation rel = iterRelations.next();
				// Process, create and add new relation

				if (restExtractor.isNeededRelation(rel, usedWays, usedNodes)) {
					// put via node with modified id into outgoingEdges hash map
					int viaId = usedNodes.get(restExtractor.getCurrentTR().getOsmViaId());

					// put into list with complete turn restrictions
					turnRestrictions.put(turnRestrictionsWrittenAmount,
							restExtractor.transformRelationToComplTR(rel,
									turnRestrictionsWrittenAmount, viaId));

					if (!outgoingEdges.containsKey(viaId)) {
						outgoingEdges.put(viaId,
								new THashSet<Integer>());
					}

					// store via, from and to way osm id and save all the restrictions related to them
					if (fromRestrictions.containsKey(restExtractor.getCurrentTR().getOsmfromId())) {
						fromRestrictions.get(restExtractor.getCurrentTR().getOsmfromId()).add(
								turnRestrictionsWrittenAmount);
					} else {
						ArrayList<Integer> aList = new ArrayList<Integer>();
						aList.add(turnRestrictionsWrittenAmount);
						fromRestrictions.put(restExtractor.getCurrentTR().getOsmfromId(), aList);
					}

					if (toRestrictions.containsKey(restExtractor.getCurrentTR().getOsmtoId())) {
						toRestrictions.get(restExtractor.getCurrentTR().getOsmtoId())
								.add(turnRestrictionsWrittenAmount);
					} else {
						ArrayList<Integer> aList = new ArrayList<Integer>();
						aList.add(turnRestrictionsWrittenAmount);
						toRestrictions.put(restExtractor.getCurrentTR().getOsmtoId(), aList);
					}

					if (viaRestrictions.containsKey(viaId))
						viaRestrictions.get(viaId).add(turnRestrictionsWrittenAmount);
					else {
						ArrayList<Integer> aList = new ArrayList<Integer>();
						aList.add(turnRestrictionsWrittenAmount);
						viaRestrictions.put(viaId, aList);
					}

					indexRelations.put(rel.getId(), turnRestrictionsWrittenAmount);
					turnRestrictionsWrittenAmount++;
				}

			}
			iterRelations.release();

		}

		// WRITE : all edges

		edgeExtractor = new EdgeExtractor(fromRestrictions, toRestrictions, viaRestrictions, usedNodes,
				numVertices, neededTagNodes, includeTurnRestrictions, useUnidirGraph, configObject,
				outgoingEdges, turnRestrictions, saveStatsToPbf, sqlWriter);

		ReleasableIterator<Way> iterWays = ways.iterate();
		while (iterWays.hasNext()) {
			Way way = iterWays.next();

			// Check tags and create edges
			edgeExtractor.transformToEdgesAndWrite(way, latitudes, longitudes);
		}
		iterWays.release();

		// kill all wrong TR
		for (int tr_id : edgeExtractor.getWrongTR()) {
			CompleteTurnRestriction tr = turnRestrictions.get(tr_id);
			if (tr.getFromEdgeId() == -1 || tr.getToEdgeId() == -1) {
				turnRestrictions.remove(tr_id);
			}
		}

		// WRITE : all turn restrictions
		if (!checkRedundantTR(turnRestrictions)) {
            restExtractor.saveTurnRestrictions(turnRestrictions, outgoingEdges, sqlWriter);

        } else {
            System.out.println("[RGC - ]redundant turn restriction information isinvolved");
        }

		// finish the SQL writing
		sqlWriter.finishWriteSQLFile();
		// FINISH
	}

	@Override
	public void release() {
		// close stream

		System.out.println("[RGC - extracting RG] ---- SUMMARY: COMPLETE RG extraction ----");
		// print summary
		System.out
				.println("[RGC - extracting RG] amountOfNodesProcessed = " + amountOfNodesProcessed);
		System.out.println("[RGC - extracting RG] amountOfWaysProcessed = " + amountOfWaysProcessed);
		System.out.println("[RGC - extracting RG] amountOfRelationsProcessed = "
				+ amountOfRelationsProcessed);
		System.out.println("[RGC - extracting RG] amountOfVerticesWritten = "
				+ amountOfVerticesWritten);
		System.out.println("[RGC - extracting RG] amountOfEdgesWritten = "
				+ edgeExtractor.getAmountOfEdgesWritten());
		System.out.println("[RGC - extracting RG] ---- SUMMARY: TURN RESTRICTIONS ----");
		System.out.println("[RGC - extracting RG] amountOfOSMTurnRestrictions = "
				+ restExtractor.getOsmRelationsAmount());
		System.out.println("[RGC - extracting RG] amountOfTurnRestrictionsWritten = "
				+ restExtractor.getTurnRestrictionsWrittenAmount());
		System.out.println("[RGC - extracting RG] amountOfIgnoredRelations = "
				+ restExtractor.getAmountOfIgnoredRelations());
		System.out.println("[RGC - extracting RG] amountOfCurruptTurnRestrictions = "
				+ edgeExtractor.getWrongTR().size());

		// System.out.println("[RGC - extracting RG] amountOfViaNodes = " +
		// restExtractor.osmTrHs.size());
		// System.out.println("[RGC - extracting RG] fromEdgesUpdated = "
		// + edgeExtractor.getFromEdgesUpdated());
		// System.out.println("[RGC - extracting RG] toEdgesUpdated = "
		// + edgeExtractor.getToEdgesUpdated());

		System.out.println("[RGC - extracting RG] amountOfUniDirTransformations = "
				+ edgeExtractor.getAmountOfUniDirTransformations());

		if (saveStatsToPbf) {
			int sum = edgeExtractor.getEdges().size();

			System.out.println("[RGC - extracting RG] Writing " + sum + " objects fo file: "
					+ pbfPath);

			ProtobufCreator
					.saveToFile(pbfPath, edgeExtractor.getEdges(), edgeExtractor.getIndexEdges());
			System.out
					.println("[RGC - extracting RG] finished writing the statistics into the pbf file");
		}

		// free ram
		this.usedNodes = null;
		this.hwyLevels = null;
		this.fromRestrictions = null;
		this.toRestrictions = null;
		this.viaRestrictions = null;
		this.turnRestrictions = null;
		restExtractor.clean();
		edgeExtractor.clean();
	}

	private boolean isOnWhiteList(Way way) {
		for (Tag tag : way.getTags()) {
			// all usable ways
			if (this.configObject.getUsableWays().contains(
					new KeyValuePair(tag.getValue(), tag.getKey()))) {
				return true;
			}
		}
		return false;
	}

	// check if a way has tags which are not allowed used with the vehicle
	private boolean isOnBlackList(Way way) {

		for (Tag tag : way.getTags()) {
			if (this.configObject.containsRestrictionTag(tag.getKey(), tag.getValue()))
				return true;
		}
		return false;
	}

	private boolean isOnWhiteList(Node node) {

		for (Tag tag : node.getTags()) {
			if (this.configObject.containsNodeTag(tag.getKey(), tag.getValue()))
				return true;
		}
		return false;
	}

	private boolean isOnWhiteList(Relation rel) {

		for (Tag tag : rel.getTags()) {
			if (this.configObject.containsRelationTag(tag.getKey(), tag.getValue()))
				return true;
		}
		return false;
	}

	private void addPairsForTagToHashSet(Node node, HashSet<KeyValuePair> hs) {
		for (Tag tag : node.getTags()) {

			if (configObject.containsNodeTag(tag.getKey(), tag.getValue()))
				hs.add(new KeyValuePair(tag.getValue(), tag.getKey()));

		}
	}

	/**
	 * Checks if redundant TR are in the HashMap
	 * 
	 * @param tr
	 *            turn restrictions
	 * @return true for redundant TRs and false for no redundant TRs
	 */
	private boolean checkRedundantTR(TIntObjectHashMap<CompleteTurnRestriction> tr) {
		THashSet<CompleteTurnRestriction> ntr = new THashSet<CompleteTurnRestriction>();
		for (TIntObjectIterator<CompleteTurnRestriction> it = tr.iterator(); it.hasNext();) {
			it.advance();
			CompleteTurnRestriction turnRestriction = it.value();
			if (ntr.contains(turnRestriction)) {
				System.out.println(turnRestriction);
			} else
				ntr.add(turnRestriction);
		}
		if (tr.size() == ntr.size())
			return false;
		System.out.println("it is: " + tr.size() + " it should be: " + ntr.size());
		return true;

	}

}
