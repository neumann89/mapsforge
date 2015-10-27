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
package org.mapsforge.routing.graph.creation.extraction.turnRestrictions;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.THashSet;
import org.mapsforge.routing.graph.creation.sql.SqlWriter;
import org.mapsforge.routing.graph.creation.extraction.ConfigObject;
import org.mapsforge.routing.graph.creation.extraction.KeyValuePair;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import java.util.List;

/**
 * This class handles the rare osm relations which were extracted by osmosis and transforms them into an
 * unified representation of turn restrictions. It handles the statistical information regarding the
 * TRs, too.
 * 
 * @author Robert Fels
 * 
 */
public class RestrictionExtractor {

	// --- could be a lot of data in ram ---
	// just needed to tackle redundant restriction data in osm
	/**
	 * all complete turn restrictions
	 */
	public THashSet<CompleteTurnRestriction> osmTrHs = new THashSet<CompleteTurnRestriction>();
	/**
	 * all turn restrictions
	 */
	public THashSet<TurnRestriction> realTrHs = new THashSet<TurnRestriction>();

	/**
	 * some restrictions are wrong tagged or not supported yet. This counter counts these ignored
	 * restrictions
	 */
	private int amountOfIgnoredRelations = 0;
	private int osmRelationsAmount = 0;

	private int TurnRestrictionsWrittenAmount = 0;
	// counter for new created TR (increments if there are more than one TR out of one relation)
	private int newCreatedTR = 0;

	private CompleteTurnRestriction currentTR;

	ConfigObject configObject;

	/**
	 * constructor for this extractor
	 *
	 * @param conf
	 *            configuration object
	 */
	public RestrictionExtractor(ConfigObject conf) {
		this.configObject = conf;
	}

	/**
	 * @param restrictions
	 *            all turn restrictions
	 * @param outgoingEdges
	 *            all outgoingEdges to all via nodes
	 * @param sqlWriter
	 *            Writer for creating the SQL statements.
	 */
	public void saveTurnRestrictions(TIntObjectHashMap<CompleteTurnRestriction> restrictions,
			TIntObjectHashMap<THashSet<Integer>> outgoingEdges, SqlWriter sqlWriter) {
		KeyValuePair noLeft = new KeyValuePair("no_left_turn", "restriction");
		KeyValuePair noRight = new KeyValuePair("no_right_turn", "restriction");
		KeyValuePair noU = new KeyValuePair("no_u_turn", "restriction");
		KeyValuePair noStraight = new KeyValuePair("no_straight_on", "restriction");
		KeyValuePair onlyLeft = new KeyValuePair("only_left_turn", "restriction");
		KeyValuePair onlyRight = new KeyValuePair("only_right_turn", "restriction");
		KeyValuePair onlyStraight = new KeyValuePair("only_straight_on", "restriction");

		amountOfIgnoredRelations += osmRelationsAmount - restrictions.size();
		osmRelationsAmount = restrictions.size();
		int tRCounter = 0;
		TIntObjectIterator<CompleteTurnRestriction> it = restrictions.iterator();
		while (it.hasNext()) {
			it.advance();
			CompleteTurnRestriction tr = it.value();
			// all tags of relation
			THashSet<KeyValuePair> trTags = tr.getTags();

			if ((trTags.contains(noLeft) || trTags.contains(noRight)
					|| trTags.contains(noU) || trTags.contains(noStraight))) {

				TurnRestriction nTR = new TurnRestriction(tRCounter, tr.getOsmId(), tr
						.getFromEdgeId(), tr
						.getViaNodeId(), tr.getToEdgeId());
				if (!realTrHs.contains(nTR)) {
					sqlWriter.addTurnRestriction(nTR);
					realTrHs.add(nTR);
					TurnRestrictionsWrittenAmount++;
					tRCounter++;
				}
			} else if (trTags.contains(onlyStraight)
					|| trTags.contains(onlyRight) || trTags.contains(onlyLeft)) {
				for (Integer edge_id : outgoingEdges.get(tr.getViaNodeId())) {
					if (edge_id != tr.getToEdgeId()) {
						TurnRestriction nTR = new TurnRestriction(tRCounter, tr.getOsmId(), tr
								.getFromEdgeId(), tr
								.getViaNodeId(), edge_id);
						if (!realTrHs.contains(nTR)) {
							sqlWriter.addTurnRestriction(nTR);
							realTrHs.add(nTR);
							TurnRestrictionsWrittenAmount++;
							newCreatedTR++;
							tRCounter++;
						}
					}
				}
			}
		}
	}

	/**
	 * @param rel
	 *            osm relation
	 * @param turnRId
	 *            turn restriction id
	 * @param viaId
	 *            via node id
	 * @return complete turn restriction
	 */
	public CompleteTurnRestriction transformRelationToComplTR(Relation rel, int turnRId, int viaId) {

		THashSet<KeyValuePair> tags = new THashSet<KeyValuePair>();

		addPairsForTagToHashSet(rel, tags);
		currentTR.setViaNodeId(viaId);
		currentTR.setId(turnRId);
		currentTR.setTags(tags);
		return currentTR;
	}

	/**
	 * @return turn restriction which is processed at the moment
	 */
	public CompleteTurnRestriction getCurrentTR() {
		return currentTR;
	}

	private void addPairsForTagToHashSet(Relation relation, THashSet<KeyValuePair> hs) {
		for (Tag tag : relation.getTags()) {
			if (configObject.containsRelationTag(tag.getKey(), tag.getValue()))
				hs.add(new KeyValuePair(tag.getValue(), tag.getKey()));
		}
	}

	/**
	 * Checks if a relation needs to be transformed into a turn restriction AND already saves the via
	 * node id
	 * 
	 * @param rel
	 *            relation to be checked
	 * @param usedWays
	 *            all used way ids
	 * @param usedNodes
	 *            all used nodes ids
	 * @return true or false
	 */
	// TODO: maybe remove usedWays and usedNodes
	public boolean isNeededRelation(Relation rel, THashSet<Long> usedWays, TLongIntHashMap usedNodes) {

		List<RelationMember> members = rel.getMembers();
		long viaId = -1;
		long fromId = -1;
		long toId = -1;

		boolean fromSet = false;
		boolean viaSet = false;
		boolean toSet = false;

		// check that a relation has only from, via and to member
		if (members.size() != 3) {
			amountOfIgnoredRelations++;
			return false;
		}

		// check if via node and from and to way are defined
		// TODO: could be extended to support via ways (complex turn restrictions)
		for (RelationMember relationMember : members) {
			if (relationMember.getMemberRole().equals("via")
					&& relationMember.getMemberType().name().equals("Way")) {
				amountOfIgnoredRelations++;
				return false;
			} else if (relationMember.getMemberRole().equals("from")
					&& relationMember.getMemberType().name().equals("Way")) {
				fromId = relationMember.getMemberId();
				if (usedWays.contains(fromId))
					fromSet = true;
			} else if (relationMember.getMemberRole().equals("to")
					&& relationMember.getMemberType().name().equals("Way")) {
				toId = relationMember.getMemberId();
				if (usedWays.contains(toId))
					toSet = true;
			} else if (relationMember.getMemberRole().equals("via")
					&& relationMember.getMemberType().name().equals("Node")) {
				viaId = relationMember.getMemberId();
				viaSet = true;
			}
		}

		if (viaSet && fromSet && toSet) {
			// set known osm value
			CompleteTurnRestriction osmTR = new CompleteTurnRestriction(-1, rel.getId(), -1,
					usedNodes.get(viaId), -1,
					fromId, toId, viaId, null);
			// check if TR is not redundant in osm data
			if (osmTrHs.contains(osmTR)) {
				amountOfIgnoredRelations++;
				return false;
			}

			currentTR = osmTR;
			osmTrHs.add(osmTR);
			osmRelationsAmount++;
			return true;
		}

		// not via, from and to members detected
		amountOfIgnoredRelations++;
		return false;
	}

	/**
	 * clean ram
	 */
	public void clean() {
		this.osmTrHs = null;
		this.realTrHs = null;
	}

	/**
	 *
	 * @return # of turn restrictions
	 */
	public int getTurnRestrictionsWrittenAmount() {
		return TurnRestrictionsWrittenAmount;
	}

	/**
	 *
	 * @return # of read osm relations
	 */
	public int getOsmRelationsAmount() {
		return osmRelationsAmount;
	}

	/**
	 *
	 * @return # created TR
	 */
	public int getNewCreatedTR() {
		return newCreatedTR;
	}

	/**
	 * 
	 * @return amount
	 */
	public int getAmountOfIgnoredRelations() {
		return amountOfIgnoredRelations;
	}
}