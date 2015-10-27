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

import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;
import org.mapsforge.routing.GeoCoordinate;
import org.mapsforge.routing.Rect;
import org.mapsforge.routing.preprocessing.data.ArrayUtils;
import org.mapsforge.routing.preprocessing.data.Edge;
import org.mapsforge.routing.preprocessing.data.Graph;
import org.mapsforge.routing.preprocessing.data.Vertex;

/**
 * Utility methods, related to clusters and clustering.
 * 
 * @author Patrick Jungermann
 * @version $Id: ClusterUtils.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class ClusterUtils {

	/**
	 * Returns the bounding box of this cluster, related to the given graph.<br/>
	 * The bounding box includes geographic coordinates of all related vertices and of all of their
	 * outgoing edges' waypoints.
	 * 
	 * @param cluster
	 *            The cluster, for which the bounding box is requested.
	 * @param graph
	 *            The graph, related to this cluster.
	 * @return The bounding box of this cluster.
	 */
	public static Rect getBoundingBox(final Cluster cluster, final Graph graph) {
		int minLongitude = Integer.MAX_VALUE;
		int maxLongitude = Integer.MIN_VALUE;
		int minLatitude = Integer.MAX_VALUE;
		int maxLatitude = Integer.MIN_VALUE;

		for (final int vertexId : cluster.getVertices()) {
			final Vertex vertex = graph.getVertex(vertexId);
			final GeoCoordinate coordinate = vertex.getCoordinate();

			minLongitude = Math.min(coordinate.getLongitudeE6(), minLongitude);
			maxLongitude = Math.max(coordinate.getLongitudeE6(), maxLongitude);
			minLatitude = Math.min(coordinate.getLatitudeE6(), minLatitude);
			maxLatitude = Math.max(coordinate.getLatitudeE6(), maxLatitude);

			for (final Edge edge : vertex.getOutboundEdges()) {
				for (final GeoCoordinate wpCoordinate : edge.getWaypoints()) {
					minLongitude = Math.min(wpCoordinate.getLongitudeE6(), minLongitude);
					maxLongitude = Math.max(wpCoordinate.getLongitudeE6(), maxLongitude);
					minLatitude = Math.min(wpCoordinate.getLatitudeE6(), minLatitude);
					maxLatitude = Math.max(wpCoordinate.getLatitudeE6(), maxLatitude);
				}
			}
		}

		return new Rect(minLongitude, maxLongitude, minLatitude, maxLatitude);
	}

	/**
	 * Sorts the clusters inside of the cluster-to-block mapping by their given sizes.
	 * 
	 * @param mapping
	 *            The cluster-to-block mapping, containing all the clusters, which have to be
	 *            rearranged.
	 * @param sizes
	 *            The size of each cluster.
	 */
	public static void sortBySize(final ClusterBlockMapping mapping, final int[] sizes) {
		new QuickSort().sort(new IndexedSortable() {

			@Override
			public void swap(final int i, final int j) {
				mapping.swapBlockIds(i, j);
				ArrayUtils.swap(sizes, i, j);
			}

			@Override
			public int compare(final int i, final int j) {
				return sizes[i] - sizes[j];
			}
		}, 0, mapping.size());
	}

	/**
	 * Returns the maximum of the number of vertices in one cluster.
	 * 
	 * @param clustering
	 *            The related clustering, containing all clusters.
	 * @return The maximum of the number of vertices in one cluster.
	 */
	public static int maxNumVerticesPerCluster(final Clustering clustering) {
		int max = 0;
		for (final Cluster cluster : clustering.getClusters()) {
			max = Math.max(max, cluster.size());
		}

		return max;
	}

	/**
	 * Returns the maximum of the number of vertices in one cluster.
	 * 
	 * @param clusterings
	 *            The related clustering, containing all clusters.
	 * @return The maximum of the number of vertices in one cluster.
	 */
	public static int maxNumVerticesPerCluster(final Clustering[] clusterings) {
		int max = 0;
		for (final Clustering clustering : clusterings) {
			max = Math.max(max, maxNumVerticesPerCluster(clustering));
		}

		return max;
	}
}
