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
package org.mapsforge.routing.ch.preprocessing.data;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.routing.preprocessing.data.ArrayUtils;

/**
 * Provides the possibility to combine paths.
 * 
 * @author Patrick Jungermann
 * @version $Id: PathCombiner.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class PathCombiner {

	/**
	 * Class-level logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(PathCombiner.class.getName());

	/**
	 * Contains each original path's start index at their related (combined) path.
	 */
	private TIntIntHashMap pathKeyToStartIndex;
	/**
	 * Contains each original path's (combined) path (identifier).
	 */
	private TIntIntHashMap pathKeyToPathId;
	/**
	 * Each (combined) path's related original path.
	 */
	private TIntObjectHashMap<int[]> pathIdToPathKey;
	/**
	 * Each path, accessible by their identifier.
	 */
	private TIntObjectHashMap<int[]> pathIdToPath;

	/**
	 * Contains the next path identifier, which can be used for new paths.
	 */
	private int nextPathId = 0;

	/**
	 * Statistical information of the path combinations.
	 */
	public final Stats stats = new Stats();

	/**
	 * Creates and initializes an object for combining paths.
	 * 
	 * @param paths
	 *            The paths, which has to be combined.
	 */
	public PathCombiner(final TIntObjectHashMap<int[]> paths) {
		final int[] pathKeys = paths.keys();
		pathKeyToStartIndex = new TIntIntHashMap(pathKeys.length);

		pathKeyToPathId = new TIntIntHashMap(pathKeys.length);
		pathIdToPathKey = new TIntObjectHashMap<int[]>(pathKeys.length);
		pathIdToPath = new TIntObjectHashMap<int[]>(pathKeys.length);

		for (int i = 0; i < pathKeys.length; i++) {
			final int pathKey = pathKeys[i];
			pathKeyToStartIndex.put(pathKey, 0);

			pathKeyToPathId.put(pathKey, i);
			pathIdToPathKey.put(i, new int[] { pathKey });
			pathIdToPath.put(i, paths.get(pathKey));
		}

		nextPathId = pathKeys.length;
	}

	/**
	 * Returns, whether statistical information should be collected or not.
	 * 
	 * @return Whether statistical information should be collected or not.
	 */
	protected boolean statsEnabled() {
		return LOGGER.isLoggable(Level.INFO);
	}

	/**
	 * Combines all paths as far as possible.
	 */
	public void combinePaths() {
		final TIntObjectHashMap<List<PathPair>> pathIdToPathPair = new TIntObjectHashMap<List<PathPair>>(
				pathIdToPath.size());
		final PriorityQueue<PathPair> queue = new PriorityQueue<PathPair>();

		// initialize
		initializeQueue(queue, pathIdToPathPair);

		// combine all paths as long as there are combinable paths
		while (!queue.isEmpty()) {
			handleNext(queue, pathIdToPathPair);
		}

		if (LOGGER.isLoggable(Level.FINE)) {
			final int numPathsPre = pathKeyToPathId.keys().length;
			final int numPathsPost = pathIdToPath.keys().length;
			int numEdgesPost = 0;
			for (final int key : pathIdToPath.keys()) {
				numEdgesPost += pathIdToPath.get(key).length;
			}

			LOGGER.fine(String
					.format(
							"reduced the number of paths from %d to %d (reduced by %d / %.2f%%)\n"
									+ "and the number of edges (sum of path lengths) from %d to %d (reduced by %d / %.2f%%)",
							numPathsPre, numPathsPost,
							stats.numOfPathCombinations, 100f * stats.numOfPathCombinations
									/ numPathsPre,
							numEdgesPost + stats.numOfSharedEdges, numEdgesPost,
							stats.numOfSharedEdges, 100f * stats.numOfSharedEdges
									/ (numEdgesPost + stats.numOfSharedEdges)
					));
		}
	}

	/**
	 * Initialize the priority queue, used to combine the most valuable paths at first.
	 * 
	 * @param queue
	 *            The queue of pairs of paths.
	 * @param pathIdToPathPair
	 *            The mapping path identifier to pair.
	 */
	protected void initializeQueue(final PriorityQueue<PathPair> queue,
			final TIntObjectHashMap<List<PathPair>> pathIdToPathPair) {
		int[] pathIds = pathIdToPath.keys();
		for (int i = 0; i < pathIds.length - 1; i++) {
			final int pathIdA = pathIds[i];
			final int[] pathA = pathIdToPath.get(pathIdA);

			for (int k = i + 1; k < pathIds.length; k++) {
				final int pathIdB = pathIds[k];
				final int[] pathB = pathIdToPath.get(pathIdB);

				addPair(pathIdA, pathA, pathIdB, pathB, queue, pathIdToPathPair);
			}
		}
	}

	/**
	 * Retrieves and handles the next item of the queue and updates everything after it.
	 * 
	 * @param queue
	 *            The queue of pairs of paths.
	 * @param pathIdToPathPair
	 *            The mapping path identifier to pair.
	 */
	protected void handleNext(final PriorityQueue<PathPair> queue,
			final TIntObjectHashMap<List<PathPair>> pathIdToPathPair) {

		final PathPair pair = queue.poll();
		final int[] pathA = pathIdToPath.get(pair.pathIdA);
		final int[] pathB = pathIdToPath.get(pair.pathIdB);
		final int[] pathAB = combinePaths(pathA, pathB, pair.index);
		final int pathIdAB = addAndRemove(pathAB, pair);

		if (statsEnabled()) {
			stats.numOfPathCombinations++;
			// pair.num == pathA.length + pathB.length - pathAB.length
			stats.numOfSharedEdges += pair.num;
		}

		// remove outdated pairs + find pathIds, which have to be updated
		final TIntHashSet updatablePathIds = new TIntHashSet();
		removeRelatedPairs(pair.pathIdA, pair, updatablePathIds, queue, pathIdToPathPair);
		removeRelatedPairs(pair.pathIdB, pair, updatablePathIds, queue, pathIdToPathPair);

		// update outdated pairs (some might be recreated for the new path, some of them not)
		for (final int updatablePathId : updatablePathIds.toArray()) {
			addPair(pathIdAB, pathAB, updatablePathId, pathIdToPath.get(updatablePathId), queue,
					pathIdToPathPair);
		}
	}

	/**
	 * Removes all pair, related to the given path (not the excluded one), and marks their further path
	 * as updatable.
	 * 
	 * @param excludedPair
	 *            The path, which should be ignored.
	 * @param pathId
	 *            The path's identifier, for which all related pairs have to be removed.
	 * @param updatablePathIds
	 *            The set of updatable paths.
	 * @param queue
	 *            The queue of pairs of paths.
	 * @param pathIdToPathPair
	 *            The mapping path identifier to pair.
	 */
	protected void removeRelatedPairs(final int pathId, final PathPair excludedPair,
			final TIntHashSet updatablePathIds, final PriorityQueue<PathPair> queue,
			final TIntObjectHashMap<List<PathPair>> pathIdToPathPair) {
		for (final PathPair relatedPair : pathIdToPathPair.remove(pathId)) {
			if (relatedPair != excludedPair) {
				if (relatedPair.pathIdA != pathId) {
					pathIdToPathPair.get(relatedPair.pathIdA).remove(relatedPair);
					queue.remove(relatedPair);

					updatablePathIds.add(relatedPair.pathIdA);

				} else if (relatedPair.pathIdB != pathId) {
					pathIdToPathPair.get(relatedPair.pathIdB).remove(relatedPair);
					queue.remove(relatedPair);

					updatablePathIds.add(relatedPair.pathIdB);
				}
			}
		}
	}

	/**
	 * Adds a pair of edges for both paths, if possible.
	 * 
	 * @param pathIdA
	 *            Path A's identifier.
	 * @param pathA
	 *            The path A.
	 * @param pathIdB
	 *            Path B's identifier.
	 * @param pathB
	 *            The path B.
	 * @param queue
	 *            The priority queue, to which new pairs have to be added.
	 * @param pathIdToPathPair
	 *            The mapping path identifier to pair, used for updates.
	 */
	protected void addPair(final int pathIdA, final int[] pathA, final int pathIdB, final int[] pathB,
			final PriorityQueue<PathPair> queue,
			final TIntObjectHashMap<List<PathPair>> pathIdToPathPair) {
		PathPair pair = createPair(pathIdA, pathA, pathIdB, pathB);
		if (pair == null) {
			pair = createPair(pathIdB, pathB, pathIdA, pathA);
		}

		if (pair != null) {
			if (!pathIdToPathPair.containsKey(pathIdA)) {
				pathIdToPathPair.put(pathIdA, new ArrayList<PathPair>());
			}
			if (!pathIdToPathPair.containsKey(pathIdB)) {
				pathIdToPathPair.put(pathIdB, new ArrayList<PathPair>());
			}
			pathIdToPathPair.get(pathIdA).add(pair);
			pathIdToPathPair.get(pathIdB).add(pair);
			queue.add(pair);
		}
	}

	/**
	 * Returns the next path identifier.
	 * 
	 * @return The next path identifier.
	 */
	protected int getNextPathId() {
		return nextPathId++;
	}

	/**
	 * Adds the new path, removes the old ones and updates the related data.
	 * 
	 * @param newPath
	 *            The new path.
	 * @param pair
	 *            The related pair of old paths.
	 * @return The new path's identifier.
	 */
	protected int addAndRemove(final int[] newPath, final PathPair pair) {
		// add new path
		final int newPathId = getNextPathId();
		pathIdToPath.put(newPathId, newPath);

		// get all related pathKeys for this new path
		final int[] pathKeysOfA = pathIdToPathKey.remove(pair.pathIdA);
		final int[] pathKeysOfB = pathIdToPathKey.remove(pair.pathIdB);
		final int[] pathKeys = new int[pathKeysOfA.length + pathKeysOfB.length];
		System.arraycopy(pathKeysOfA, 0, pathKeys, 0, pathKeysOfA.length);
		System.arraycopy(pathKeysOfB, 0, pathKeys, pathKeysOfA.length, pathKeysOfB.length);

		// update the pathId-pathKey relation
		pathIdToPathKey.put(newPathId, pathKeys);

		for (final int pathKey : pathKeys) {
			// update pathKey-pathId relation
			pathKeyToPathId.put(pathKey, newPathId);
		}
		for (final int pathKey : pathKeysOfA) {
			// update start index related to the new path
			// only needed for A's pathKeys, because B starts at index 0 at the new path
			pathKeyToStartIndex.put(pathKey, pathKeyToStartIndex.get(pathKey) + pair.index);
		}

		// remove old paths
		pathIdToPath.remove(pair.pathIdA);
		pathIdToPath.remove(pair.pathIdB);

		return newPathId;
	}

	/**
	 * Creates a new {@link PathPair}, if both paths can be combined (start of A is part of B).
	 * 
	 * @param pathIdA
	 *            Path A's identifier.
	 * @param pathA
	 *            The path A.
	 * @param pathIdB
	 *            Path B's identifier.
	 * @param pathB
	 *            The path B.
	 * @return The new {@link PathPair}, if they can be combined.
	 */
	protected PathPair createPair(final int pathIdA, final int[] pathA, final int pathIdB,
			final int[] pathB) {
		PathPair pair = null;

		int index = ArrayUtils.indexOf(pathA[0], pathB);
		if (index != -1) {
			int num = 1;

			int last = Math.min(pathA.length - 1, pathB.length - 1 - index);
			for (int i = 1; i <= last; i++) {
				if (pathA[i] != pathB[index + 1]) {
					// no combination possible
					num = 0;
					break;
				}

				num++;
			}

			if (num > 0) {
				pair = new PathPair(pathIdA, pathIdB, index, num);
			}
		}

		return pair;
	}

	/**
	 * Combines both paths to one new path and returns the combined path.
	 * 
	 * @param pathA
	 *            The first path A.
	 * @param pathB
	 *            The second path B.
	 * @param index
	 *            The index of the first item of A in B.
	 * @return The combined path.
	 */
	protected int[] combinePaths(final int[] pathA, final int[] pathB, int index) {
		if (index == -1) {
			return null;
		}

		// #items of B, not in A at the start: index
		// #items of A: pathA.length
		// #item of B, not in A at the end: pathB.length - index - pathA.length
		final int restOfB = pathB.length - index - pathA.length;
		final int newSize = index + pathA.length + (restOfB > 0 ? restOfB : 0);
		final int[] newPath = new int[newSize];

		int dstPos = 0;
		// items of B, not in A at the start
		System.arraycopy(pathB, 0, newPath, dstPos, index);
		dstPos += index;
		// items of A
		System.arraycopy(pathA, 0, newPath, dstPos, pathA.length);
		dstPos += pathA.length;
		// items of B, not in A at the end
		if (restOfB > 0) {
			System.arraycopy(pathB, index + pathA.length, newPath, dstPos, pathB.length - index
					- pathA.length);
		}

		return newPath;
	}

	/**
	 * Returns all path identifier.
	 * 
	 * @return All path identifier.
	 */
	public int[] getPathIds() {
		return pathIdToPath.keys();
	}

	/**
	 * Returns the path, related to the path identifier.
	 * 
	 * @param pathId
	 *            The path's identifier.
	 * @return The path.
	 */
	public int[] getPath(final int pathId) {
		return pathIdToPath.get(pathId);
	}

	/**
	 * Returns all path keys related to a specific path, grouped by their start index.
	 * 
	 * @param pathId
	 *            The path's identifier.
	 * @return All path keys related to a specific path, grouped by their start index.
	 */
	public int[][] getPathKeysByPathId(final int pathId) {
		final int[] path = pathIdToPath.get(pathId);
		final int[] pathKeys = pathIdToPathKey.get(pathId);

		final TIntObjectHashMap<TIntArrayList> indexToPathKeys = new TIntObjectHashMap<TIntArrayList>(
				path.length);
		for (final int pathKey : pathKeys) {
			final int index = pathKeyToStartIndex.get(pathKey);

			if (!indexToPathKeys.containsKey(index)) {
				indexToPathKeys.put(index, new TIntArrayList());
			}

			indexToPathKeys.get(index).add(pathKey);
		}

		final int[][] pathKeysPerIndex = new int[path.length][0];
		for (final int index : indexToPathKeys.keys()) {
			pathKeysPerIndex[index] = indexToPathKeys.get(index).toArray();
		}

		return pathKeysPerIndex;
	}

	/**
	 * A pair of two combinable paths.
	 * 
	 * @author Patrick Jungermann
	 * @version $Id: PathCombiner.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
	 */
	class PathPair implements Comparable<PathPair> {
		/**
		 * Path A's identifier.
		 */
		final int pathIdA;
		/**
		 * Path B's identifier.
		 */
		final int pathIdB;
		/**
		 * Index of the first element of pathA at pathB.
		 */
		final int index;
		/**
		 * The number of edges shared by both paths.
		 */
		final int num;

		/**
		 * Creates pair of two combinable paths.
		 * 
		 * @param pathIdA
		 *            Path A's identifier.
		 * @param pathIdB
		 *            Path B's identifier.
		 * @param index
		 *            The index of A's first item in B.
		 * @param num
		 *            The number of edges shared by both paths.
		 */
		public PathPair(final int pathIdA, final int pathIdB, final int index, final int num) {
			this.pathIdA = pathIdA;
			this.pathIdB = pathIdB;
			this.index = index;
			this.num = num;
		}

		@Override
		public int compareTo(PathPair o) {
			// in desc. order, null values last
			return o == null ? -1 : (num > o.num ? -1 : (num == o.num ? 0 : 1));
		}

		@Override
		public String toString() {
			return String.format("PathPair{%d,%d,%d,%d}", pathIdA, pathIdB, index, num);
		}

	}

	/**
	 * Contains the statistical information about the path combination process.
	 */
	class Stats {
		/**
		 * The number of path combinations. The total number of path is reduced by this number.
		 */
		int numOfPathCombinations = 0;
		/**
		 * Represents the number of shared edges during all combinations. The total number of edges will
		 * be reduced by this number.
		 */
		int numOfSharedEdges = 0;
	}
}
