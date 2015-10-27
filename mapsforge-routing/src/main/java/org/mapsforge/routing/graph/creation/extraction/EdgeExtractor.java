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

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.graph.creation.sql.SqlWriter;
import org.mapsforge.routing.graph.creation.extraction.turnRestrictions.CompleteTurnRestriction;
import org.mapsforge.routing.graph.creation.osmosis.TagHighway;
import org.mapsforge.routing.graph.creation.weighting.IWeightMetric;
import org.mapsforge.routing.graph.creation.weighting.TimeMetric;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import java.util.*;

/**
 * This class creates the new edges of the routing graph out of raw osm ways.
 * 
 * @author Robert Fels
 * 
 */
public class EdgeExtractor {

	private int amountOfUniDirTransformations = 0;
	private int amountOfEdgesWritten = 0;
	private int fromEdgesUpdated = 0;
	private int toEdgesUpdated = 0;
	private THashSet<Integer> wrongTR;

	// ---lot of data stored in ram ---
	// ONLY USED WHEN PBF FILE FOR STATISTICS IS CREATED (using a DB would be better than PBF)
	private TIntObjectHashMap<CompleteEdge> edges;
	// List which maps source and target vertices to the corresponding edges (is needed for the
	// evaluation of a calculated route at the end, statistical data will be given)
	private TObjectIntHashMap<String> indexEdges;

	TLongIntHashMap usedNodes;
	int numVertices;
	// mapping from via vertex node to outgoing edges
	private TIntObjectHashMap<THashSet<Integer>> outgoingEdges;
	private TIntObjectHashMap<CompleteTurnRestriction> turnRestrictions;

	TLongObjectHashMap<CompleteNode> neededTagNodes;

	private final HashSet<String> remValues = new HashSet<String>(Arrays.asList("name",
            "destination", "ref"));

	TLongObjectHashMap<ArrayList<Integer>> fromRestrictions;
	TLongObjectHashMap<ArrayList<Integer>> toRestrictions;
	TIntObjectHashMap<ArrayList<Integer>> viaRestrictions;

	ConfigObject configObject;
	boolean includeTurnRestrictions;
	boolean useUniDirGraph;
	boolean saveStatsToPbf;
	private IWeightMetric weightMetric;
	private SqlWriter adapter;

	/**
	 * Constructor for the edge extractor
	 *
	 * @param fromRestrictions
	 *            hashmap which maps a from_osm_way id to all restriction id's (in array)where the way
	 *            is a part of
	 * @param toRestrictions
	 *            hashmap which maps a to_osm_way id to all restriction id's where the way is a part of
	 * @param viaRestrictions
	 *            maps all restrictions for one via node id
	 * @param usedNodes
	 *            Lists long to vertexID and long to way point id
	 * @param numVertices
	 *            amount of Vertices (idx)
	 * @param neededTagNodes
	 *            List with all needed Nodes (usually on ways), they have the tags according to the used
	 *            metric
	 * @param includeTurnRestrictions
	 *            true/false if including turn restrictions
	 * @param useUniDirGraph
	 *            true/false if inlcuding unidirectional transformation
	 * @param configObject
	 *            plz @see ConfigObject
	 * @param outgoingEdges
	 *            mapping from via vertex node to outgoing edges
	 * @param turnRestrictions
	 *            all complete turn restrictions in a hash map
	 * @param saveStatsToPbf
	 *            true/false for saving statistics into pbf
	 * @param sqlWriter
	 *            Writer to create the SQL file.
	 */
	public EdgeExtractor(TLongObjectHashMap<ArrayList<Integer>> fromRestrictions,
			TLongObjectHashMap<ArrayList<Integer>> toRestrictions,
			TIntObjectHashMap<ArrayList<Integer>> viaRestrictions,
			TLongIntHashMap usedNodes,
			int numVertices,
			TLongObjectHashMap<CompleteNode> neededTagNodes,
			boolean includeTurnRestrictions,
			boolean useUniDirGraph,
			ConfigObject configObject,
			TIntObjectHashMap<THashSet<Integer>> outgoingEdges,
			TIntObjectHashMap<CompleteTurnRestriction> turnRestrictions,
			boolean saveStatsToPbf,
			SqlWriter sqlWriter) {
		this.fromRestrictions = fromRestrictions;
		this.toRestrictions = toRestrictions;
		this.viaRestrictions = viaRestrictions;
		this.usedNodes = usedNodes;
		this.numVertices = numVertices;
		this.neededTagNodes = neededTagNodes;
		this.includeTurnRestrictions = includeTurnRestrictions;
		this.useUniDirGraph = useUniDirGraph;
		this.configObject = configObject;
		this.outgoingEdges = outgoingEdges;
		this.turnRestrictions = turnRestrictions;
		this.saveStatsToPbf = saveStatsToPbf;
		this.adapter = sqlWriter;

		if (saveStatsToPbf)
			edges = new TIntObjectHashMap<CompleteEdge>();

		indexEdges = new TObjectIntHashMap<String>();
		wrongTR = new THashSet<Integer>();
		this.weightMetric = new TimeMetric(configObject);
	}

	/**
	 * get indices of wrong turn restrictions
	 *
	 * @return HS with indices
	 */
	public THashSet<Integer> getWrongTR() {
		return wrongTR;
	}

	/**
	 *
	 * @return amount of uni directional transformations
	 */
	public int getAmountOfUniDirTransformations() {
		return amountOfUniDirTransformations;
	}

	/**
	 *
	 * @return amount of all written edges
	 */
	public int getAmountOfEdgesWritten() {
		return amountOfEdgesWritten;
	}

	/**
	 *
	 * @return amount of all updated from edges
	 */
	public int getFromEdgesUpdated() {
		return fromEdgesUpdated;
	}

	/**
	 *
	 * @return amount of all updated to edges
	 */
	public int getToEdgesUpdated() {
		return toEdgesUpdated;
	}

	/**
	 * get all complete edges if saved
	 *
	 * @return complete edge HM
	 */
	public TIntObjectHashMap<CompleteEdge> getEdges() {
		return edges;
	}

	/**
	 * clean ram
	 */
	public void clean() {
		this.edges = null;
		this.indexEdges = null;

	}

	/**
	 * get edge indexes
	 *
	 * @return edge indexes HM
	 */
	public TObjectIntHashMap<String> getIndexEdges() {
		return indexEdges;
	}

	/**
	 * This method transforms an osm-way into CompleteEdges
	 *
	 * @param way
	 *            the way to be processed
	 * @param latitudeE6
	 *            an array of latitude-doubles
	 * @param longitudeE6
	 *            an array of longitude-doubles
	 */
	public void transformToEdgesAndWrite(Way way, int[] latitudeE6, int[] longitudeE6) {

		// check if from or to way

		long wayOsmId = way.getId();
		boolean fromEdge = fromRestrictions.containsKey(wayOsmId);
		boolean toEdge = toRestrictions.containsKey(wayOsmId);
		int fromUpdateCounter = 0;
		int toUpdateCounter = 0;
		List<WayNode> wayNodes = way.getWayNodes();
		boolean firstEdge = false;
		boolean lastEdge = false;
		// edge which consists of only one osm-way
		boolean singleEdge = false;

		//
		// if (way.getId() == 4073978 || way.getId() == 53284366 || way.getId() == 46895502
		// || way.getId() == 4352777) {
		// }

		LinkedList<Integer> indices = new LinkedList<Integer>(); // list with indexes for waypoints
		// Check waypoints in between and save
		for (int i = 0; i < wayNodes.size(); i++) {
			// index of vertex as it'll be saved into the DB
			int idx = usedNodes.get(wayNodes.get(i).getNodeId());
			// add only indices of vertices
			if (idx < numVertices) {
				indices.addLast(i);
			}
		}

		// for all vertex indices of a way
		for (int i = 1; i < indices.size(); i++) {
			// 2 adjacent vertices are an edge
			int start = indices.get(i - 1);
			int end = indices.get(i);
			// create arrays for lon and lat (size of # of WP incl. vertices)
			double[] lon = new double[end - start + 1];
			double[] lat = new double[end - start + 1];

			// save the coordinates of all waypoints
			GeoCoordinate[] allwp = new GeoCoordinate[lon.length];

			// save Waypoints as complete nodes (only the ones which are needed)
			ArrayList<CompleteNode> allWayNodes = new ArrayList<CompleteNode>();

			// calculate length of the new edge and add way nodes for the edge
			for (int j = start; j <= end; j++) {
				// id of node
				long wayNode = way.getWayNodes().get(j).getNodeId();
				// get index of wp
				int idx = usedNodes.get(wayNode);
				lon[j - start] = GeoCoordinate.intToDouble(longitudeE6[idx]);
				lat[j - start] = GeoCoordinate.intToDouble(latitudeE6[idx]);
				// put in allwp with geocoordinates
				allwp[j - start] = new GeoCoordinate(lat[j - start], lon[j - start]);

				if (neededTagNodes.containsKey(wayNode)) {
					CompleteNode cn = neededTagNodes.get(wayNode);
					allWayNodes.add(cn);
				}
			}
			boolean oneway;

			// set source, target and one way attribute
			int sourceId;
			int targetId;
			// Check if oneway
			if (isOneWay(way) == -1) {
				sourceId = usedNodes.get(way.getWayNodes().get(end).getNodeId());
				targetId = usedNodes.get(way.getWayNodes().get(start).getNodeId());
				oneway = true;
				lon = reverse(lon);
				lat = reverse(lat);
			} else {
				sourceId = usedNodes.get(way.getWayNodes().get(start).getNodeId());
				targetId = usedNodes.get(way.getWayNodes().get(end).getNodeId());
				oneway = isOneWay(way) == 1;
			}

			// this is for motorways and primary roads
			// name attribute always needed, so it's independent from the config file
			Tag wayName = getTag(way, "name");

			// this is for motorway links which lead onto a highway
			Tag wayRef = getTag(way, "ref");

			// this is for destination of a link
			Tag wayDest = getTag(way, "destination");

			// type of the highway
			// type attribute always needed, so it's independent from the config file
			Tag wayType = getType(way);

			if (lat.length != lon.length) {
				System.out.println("[RGC - extracting RG] FATAL error lat.length!=lon.length ");
				break;
			}

			THashSet<KeyValuePair> hs = new THashSet<KeyValuePair>();

			// check all tags
			addPairsForTagToHashSet(way, hs);

			// create the new edge
			long key = way.getId();

			CompleteEdge ce = new CompleteEdge(key,
					sourceId,
					targetId,
					allwp,
					wayName != null ? wayName.getValue() : null,
					wayType != null ? wayType.getValue() : null,
					isRoundabout(way),
					oneway,
					wayRef != null ? wayRef.getValue() : null,
					wayDest != null ? wayDest.getValue() : null,
					// null,
					0, // don't like the zero, ways to avoid that?
					hs,
					allWayNodes);

			// check if first or last vertex are via nodes
            firstEdge = i == 1 && indices.size() != 2;
            lastEdge = indices.size() != 2 && i == indices.size() - 1;
            singleEdge = i == 1 && indices.size() == 2;

			// attention: no beautiful code is coming up ;)
			// fill sql-file without turn restrictions
			if (!includeTurnRestrictions) {
				// unidirectional transformation
				if (useUniDirGraph
						&& !oneway
						&& uniDirTransformation(configObject.getJunctionNodes(), ce,
								configObject.getJunctionDistance())) {

					splitAndWriteEdges(ce);
				} else {
					// write
					writeEdge(ce);
				}
			} else {
				if (useUniDirGraph
						&& !oneway
						&& uniDirTransformation(configObject.getJunctionNodes(), ce,
								configObject.getJunctionDistance())) {

					if (firstEdge || lastEdge || singleEdge) {
						int[] from_to_array = splitAndWriteEdgesTR(ce, wayOsmId, fromEdge, toEdge,
								firstEdge,
								lastEdge, singleEdge);
						fromUpdateCounter += from_to_array[0];
						toUpdateCounter += from_to_array[1];

					} else
						splitAndWriteEdges(ce);

					amountOfUniDirTransformations++;

				} else {
					// write bidirectional or one way edge

					// calculate and set weight for the particular edge
					ce.setWeight(weightMetric.getCostDouble(ce));
					// write
					if (!(!ce.isOneWay() && fromEdge))
						writeEdge(ce);

					// fromedge:
					if (fromEdge) {
						// first edge
						if (firstEdge) {
							if (ce.isOneWay()) {
								// only -1 needs to be considered and startpoint (target of -1_edge)
								if (isOneWay(way) == -1) {
									fromUpdateCounter += checkAndUpdateFromEdges(ce.getTargetId(),
											wayOsmId,
											amountOfEdgesWritten - 1);
								}

							} else {
								// for bidirectional only source needs to be checked
								// fromUpdateCounter += checkAndUpdateFromEdges(ce.getSourceId(),
								// wayOsmId, amountOfEdgesWritten - 1);
								int[] from_to_array = splitAndWriteEdgesTR(ce, wayOsmId, fromEdge,
										toEdge,
										firstEdge,
										lastEdge, singleEdge);
								fromUpdateCounter += from_to_array[0];
								toUpdateCounter += from_to_array[1];

							}
						}
						// last edge
						if (lastEdge) {
							if (ce.isOneWay()) {
								// only the oneway in right direction needs to be considered
								if (isOneWay(way) == 1) {
									fromUpdateCounter += checkAndUpdateFromEdges(ce.getTargetId(),
											wayOsmId,
											amountOfEdgesWritten - 1);
								}

							} else {
								// for bidirectional only target needs to be checked
								// fromUpdateCounter += checkAndUpdateFromEdges(ce.getTargetId(),
								// wayOsmId,
								// amountOfEdgesWritten - 1);
								int[] from_to_array = splitAndWriteEdgesTR(ce, wayOsmId, fromEdge,
										toEdge,
										firstEdge,
										lastEdge, singleEdge);
								fromUpdateCounter += from_to_array[0];
								toUpdateCounter += from_to_array[1];
							}

						}

						// both:
						if (singleEdge) {
							if (ce.isOneWay()) {
								// check both one way possibilities
								if (isOneWay(way) == -1) {
									fromUpdateCounter += checkAndUpdateFromEdges(ce.getTargetId(),
											wayOsmId,
											amountOfEdgesWritten - 1);
								}
								if (isOneWay(way) == 1) {
									fromUpdateCounter += checkAndUpdateFromEdges(ce.getTargetId(),
											wayOsmId,
											amountOfEdgesWritten - 1);
								}
							} else {
								// bidirectional: src and tar could be via node

								// fromUpdateCounter += checkAndUpdateFromEdges(ce.getSourceId(),
								// wayOsmId, amountOfEdgesWritten - 1);
								//
								// fromUpdateCounter += checkAndUpdateFromEdges(ce.getTargetId(),
								// wayOsmId,
								// amountOfEdgesWritten - 1);

								int[] from_to_array = splitAndWriteEdgesTR(ce, wayOsmId, fromEdge,
										toEdge,
										firstEdge,
										lastEdge, singleEdge);
								fromUpdateCounter += from_to_array[0];
								toUpdateCounter += from_to_array[1];
							}
						}
					}

					if (toEdge) {
						// first edge:
						if (firstEdge) {
							if (ce.isOneWay()) {
								// only right one way needs to be considered(not the reverse)
								if (isOneWay(way) == 1) {
									toUpdateCounter += checkAndUpdateToEdges(ce.getSourceId(),
											wayOsmId,
											amountOfEdgesWritten - 1);
								}
							} else {
								// bidirectional:
								toUpdateCounter += checkAndUpdateToEdges(ce.getSourceId(),
										wayOsmId,
										amountOfEdgesWritten - 1);
							}

						}
						// last edge:
						if ((lastEdge)) {
							if (ce.isOneWay()) {
								// only reversed oneway needs to be considered
								if (isOneWay(way) == -1) {
									toUpdateCounter += checkAndUpdateToEdges(ce.getSourceId(),
											wayOsmId,
											amountOfEdgesWritten - 1);
								}
							} else {
								// bidir: consider only right direction
								toUpdateCounter += checkAndUpdateToEdges(ce.getTargetId(),
										wayOsmId,
										amountOfEdgesWritten - 1);

							}

						}
						// both:
						if (singleEdge) {
							if (ce.isOneWay()) {
								if (isOneWay(way) == 1) {
									toUpdateCounter += checkAndUpdateToEdges(ce.getSourceId(),
											wayOsmId,
											amountOfEdgesWritten - 1);
								}
								if (isOneWay(way) == -1) {
									toUpdateCounter += checkAndUpdateToEdges(ce.getSourceId(),
											wayOsmId,
											amountOfEdgesWritten - 1);
								}
							} else {
								// bidir: check both
								toUpdateCounter += checkAndUpdateToEdges(ce.getSourceId(),
										wayOsmId,
										amountOfEdgesWritten - 1);
								toUpdateCounter += checkAndUpdateToEdges(ce.getTargetId(),
										wayOsmId,
										amountOfEdgesWritten - 1);

							}
						}
					}

					if (firstEdge || lastEdge || singleEdge) {
						// check if it is an outgoing edge from a via node
						if (outgoingEdges.contains(ce.getSourceId())) {
							outgoingEdges.get(ce.getSourceId()).add(amountOfEdgesWritten - 1);
						}
						if (!ce.isOneWay() && outgoingEdges.contains(ce.getTargetId())) {
							outgoingEdges.get(ce.getTargetId()).add(amountOfEdgesWritten - 1);
						}
					}

				}

			}

		}

		if (fromEdge) {
			if (fromRestrictions.get(wayOsmId) == null) {
				System.out
						.println("CRITICAL: transformEdges: edge is from edge but has no TR_ID!!!!");
			}
			if (fromUpdateCounter != fromRestrictions.get(wayOsmId).size()) {
				// System.out.println("CRITICAL: transformEdges: not all from edge ids updated, only "
				// + fromUpdateCounter + " not " + fromRestrictions.get(wayOsmId).size()
				// + " for edge id: " + wayOsmId);
				ArrayList<Integer> arrayList = fromRestrictions.get(wayOsmId);
				for (Integer integer : arrayList) {
					wrongTR.add(integer);

				}
			}
		}

		if (toEdge) {
			if (toRestrictions.get(wayOsmId) == null) {
				System.out.println("CRITICAL: splitAndWrite: edge is to edge but has no TR_ID!!!!");
			}
			if (toUpdateCounter != toRestrictions.get(wayOsmId).size()) {
				// System.out.println("CRITICAL: transformEdges: not all to edge ids updated, only "
				// + toUpdateCounter + " not " + toRestrictions.get(wayOsmId).size()
				// + " for edge id: " + wayOsmId);
				ArrayList<Integer> arrayList = toRestrictions.get(wayOsmId);
				for (Integer integer : arrayList) {
					wrongTR.add(integer);

				}
			}
		}

	}

	/**
	 * Creates and saves unidirectional edges out of a bidirectional edges
	 *
	 * @param ce
	 *            bidirectional edge
	 * @param wayOsmId
	 *            way osm id
	 * @param fromEdge
	 *            bool if from edge
	 * @param toEdge
	 *            bool if to edge
	 * @param firstEdge
	 *            check if this is the first routing edge of an osm way
	 * @param lastEdge
	 *            check if this is the last routing edge of an osm way
	 * @param singleEdge
	 *            edge which consists of only one osm-way
	 * @return array with amount of updated from_edges [0] and to_edges [1]
	 */
	public int[] splitAndWriteEdgesTR(CompleteEdge ce, long wayOsmId,
			boolean fromEdge,
			boolean toEdge,
			boolean firstEdge,
			boolean lastEdge,
			boolean singleEdge) {

		int[] from_to_array = new int[2];
		int fromUpdateCounter = 0;
		int toUpdateCounter = 0;
		// write first unidirectional edge
		ce.setOneWay(true);

		// we call the first splitted edge which is written firstAddedEdge!
		int firstAddedEdge = amountOfEdgesWritten;
		// write
		writeEdge(ce);

		// write second oneway

		// switch source and target and indices of the waypoints
		int sourceId = ce.getSourceId();
		int targetId = ce.getTargetId();
		ArrayList<CompleteNode> allWayNodes = ce.getAllUsedNodes();
		Collections.reverse(allWayNodes);
		GeoCoordinate[] allwp = ce.getAllWaypoints();
		allwp = reverse(allwp);

		ce.setSourceId(targetId);
		ce.setTargetId(sourceId);
		ce.setAllUsedNodes(allWayNodes);
		ce.setAllWaypoints(allwp);

		// we call the first splitted edge which is written firstAddedEdge!
		int secAddedEdge = amountOfEdgesWritten;

		// write
		writeEdge(ce);

		// check if it is an outgoing edge from a via node, respective to the start(source) node
		if (outgoingEdges.contains(sourceId)) {
			outgoingEdges.get(sourceId).add(firstAddedEdge);

		}
		if (outgoingEdges.contains(targetId)) {
			outgoingEdges.get(targetId).add(secAddedEdge);
		}

		// check if it is the new edge out of the turn restriction relation
		if (fromEdge) {
			if ((firstEdge)) {
				// we need the edge which has the via as target!
				// case for opposite direction (via is first node)
				fromUpdateCounter += checkAndUpdateFromEdges(sourceId, wayOsmId,
						secAddedEdge);

			}
			if ((lastEdge)) {
				fromUpdateCounter += checkAndUpdateFromEdges(targetId, wayOsmId, firstAddedEdge);
			}
			if (singleEdge) {
				fromUpdateCounter += checkAndUpdateFromEdges(targetId, wayOsmId, firstAddedEdge);
				fromUpdateCounter += checkAndUpdateFromEdges(sourceId, wayOsmId,
						secAddedEdge);
			}

		}
		if (toEdge) {
			if (firstEdge) {
				toUpdateCounter += checkAndUpdateToEdges(sourceId, wayOsmId, firstAddedEdge);
			}
			if (lastEdge) {
				toUpdateCounter += checkAndUpdateToEdges(targetId, wayOsmId, secAddedEdge);
			}
			if (singleEdge) {
				toUpdateCounter += checkAndUpdateToEdges(sourceId, wayOsmId, firstAddedEdge);
				toUpdateCounter += checkAndUpdateToEdges(targetId, wayOsmId, secAddedEdge);
			}

		}

		from_to_array[0] = fromUpdateCounter;
		from_to_array[1] = toUpdateCounter;
		return from_to_array;

	}

	/**
	 * Creates and saves unidirectional edges out of a bidirectional edges
	 *
	 * @param ce
	 *            bidirectional edge
	 */
	private void splitAndWriteEdges(CompleteEdge ce) {

		// write first unidirectional edge
		ce.setOneWay(true);

		// write
		writeEdge(ce);

		// write second oneway

		// switch source and target and indices of the waypoints
		int sourceId = ce.getSourceId();
		int targetId = ce.getTargetId();
		ArrayList<CompleteNode> allWayNodes = ce.getAllUsedNodes();
		Collections.reverse(allWayNodes);
		GeoCoordinate[] allwp = ce.getAllWaypoints();
		allwp = reverse(allwp);

		ce.setSourceId(targetId);
		ce.setTargetId(sourceId);
		ce.setAllUsedNodes(allWayNodes);
		ce.setAllWaypoints(allwp);

		// write
		writeEdge(ce);

	}

	private int checkAndUpdateFromEdges(int vertex_id, long fromEdge, int newFromEdge) {
		int fromEdgesUpdCounter = 0;
		if (viaRestrictions.contains(vertex_id)) {

			ArrayList<Integer> via_id_list = viaRestrictions.get(vertex_id);
			ArrayList<Integer> from_id_list = fromRestrictions.get(fromEdge);
			for (Integer tr_id : via_id_list) {
				if (from_id_list.contains(tr_id)) {
					turnRestrictions.get(tr_id).setFromEdgeId(newFromEdge);

					fromEdgesUpdated++;
					fromEdgesUpdCounter++;
				}
			}
		}
		return fromEdgesUpdCounter;
	}

	private int checkAndUpdateToEdges(int vertex_id, long toEdge, int newToEdge) {
		int toEdgeUpdCounter = 0;
		if (viaRestrictions.contains(vertex_id)) {
			ArrayList<Integer> via_id_list = viaRestrictions.get(vertex_id);
			ArrayList<Integer> to_id_list = toRestrictions.get(toEdge);
			for (Integer tr_id : via_id_list) {
				if (to_id_list.contains(tr_id)) {
					turnRestrictions.get(tr_id).setToEdgeId(newToEdge);

					toEdgesUpdated++;
					toEdgeUpdCounter++;
				}
			}
		}
		return toEdgeUpdCounter;

	}

	/**
	 * Edge is written into sql file
	 *
	 * @param ce
	 *            complete edge
	 */
	private void writeEdge(CompleteEdge ce) {

		// calculate and set weight for the particular edge
		ce.setWeight(weightMetric.getCostDouble(ce));

		// insert edge
		if (saveStatsToPbf) {
			edges.put(amountOfEdgesWritten, ce);
			adapter.addCompleteEdge(ce);
		} else
			adapter.addCompleteEdge(ce);

		// fill Hashmap to map source and target vertices to the corresponding edges
		String st = String.valueOf(ce.getSourceId()) + String.valueOf(ce.getTargetId());
		if (saveStatsToPbf) {
			// in case of same node to node connection take the edge with less weight!
			if (indexEdges.containsKey(st)) {
				if (indexEdges.get(st) > edges.get(indexEdges.get(st)).getWeight()) {
					indexEdges.put(st, amountOfEdgesWritten);
				}
			} else
				indexEdges.put(st, amountOfEdgesWritten);
		}

		amountOfEdgesWritten++;

	}

	/**
	 * Returns if a way is a one way
	 *
	 * @param way
	 *            way to be checked
	 * @return -1 for reverse to creation direction, 0 for no, 1 for yes
	 */
	private static int isOneWay(Way way) {

		Tag hwyTag = getTag(way, "highway");
		if (hwyTag != null
				&& (hwyTag.getValue().equals(TagHighway.MOTORWAY)
						|| hwyTag.getValue().equals(TagHighway.MOTORWAY_LINK)
						|| hwyTag.getValue().equals(TagHighway.TRUNK) || hwyTag.getValue()
						.equals(TagHighway.TRUNK_LINK))) {
			return 1;
		}

		Tag onwyTag = getTag(way, "oneway");
		if (onwyTag == null) {
			return 0;
		} else if (onwyTag.getValue().equals("true")
				|| onwyTag.getValue().equals("yes")
				|| onwyTag.getValue().equals("t")
				|| onwyTag.getValue().equals("1")) {
			return 1;
		} else if (onwyTag.getValue().equals("false")
				|| onwyTag.getValue().equals("no")
				|| onwyTag.getValue().equals("f")
				|| onwyTag.getValue().equals("0")) {
			return 0;
		} else if (onwyTag.getValue().equals("-1")) {
			return -1;
		}
		return 0;
	}

	/**
	 * Returns a reverse array
	 *
	 * @param array
	 *            arr
	 * @return arr
	 */
	private static double[] reverse(double[] array) {
		double[] tmp = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			tmp[array.length - 1 - i] = array[i];
		}
		return tmp;
	}

	/**
	 * Returns a reverse array
	 *
	 * @param array
	 *            arr
	 * @return arr
	 */
	private static GeoCoordinate[] reverse(GeoCoordinate[] array) {
		GeoCoordinate[] tmp = new GeoCoordinate[array.length];
		for (int i = 0; i < array.length; i++) {
			tmp[array.length - 1 - i] = array[i];
		}
		return tmp;
	}

	private static Tag getTag(Way way, String tagName) {
		for (Tag tag : way.getTags()) {
			if (tag.getKey().equals(tagName)) {
				return tag;
			}
		}
		return null;
	}

	private Tag getType(Way way) {
		for (Tag tag : way.getTags()) {

			if (this.configObject.getUsableWays().contains(
					new KeyValuePair(tag.getValue(), tag.getKey()))) {
				return tag;
			}
		}
		return null;
	}

	private void addPairsForTagToHashSet(Way way, THashSet<KeyValuePair> hs) {
		for (Tag tag : way.getTags()) {

			if (remValues.contains(tag.getKey()))
				continue;

			if (configObject.containsWayTag(tag.getKey(), tag.getValue()))
				hs.add(new KeyValuePair(tag.getValue(), tag.getKey()));
		}
	}

	private static boolean isRoundabout(Way way) {
		Tag t = getTag(way, "junction");

        return t != null && t.getValue().equals("roundabout");
    }

	/**
	 * Function to check if a transformation into a unidirectional graph is necessary
	 *
	 * @param kvp
	 *            key value pair, for example a tag for a traffic light
	 * @param ce
	 *            edge you want to check
	 * @param treshold
	 *            amount of nodes to check
	 * @return true for transformation false for
	 */
	private boolean uniDirTransformation(THashSet<KeyValuePair> kvp, CompleteEdge ce,
			int treshold) {

		// get geo coordinates of source and target vertex
		GeoCoordinate[] allWayPointCoord = ce.getAllWaypoints();
		GeoCoordinate sourceGeoCoord = allWayPointCoord[0];
		GeoCoordinate targetGeoCoord = allWayPointCoord[allWayPointCoord.length - 1];
		// all nodes which are on edge
		ArrayList<CompleteNode> allUsedNodes = ce.getAllUsedNodes();

		boolean outOfRangeStart = false;
		boolean outOfRangeEnd = false;
		if (allUsedNodes.size() != 0) {

			for (int i = 0; i <= allUsedNodes.size() / 2; i++) {
				// check nodes at the beginning of an edge
				double distanceToSource = allUsedNodes.get(i).getCoordinate()
						.sphericalDistance(sourceGeoCoord);
				if (distanceToSource <= treshold) {
					// should not be a vertex
					if (distanceToSource != 0) {
						for (KeyValuePair keyValuePair : allUsedNodes.get(i).getAdditionalTags()) {
							if (kvp.contains(keyValuePair)) {
								return true;
							}
						}
					}
				} else
					outOfRangeStart = true;

				double distanceToTarget = allUsedNodes.get(allUsedNodes.size() - i - 1).getCoordinate()
						.sphericalDistance(targetGeoCoord);
				// check nodes at the end of an edge
				if (!outOfRangeEnd && distanceToTarget <= treshold) {
					// should not be a vertex
					if (distanceToTarget != 0) {
						for (KeyValuePair keyValuePair : allUsedNodes.get(
								ce.getAllUsedNodes().size() - i - 1).getAdditionalTags()) {
							if (kvp.contains(keyValuePair)) {
								return true;
							}
						}
					}
				} else
					outOfRangeEnd = true;

				if (outOfRangeEnd && outOfRangeStart)
					return false;
			}
			if (treshold == 0) {
				return true;
			}
		}
		return false;
	}

}
