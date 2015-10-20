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
package org.mapsforge.routing.ch.android;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * Implementation of the Contraction Hierarchies' query algorithm.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHAlgorithm.java 1662 2011-12-30 12:08:08Z Patrick.Jungermann@googlemail.com $
 */
class CHAlgorithm {

	/**
	 * The Contraction Hierarchies graph.
	 */
	protected final CHGraph graph;
	/**
	 * Flag and also index for the direction forward.
	 */
	protected final static int FORWARD = 0;
	/**
	 * Flag and also index for the direction backward.
	 */
	protected final static int BACKWARD = 1;
	/**
	 * Queue for handling the stall-on-demand related item. Not thread-safe! Will be used at
	 * {@link CHAlgorithm#search}. If a multi-threaded variant will be used, a local queue has to be
	 * used instead.
	 */
	private final ArrayDeque<QueueItem> stallQueue = new ArrayDeque<QueueItem>();

	/**
	 * Constructs an instance for the given Contraction Hierarchies graph, which will be used as basis
	 * for this query algorithm.
	 * 
	 * @param graph
	 *            The Contraction Hierarchies graph.
	 */
	public CHAlgorithm(final CHGraph graph) {
		this.graph = graph;
	}

	/**
	 * Searchs for the shortest path between the source and target and returns it, if there is any.
	 * 
	 * @param sourceId
	 *            The source of the path.
	 * @param targetId
	 *            The target of the path.
	 * @return The shortest path between source and target, if there is any.
	 * @throws IOException
	 *             if there was a problem with reading the required data.
	 */
	public LinkedList<CHEdge> getShortestPath(final int sourceId, final int targetId)
			throws IOException {
		final LinkedList<CHEdge> shortestPath = new LinkedList<CHEdge>();

		// TODO: bidirectional dijkstra to find the shortest path, if any (dual-threading possible)
		// TODO: 2 diff. impl. (dual- vs. single-threaded) with switch at CHRouter based on avail. CPUs?
		// or using a factory?

		int direction = FORWARD;
		final Queue[] queues = new Queue[] { new Queue(), new Queue() };
		final QueueItemMap[] discovered = new QueueItemMap[] { new QueueItemMap(), new QueueItemMap() };

		final QueueItem sourceItem = new QueueItem(0, sourceId, new CHEdge[0]);
		final QueueItem targetItem = new QueueItem(0, targetId, new CHEdge[0]);
		queues[FORWARD].add(sourceItem);
		queues[BACKWARD].add(targetItem);
		discovered[FORWARD].put(sourceId, sourceItem);
		discovered[BACKWARD].put(targetId, targetItem);

		int candidateId = -1;
		int candidateDistance = Integer.MAX_VALUE;
		while (!queues[FORWARD].isEmpty() || !queues[BACKWARD].isEmpty()) {
			final Queue queue = queues[direction];

			if (!queue.isEmpty()) {
				final QueueItemMap discoveredSelf = discovered[direction];
				final QueueItemMap discoveredOther = discovered[nextDirection(direction)];

				final int newCandidateId = search(direction, queue, discoveredSelf, discoveredOther,
						candidateDistance);
				if (newCandidateId != -1) {
					candidateId = newCandidateId;
					candidateDistance = discoveredSelf.get(candidateId).distance
							+ discoveredOther.get(candidateId).distance;
				}
			}

			direction = nextDirection(direction);
		}

		if (candidateId != -1) {
			int currentSourceId = sourceId;

			for (final int dir : new int[] { FORWARD, BACKWARD }) {
				for (final CHEdge edge : discovered[dir].get(candidateId).path) {
					if (edge.shortcut) {
						final CHEdge[] unpackedPath = graph.unpackShortcut(edge, currentSourceId);
						Collections.addAll(shortestPath, unpackedPath);

						currentSourceId = unpackedPath[unpackedPath.length - 1].getTargetId();

					} else {
						if (edge.getSourceId() != currentSourceId) {
							edge.switchSourceAndTarget();
						}
						shortestPath.add(edge);

						currentSourceId = edge.getTargetId();
					}
				}
			}
		}

		return shortestPath;
	}

	/**
	 * Returns the next direction, depending on the current direction.
	 * 
	 * @param currentDirection
	 *            The current direction.
	 * @return The next direction.
	 */
	protected static int nextDirection(final int currentDirection) {
		return currentDirection == FORWARD ? BACKWARD : FORWARD;
	}

	/**
	 * Returns all ingoing or outgoing edges, depending on the given direction.
	 * 
	 * @param vertexId
	 *            The vertex' identifier, for which all related edges are requested.
	 * @param direction
	 *            The current direction.
	 * @return All related edges, depending on the direction in ascending order by their weight.
	 * @throws java.io.IOException
	 *             if there was a problem with retrieving the edges.
	 */
	protected CHEdge[] getEdgesByDirection(final int vertexId, final int direction) throws IOException {
		return direction == FORWARD
				? graph.getOutgoingEdgesToHigherVertices(vertexId)
				: graph.getIngoingEdgesFromHigherVertices(vertexId);
	}

	/**
	 * Searchs for the next possible steps in the current direction.
	 * 
	 * @param direction
	 *            The current direction, in which the search has to be continued.
	 * @param queue
	 *            The queue, which contains all items, which have to be handled next, ordered by their
	 *            priority.
	 * @param discovered
	 *            Contains information about, if a vertex was already discovered, or not.
	 * @param discoveredByOther
	 *            Contains information about, if a vertex was already discovered by the other direction.
	 * @param candidateDistance
	 *            The current candidate distance for the current shortest path candidate.
	 * @return The new candidate vertex' identifier, or {@code -1}, if there is none.
	 * @throws IOException
	 *             if there was a problem with reading the required data.
	 */
	protected int search(final int direction, final Queue queue, final QueueItemMap discovered,
			final QueueItemMap discoveredByOther, int candidateDistance) throws IOException {
		int newCandidate = -1;

		// settle item
		final QueueItem item = queue.poll();

		if (item.stalled) {
			// settled, but no edges relaxed
			return newCandidate;
		}

		// search will be aborted in this direction
		// (distance of the smallest elem. is at least as large as the current candidate path's
		// distance)
		if (item.distance >= candidateDistance) {
			queue.clear();
			return newCandidate;
		}

		// new candidate?
		if (discoveredByOther.containsKey(item.id) && !discoveredByOther.get(item.id).stalled) {
			final int newDistance = discoveredByOther.get(item.id).distance + item.distance;
			if (newDistance < candidateDistance) {
				newCandidate = item.id;
			}
		}

		// stall-on-demand
		final CHEdge[] stallEdges = getEdgesByDirection(item.id, nextDirection(direction));
		for (final CHEdge stallEdge : stallEdges) {
			// the other vertex is always the higher vertex,
			// because we will always move to a higher level
			final int vertexId = stallEdge.getHighestVertexId();

			if (discovered.containsKey(vertexId)) {
				final int shorterDistance = discovered.get(vertexId).distance + stallEdge.weight;

				if (shorterDistance < item.distance) {
					// start a search for further nodes at the current vertex
					// only insert vertices with a sub-optimal path

					// only used as "stall distance", no path modification required!
					// stalled items will not be unstalled but will be replaced by a new item
					item.distance = shorterDistance;
					item.stalled = true;
					stallQueue.push(item);

					while (!stallQueue.isEmpty()) {
						final QueueItem stallItem = stallQueue.pop();
						final int stallDistance = stallItem.distance;

						final CHEdge[] edges = getEdgesByDirection(stallItem.id, direction);
						for (final CHEdge edge : edges) {
							final int stallVertexId = edge.getHighestVertexId();

							if (discovered.containsKey(stallVertexId)
									&& discovered.get(stallVertexId).stalled) {
								final int stallVertexDistance = stallDistance + edge.weight;

								// sub-optimal path found -> insert
								if (stallVertexDistance < discovered.get(stallVertexId).distance) {
									// add or decrease-key
									QueueItem stallVertexItem = discovered.get(stallVertexId);
									queue.remove(stallVertexItem);

									// only used as "stall distance", no path modification required!
									// stalled items will not be unstalled but will be replaced by a new
									// item
									stallVertexItem.distance = stallVertexDistance;
									stallVertexItem.stalled = true;

									queue.add(stallVertexItem);
									stallQueue.push(stallVertexItem);
								}
							}
						}
					}

					return newCandidate;
				}
			}
		}

		// relax all edges
		final CHEdge[] edges = getEdgesByDirection(item.id, direction);
		for (final CHEdge edge : edges) {
			// the other vertex is always the higher vertex,
			// because we will always move to a higher level
			final int vertexId = edge.getHighestVertexId();
			final boolean undiscovered = !discovered.containsKey(vertexId);

			// undiscovered OR
			if (undiscovered
					// (lower distance AND ...
					|| ((discovered.get(vertexId).distance > item.distance + edge.weight)
					// ... AND vertex not already settled [no item in the queue, no "decrease-key"
					// possible])
					&& queue.remove(discovered.get(vertexId)))) {

				// create path
				CHEdge[] path = new CHEdge[item.path.length + 1];
				if (direction == FORWARD) {
					System.arraycopy(item.path, 0, path, 0, item.path.length);
					path[path.length - 1] = edge;

				} else {
					path[0] = edge;
					System.arraycopy(item.path, 0, path, 1, item.path.length);
				}

				QueueItem newItem = new QueueItem(item.distance + edge.weight, vertexId, path);
				queue.add(newItem);
				discovered.put(newItem.id, newItem);

			} else {
				// will not be used anymore
				graph.poolEdges.release(edge);
			}
		}

		return newCandidate;
	}

	/**
	 * Needed as workaround for the generic array creation.
	 */
	private class QueueItemMap extends TIntObjectHashMap<QueueItem> {
	}

	/**
	 * Needed as workaround for the generic array creation.
	 */
	private class Queue extends PriorityQueue<QueueItem> {
	}

	/**
	 * Used to store the data for an item, which has to be handled in the future and which should be
	 * stored in a queue.
	 */
	private class QueueItem implements Comparable<QueueItem> {

		/**
		 * The distance of the path, to reach this item.
		 */
		protected int distance;
		/**
		 * The related vertex' identifier, which marks the last step of the path.
		 */
		protected final int id;
		/**
		 * The path, which leads to the related vertex.
		 */
		protected final CHEdge[] path;
		/**
		 * Whether the item was stalled or not.
		 */
		protected boolean stalled = false;

		/**
		 * Constructs a new item with the given distance, identifier and path.
		 * 
		 * @param distance
		 *            The new item's path's distance.
		 * @param id
		 *            The new item's related vertex' identifier.
		 * @param path
		 *            The new item's path to reach the related vertex.
		 */
		public QueueItem(final int distance, final int id, final CHEdge[] path) {
			this.distance = distance;
			this.id = id;
			this.path = path;
		}

		@Override
		public int compareTo(final QueueItem o) {
			if (o == null) {
				return -1;
			}
			else {
				// compareTo(distance, o.distance)
				return distance < o.distance ? -1 : (distance == o.distance ? 0 : 1);
			}
		}

		@Override
		public String toString() {
			return String.format("{id=%d, distance=%d, path.length=%d}", id, distance, path.length);
		}
	}
}
