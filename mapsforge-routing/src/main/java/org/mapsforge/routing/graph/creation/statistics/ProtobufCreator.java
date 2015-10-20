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
package org.mapsforge.routing.graph.creation.statistics;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.graph.creation.extraction.CompleteEdge;
import org.mapsforge.routing.graph.creation.extraction.CompleteNode;
import org.mapsforge.routing.graph.creation.extraction.KeyValuePair;
import org.mapsforge.routing.graph.creation.statistics.StatsCreatorProtos.*;
import org.mapsforge.routing.graph.creation.statistics.StatsCreatorProtos.AllGraphDataPBF.Builder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * 
 * Class for loading and saving the PBF file.
 * 
 * @author Robert Fels
 * 
 */
public class ProtobufCreator {
	/**
	 * This methods loads data from a pbf-file.
	 * 
	 * @param path
	 *            , where the file is located
	 * @param edges
	 *            , all edges will be written into that map
	 * @param indexEdges
	 *            , all edge indices will be written into that map
	 */
	public static void loadFromFile(String path,
			TIntObjectHashMap<CompleteEdge> edges,
			TObjectIntHashMap<String> indexEdges
			// ,TIntObjectHashMap<CompleteRelation> relations
			) {
		AllGraphDataPBF allGraphData = null;
		try {
			allGraphData = AllGraphDataPBF.parseFrom(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out
					.println("Routing graph pbf not found! Please create it with the 'mapsforge-graph-creator-jar-copy' ant task from "
							+
							"the mapsforge project. ");
		} catch (IOException e) {
			e.printStackTrace();
		}

		long t = System.currentTimeMillis();
		System.out.println("[RG - PBF FILE] Start reading of pbf file: (" + path + ")");
		readEdges(allGraphData, edges);
		readEdgeIndexes(allGraphData, indexEdges);
		// readRelations(allGraphData, relations);
		t = System.currentTimeMillis() - t;
		System.out
				.println("[RG - PBF FILE] Finished reading pbf file in: " + t + "milliseconds.");

	}

	/**
	 * This method saves the lists to a protobuf file, using GraphCreatorProtos.java
	 * 
	 * @param path
	 *            , where the file is located
	 * @param edges
	 *            the edges to be saved
	 * @param indexEdges
	 *            Mapping from source and target vertices to edges
	 */
	public static void saveToFile(String path,
			TIntObjectHashMap<CompleteEdge> edges,
			TObjectIntHashMap<String> indexEdges
			// ,TIntObjectHashMap<CompleteRelation> relations
			) {
		System.out.println("[RG - PBF FILE] Beginn to write RG into PBF file");
		long t = System.currentTimeMillis();
		AllGraphDataPBF.Builder allGraphData = AllGraphDataPBF.newBuilder();

		writeEdges(allGraphData, edges);

		writeEdgeIndexes(allGraphData, indexEdges);

		// writeRelations(allGraphData, relations);

		FileOutputStream output;
		try {
			output = new FileOutputStream(path);
			allGraphData.build().writeTo(output);
			t = System.currentTimeMillis() - t;
			System.out.println("[RG - PBF FILE]Finished writing RG to PBF in: " + t + "milliseconds");
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void readEdges(AllGraphDataPBF allGraphData,
			TIntObjectHashMap<CompleteEdge> edges) {

		edges.clear();
		for (CompleteEdgePBF ce_pbf : allGraphData.getAllEdgesList()) {

			THashSet<KeyValuePair> additionalTags = new THashSet<KeyValuePair>();

			for (KeyValuePairPBF kv_pbf : ce_pbf.getAdditionalTagsList()) {
				additionalTags.add(new KeyValuePair(kv_pbf.getValue(), kv_pbf.getKey()));
			}

			GeoCoordinate[] allWP = new GeoCoordinate[ce_pbf.getAllWaypointsCount()];

			for (int j = 0; j < allWP.length; j++) {
				GeoCoordinatePBF geo_pbf = ce_pbf.getAllWaypointsList().get(j);
				allWP[j] = new GeoCoordinate(geo_pbf.getLatitude(), geo_pbf.getLongitude());
			}
			// read nodes
			ArrayList<CompleteNode> allUsedNodes = new ArrayList<CompleteNode>();

			for (CompleteNodePBF node_pbf : ce_pbf.getAllUsedNodesList()) {
				GeoCoordinate coordinate = new GeoCoordinate(node_pbf.getCoordinate().getLatitude(),
						node_pbf.getCoordinate().getLongitude());
				HashSet<KeyValuePair> hs = new HashSet<KeyValuePair>();

				for (KeyValuePairPBF kv_pbf : node_pbf.getAdditionalTagsList()) {
					hs.add(new KeyValuePair(kv_pbf.getValue(), kv_pbf.getKey()));
				}

				allUsedNodes
						.add(new CompleteNode(node_pbf.getId(), coordinate, hs));
			}

			CompleteEdge ce = new CompleteEdge(
					ce_pbf.getId(),
					ce_pbf.getSourceID(),
					ce_pbf.getTargetID(),
					allWP,
					ce_pbf.getName(),
					ce_pbf.getType(),
					ce_pbf.getRoundabout(),
					ce_pbf.getIsOneWay(),
					ce_pbf.getRef(),
					ce_pbf.getDestination(),
					ce_pbf.getWeight(),
					additionalTags,
					allUsedNodes);

			edges.put(ce_pbf.getIndex(), ce);

		}

	}

	private static void readEdgeIndexes(AllGraphDataPBF allGraphData,
			TObjectIntHashMap<String> indexEdges) {

		indexEdges.clear();

		for (IndexEdgePBF ie_pbf : allGraphData.getAllEdgeIndexesList()) {
			indexEdges.put(ie_pbf.getScrTarget(), ie_pbf.getEdgeId());
		}

	}

	// private static void readRelations(AllGraphDataPBF allGraphData,
	// TIntObjectHashMap<CompleteRelation> relations) {
	//
	// relations.clear();
	//
	// int j = 0;
	// for (CompleteRelationPBF cr_pbf : allGraphData.getAllRelationsList()) {
	// RelationMember[] member = new RelationMember[cr_pbf.getMemberCount()];
	//
	// int i = 0;
	// for (RelationMemberPBF rm_pbf : cr_pbf.getMemberList()) {
	// EntityType memberType = null;
	//
	// if (rm_pbf.getMemberType() == MemberType.NODE)
	// memberType = EntityType.Node;
	// if (rm_pbf.getMemberType() == MemberType.WAY)
	// memberType = EntityType.Way;
	// if (rm_pbf.getMemberType() == MemberType.RELATION)
	// memberType = EntityType.Relation;
	//
	// member[i] = new RelationMember(rm_pbf.getMemberId(), memberType,
	// rm_pbf.getMemberRole());
	// i++;
	// }
	//
	// HashSet<KeyValuePair> additionalTags = new HashSet<KeyValuePair>();
	//
	// for (KeyValuePairPBF kv_PBF : cr_pbf.getTagsList()) {
	// additionalTags.add(new KeyValuePair(kv_PBF.getValue(), kv_PBF.getKey()));
	// }
	// CompleteRelation cr = new CompleteRelation(cr_pbf.get, member, additionalTags);
	// relations.put(j, cr);
	// j++;
	// }
	//
	// }

	private static void writeEdges(AllGraphDataPBF.Builder allGraphData,
			TIntObjectHashMap<CompleteEdge> edges) {

		for (TIntObjectIterator<CompleteEdge> it = edges.iterator(); it.hasNext();) {
			it.advance();
			CompleteEdge edge = it.value();
			CompleteEdgePBF.Builder ce_PBF = CompleteEdgePBF.newBuilder();

			ce_PBF.setId(edge.getId());
			ce_PBF.setSourceID(edge.getSourceId());
			ce_PBF.setTargetID(edge.getTargetId());
			if (edge.getName() != null)
				ce_PBF.setName(edge.getName());
			if (edge.getType() != null)
				ce_PBF.setType(edge.getType());
			ce_PBF.setRoundabout(edge.isRoundabout());
			ce_PBF.setIsOneWay(edge.isOneWay());
			if (edge.getRef() != null)
				ce_PBF.setRef(edge.getRef());
			if (edge.getDestination() != null)
				ce_PBF.setDestination(edge.getDestination());
			ce_PBF.setWeight(edge.getWeight());

			for (KeyValuePair kv : edge.getAdditionalTags()) {
				KeyValuePairPBF.Builder kv_PBF = KeyValuePairPBF.newBuilder().setKey(kv.getKey());
				kv_PBF.setValue(kv.getValue());
				ce_PBF.addAdditionalTags(kv_PBF);
			}

			for (GeoCoordinate geo : edge.getAllWaypoints()) {
				GeoCoordinatePBF.Builder geo_PBF = GeoCoordinatePBF.newBuilder();
				geo_PBF.setLatitude(geo.getLatitude());
				geo_PBF.setLongitude(geo.getLongitude());
				ce_PBF.addAllWaypoints(geo_PBF);
			}
			// write nodes
			for (CompleteNode node : edge.getAllUsedNodes()) {
				CompleteNodePBF.Builder node_PBF = CompleteNodePBF.newBuilder();
				node_PBF.setId(node.getId());

				GeoCoordinatePBF.Builder geo_PBF = GeoCoordinatePBF.newBuilder();
				geo_PBF.setLatitude(node.getCoordinate().getLatitude());
				geo_PBF.setLongitude(node.getCoordinate().getLongitude());
				node_PBF.setCoordinate(geo_PBF);

				for (KeyValuePair kv : node.getAdditionalTags()) {
					KeyValuePairPBF.Builder kv_PBF = KeyValuePairPBF.newBuilder();
					kv_PBF.setKey(kv.getKey());
					kv_PBF.setValue(kv.getValue());
					node_PBF.addAdditionalTags(kv_PBF);
				}

				ce_PBF.addAllUsedNodes(node_PBF);

			}

			ce_PBF.setIndex(it.key());

			allGraphData.addAllEdges(ce_PBF);
		}

	}

	private static void writeEdgeIndexes(Builder allGraphData,
			TObjectIntHashMap<String> indexEdges) {

		for (TObjectIntIterator<String> it = indexEdges.iterator(); it.hasNext();) {
			it.advance();
			IndexEdgePBF.Builder ie_PBF = IndexEdgePBF.newBuilder();

			ie_PBF.setScrTarget(it.key());
			ie_PBF.setEdgeId(indexEdges.get(it.key()));

			allGraphData.addAllEdgeIndexes(ie_PBF);
		}
	}

	// private static void writeRelations(AllGraphDataPBF.Builder allGraphData,
	// TIntObjectHashMap<CompleteRelation> relations) {
	//
	// for (TIntObjectIterator<CompleteRelation> it = relations.iterator(); it.hasNext();) {
	// it.advance();
	// CompleteRelation relation = it.value();
	// CompleteRelationPBF.Builder cr_PBF = CompleteRelationPBF.newBuilder();
	//
	// for (RelationMember rm : relation.getMember()) {
	// RelationMemberPBF.Builder rm_PBF =
	// RelationMemberPBF.newBuilder();
	//
	// rm_PBF.setMemberId(rm.getMemberId());
	// rm_PBF.setMemberRole(rm.getMemberRole());
	//
	// switch (rm.getMemberType()) {
	// case Node:
	// rm_PBF.setMemberType(MemberType.NODE);
	// break;
	// case Way:
	// rm_PBF.setMemberType(MemberType.WAY);
	// break;
	// case Relation:
	// rm_PBF.setMemberType(MemberType.RELATION);
	// break;
	// case Bound:
	// break;
	// }
	//
	// cr_PBF.addMember(rm_PBF);
	// }
	//
	// for (KeyValuePair kv : relation.getTags()) {
	// KeyValuePairPBF.Builder kv_PBF =
	// KeyValuePairPBF.newBuilder();
	// kv_PBF.setKey(kv.getKey());
	// kv_PBF.setValue(kv.getValue());
	// cr_PBF.addTags(kv_PBF);
	// }
	//
	// allGraphData.addAllRelations(cr_PBF);
	// }
	// }
}
