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
package org.mapsforge.routing.preprocessing.data.clustering;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Collection;

import org.mapsforge.routing.preprocessing.data.ArrayUtils;

/**
 * This class implements a mapping from cluster to blocks and vice versa. This is analog to a
 * specialized double hash map. Additionally block ids can be reassigned by the swap operation.
 */
public final class ClusterBlockMapping {

	/**
	 * maps cluster to block ids.
	 */
	private final TObjectIntHashMap<Cluster> blockIds;
	/**
	 * All clusters of all levels. The array index defines the block id of each cluster.
	 * 
	 */
	private final Cluster[] clusters;

	/**
	 * Construct the bidirectional mapping between clusters and block ids.
	 * 
	 * @param clusterings
	 *            the clustering.
	 */
	public ClusterBlockMapping(Clustering[] clusterings) {
		// get number of clusters
		int n = 0;
		for (final Clustering clustering : clusterings) {
			n += clustering.size();
		}

		this.clusters = new Cluster[n];
		this.blockIds = new TObjectIntHashMap<Cluster>(10, 0.5f, -1);

		int blockId = 0;
		for (final Clustering clustering : clusterings) {
			for (Cluster cluster : clustering.getClusters()) {
				blockIds.put(cluster, blockId);
				clusters[blockId++] = cluster;
			}
		}
	}

	/**
	 * swaps the ids of the i-th and the j-th cluster.
	 * 
	 * @param i
	 *            the first cluster to be swapped.
	 * @param j
	 *            the second cluster to be swapped.
	 */
	public void swapBlockIds(final int i, final int j) {
		ArrayUtils.swap(clusters, i, j);
		blockIds.put(clusters[i], i);
		blockIds.put(clusters[j], j);
	}

	/**
	 * Maps the given cluster to block ids.
	 * 
	 * @param col
	 *            the clusters to be mapped to blcok ids.
	 * @return return the block ids in the same order as the given clusters.
	 */
	public int[] getBlockIds(final Collection<Cluster> col) {
		final int[] arr = new int[col.size()];

		int i = 0;
		for (Cluster c : col) {
			arr[i++] = getBlockId(c);
		}

		return arr;
	}

	/**
	 * Maps from cluster to the block id.
	 * 
	 * @param cluster
	 *            input for mapping. Must not be null.
	 * @return Returns the assigned block id.
	 */
	public int getBlockId(final Cluster cluster) {
		return blockIds.get(cluster);
	}

	/**
	 * Maps from block id to cluster.
	 * 
	 * @param blockId
	 *            must be valid.
	 * @return Returns the associated cluster.
	 */
	public Cluster getCluster(final int blockId) {
		return clusters[blockId];
	}

	/**
	 * @return Returns the number of clusters / blocks.
	 */
	public int size() {
		return clusters.length;
	}
}
