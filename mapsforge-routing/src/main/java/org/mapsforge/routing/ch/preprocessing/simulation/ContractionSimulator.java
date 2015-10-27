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
package org.mapsforge.routing.ch.preprocessing.simulation;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import org.mapsforge.routing.ch.preprocessing.PreprocessorSettings;
import org.mapsforge.routing.ch.preprocessing.data.PriorityQueueItem;
import org.mapsforge.routing.ch.preprocessing.graph.Graph;

/**
 * Simulator for vertex contractions.
 * 
 * @author Patrick Jungermann
 * @version $Id: ContractionSimulator.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class ContractionSimulator {

	/**
	 * Simulates the contraction for each of the vertices.
	 * 
	 * @param settings
	 *            The preprocessing's settings.
	 * @param graph
	 *            The related graph.
	 * @param vertices
	 *            The batch of vertex identifiers.
	 * @param processedVertices
	 *            All processed vertices.
	 * @return The result of the simulations.
	 */
	public static TIntObjectHashMap<ContractionSimulationResult> simulateBatch(
			final PreprocessorSettings settings, final Graph graph, final int[] vertices,
			final boolean[] processedVertices) {

		final TIntObjectHashMap<ContractionSimulationResult> map = new TIntObjectHashMap<ContractionSimulationResult>(
				vertices.length, 0.5f, -1);

		for (final int vertexId : vertices) {
			map.put(vertexId, simulate(settings, graph, vertexId, processedVertices));
		}

		return map;
	}

	/**
	 * Simulates the contraction of the vertex.
	 * 
	 * @param settings
	 *            The preprocessing's settings.
	 * @param graph
	 *            The related graph.
	 * @param vertexId
	 *            The vertex' identifier.
	 * @param processedVertices
	 *            All processed vertices.
	 * @return The result of the simulation.
	 */
	public static ContractionSimulationResult simulate(final PreprocessorSettings settings,
			final Graph graph, final int vertexId, final boolean[] processedVertices) {
		final ContractionSimulationResult result = new ContractionSimulationResult();

		final int[] ingoingEdges = graph.getIngoingEdgesOfVertex(vertexId);
		final int numIngoingEdges = ingoingEdges.length;
		final int[] outgoingEdges = graph.getOutgoingEdgesOfVertex(vertexId);
		final int numOutgoingEdges = outgoingEdges.length;

		final boolean[] ingoingEdgesWithShortcuts = new boolean[numIngoingEdges];
		final boolean[] outgoingEdgesWithShortcuts = new boolean[numOutgoingEdges];

		final TIntObjectHashMap<TIntIntHashMap> sndEdgesPerFstEdge = new TIntObjectHashMap<TIntIntHashMap>();

		boolean shortcutNecessary;
		int ingoingEdgeId, outgoingEdgeId, sourceId, targetId, weightIngoing, weightOutgoing;
		for (int i = 0; i < numIngoingEdges; i++) {
			ingoingEdgeId = ingoingEdges[i];
			sourceId = graph.getOtherVertexOfEdge(ingoingEdgeId, vertexId);

			// vertex and it's edges have been already removed?
			if (!processedVertices[sourceId]) {
				weightIngoing = graph.getWeightOfEdge(ingoingEdgeId);

				for (int o = 0; o < numOutgoingEdges; o++) {
					outgoingEdgeId = outgoingEdges[o];
					targetId = graph.getOtherVertexOfEdge(outgoingEdgeId, vertexId);

					// vertex and it's edges have been already removed?
					if (!processedVertices[targetId]) {
						weightOutgoing = graph.getWeightOfEdge(outgoingEdgeId);

						// not the same? - cycles not allowed
						if (sourceId != targetId) {
							shortcutNecessary = !hasPathShorterEqualThan(
									weightIngoing + weightOutgoing,
									settings.searchSpaceHopLimit, graph, sourceId, targetId,
									ingoingEdgeId,
									outgoingEdgeId, processedVertices);

							if (shortcutNecessary) {
								// undirected shortcut possible?
								if (sndEdgesPerFstEdge.containsKey(outgoingEdgeId)
										&& sndEdgesPerFstEdge.get(outgoingEdgeId).containsKey(
												ingoingEdgeId)) {
									// undirected shortcut edge
									sndEdgesPerFstEdge.get(outgoingEdgeId).put(ingoingEdgeId, 2);

								} else {
									// (maybe temp.) directed shortcut edge
									TIntIntHashMap fstSnd;
									if (sndEdgesPerFstEdge.containsKey(ingoingEdgeId)) {
										fstSnd = sndEdgesPerFstEdge.get(ingoingEdgeId);
									} else {
										fstSnd = new TIntIntHashMap();
										sndEdgesPerFstEdge.put(ingoingEdgeId, fstSnd);
									}
									if (fstSnd.containsKey(outgoingEdgeId)) {
										fstSnd.put(outgoingEdgeId,
												Math.max(1, fstSnd.get(outgoingEdgeId)));
									} else {
										fstSnd.put(outgoingEdgeId, 1);
									}
								}

								// undirected shortcuts will be counted twice
								result.numOfShortcuts++;
								result.originalEdgeCountAdded += graph
										.getOriginalEdgeCountOfEdge(ingoingEdgeId)
										+ graph.getOriginalEdgeCountOfEdge(outgoingEdgeId);

								if (!ingoingEdgesWithShortcuts[i]) {
									ingoingEdgesWithShortcuts[i] = true;
									result.numOfRemovedEdges++;
									result.originalEdgeCountRemoved += graph
											.getOriginalEdgeCountOfEdge(ingoingEdgeId);
								}

								if (!outgoingEdgesWithShortcuts[o]) {
									outgoingEdgesWithShortcuts[o] = true;
									result.numOfRemovedEdges++;
									result.originalEdgeCountRemoved += graph
											.getOriginalEdgeCountOfEdge(outgoingEdgeId);
								}
							}
						}
					}
				}
			}
		}

		final List<int[]> shortcutEdgePairList = new ArrayList<int[]>();
		final int[] e1Keys = sndEdgesPerFstEdge.keys();
		Arrays.sort(e1Keys);

		for (final int edge1 : e1Keys) {
			final TIntIntHashMap sndEdges = sndEdgesPerFstEdge.get(edge1);
			final int[] e2Keys = sndEdges.keys();
			Arrays.sort(e2Keys);

			for (final int edge2 : e2Keys) {
				shortcutEdgePairList.add(new int[] { edge1, edge2, sndEdges.get(edge2) });
			}
		}

		result.shortcutEdgePairs = shortcutEdgePairList
				.toArray(new int[shortcutEdgePairList.size()][]);

		return result;
	}

	/**
	 * Dijkstra-based implementation for checking, if there is any other edge within a specified search
	 * space, which is shorter or equal than a given limit.
	 * 
	 * @param weightSumLimit
	 *            The limit for the paths.
	 * @param maxHops
	 *            The maximum of hops, used to limit the search space.
	 * @param graph
	 *            The related
	 *            {@link org.mapsforge.routing.ch.preprocessing.graph.Graph}.
	 * @param sourceId
	 *            The source's identifier of the searched path.
	 * @param targetId
	 *            The target's identifier of the searched path.
	 * @param excludedEdgeId1
	 *            The first excluded edge's identifier. A path of the first and followed by the second
	 *            excluded edge is not a valid one.
	 * @param excludedEdgeId2
	 *            The second excluded edge's identifier. A path of the first and followed by the second
	 *            excluded edge is not a valid one.
	 * @param processedVertices
	 *            All processed vertices.
	 * @return {@code true}, if there was suitable path, otherwise {@code false}.
	 */
	protected static boolean hasPathShorterEqualThan(final int weightSumLimit, final int maxHops,
			final Graph graph, final int sourceId, final int targetId, final int excludedEdgeId1,
			final int excludedEdgeId2, final boolean[] processedVertices) {

		final PriorityQueue<PriorityQueueItem<Integer, Payload>> queue = new PriorityQueue<PriorityQueueItem<Integer, Payload>>();
		final TIntObjectHashMap<PriorityQueueItem<Integer, Payload>> discovered = new TIntObjectHashMap<PriorityQueueItem<Integer, Payload>>();

		PriorityQueueItem<Integer, Payload> item = new PriorityQueueItem<Integer, Payload>(sourceId, 0);
		item.setPayLoad(new Payload());
		discovered.put(sourceId, item);
		queue.add(item);

		PriorityQueueItem<Integer, Payload> targetItem;
		Payload payload;
		int edgeSourceId, edgeTargetId, weightSum;
		while (!queue.isEmpty()) {
			item = queue.poll();

			payload = item.getPayload();
			if (payload.hops + 1 <= maxHops) {
				edgeSourceId = item.getId();

				for (final int edgeId : graph.getOutgoingEdgesOfVertex(edgeSourceId)) {
					// ignore the original path (excludedEdge1, excludedEdge2)
					if (payload.edge1 != excludedEdgeId1
							|| payload.edge2 != -1
							|| edgeId != excludedEdgeId2) {
						weightSum = item.getPriority() + graph.getWeightOfEdge(edgeId);

						edgeTargetId = graph.getOtherVertexOfEdge(edgeId, edgeSourceId);
						if (edgeTargetId == targetId && weightSum <= weightSumLimit) {
							return true;
						}

						if (!processedVertices[edgeTargetId]) {
							targetItem = discovered.get(edgeTargetId);
							if (targetItem == null) {
								if (weightSum <= weightSumLimit) {
									targetItem = new PriorityQueueItem<Integer, Payload>(edgeTargetId,
											weightSum);
									targetItem.setPayLoad(new Payload(payload, edgeId));
									discovered.put(edgeTargetId, targetItem);
									queue.add(targetItem);

								} else {
									discovered.put(edgeTargetId, null);
								}

							} else if (targetItem.getPriority() > weightSum) {
								// update priority
								queue.remove(targetItem);
								targetItem.setPriority(weightSum);
								targetItem.setPayLoad(new Payload(payload, edgeId));
								queue.add(targetItem);
							}
						}
					}
				}
			}
		}

		// no path found
		return false;
	}

	/**
	 * Payload used for a queue item to store some meta information about the item.
	 */
	static class Payload {
		/**
		 * The number of hops used to reach the related item.
		 */
		final int hops;
		/**
		 * The first edge of the path to the related item.
		 */
		int edge1 = -1;
		/**
		 * The second edge of the path to the related item.
		 */
		int edge2 = -1;

		/**
		 * Constructs a payload object for a start item.
		 */
		public Payload() {
			this.hops = 0;
		}

		/**
		 * Constructs a follower item reached via the edge for the given payload.
		 * 
		 * @param payload
		 *            The previous payload.
		 * @param edge
		 *            The used edge to reach the related item.
		 */
		public Payload(final Payload payload, final int edge) {
			hops = payload.hops + 1;

			if (payload.edge1 == -1) {
				edge1 = edge;
				edge2 = -1;

			} else {
				edge1 = payload.edge1;
				edge2 = payload.edge2 != -1 ? payload.edge2 : edge;
			}
		}
	}
}
