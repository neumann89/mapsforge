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
package org.mapsforge.routing.ch.preprocessing.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A factory for test graphs.
 * 
 * @author Patrick Jungermann
 * @version $Id: TestGraphFactory.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class TestGraphFactory {

	/**
	 * Class-level logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(TestGraphFactory.class.getName());

	/**
	 * The default value for the maximum degree of a vertex.
	 */
	private static final int DEFAULT_MAX_DEGREE = 8;

	/**
	 * Creates a {@link TestGraph} with the given number of vertices and edges. All edges have a random
	 * weight, which is greater than zero.
	 * 
	 * @param numVertices
	 *            The number of vertices of the created graph.
	 * @param numEdges
	 *            The number of edges of the created graph.
	 * @return The created graph.
	 */
	public static TestGraph createGraph(final int numVertices, final int numEdges) {
		return createGraph(numVertices, numEdges, DEFAULT_MAX_DEGREE);
	}

	/**
	 * Creates a {@link TestGraph} with the given number of vertices and edges. All edges have a random
	 * weight, which is greater than zero. The degree of each vertex is limited to the given maximum.
	 * 
	 * @param numVertices
	 *            The number of vertices of the created graph.
	 * @param numEdges
	 *            The number of edges of the created graph.
	 * @param maxDegree
	 *            The maximum degree, used for all vertices.
	 * @return The created graph.
	 */
	public static TestGraph createGraph(final int numVertices, final int numEdges, final int maxDegree) {
		final TestGraph graph = new TestGraph(numVertices, numEdges);

		final int[][] weights = new int[numVertices][numVertices];
		final int[] degree = new int[numVertices];
		int sourceId, targetId, weight, i;

		HashMap<Integer, ArrayList<Integer>> neighbors = null;
		if (LOGGER.isLoggable(Level.FINEST)) {
			neighbors = new HashMap<Integer, ArrayList<Integer>>();
		}

		final Random rnd = new Random();
		for (i = 0; i < numEdges; i++) {
			weight = 0;

			while (weight == 0) {
				sourceId = rnd.nextInt(numVertices);
				targetId = rnd.nextInt(numVertices);

				if (sourceId != targetId && degree[sourceId] < maxDegree
						&& degree[targetId] < maxDegree) {
					if (weights[sourceId][targetId] == 0) {
						weight = weights[sourceId][targetId] = rnd.nextInt(100) + 1;
						degree[sourceId]++;
						degree[targetId]++;

						graph.addEdge(sourceId, targetId, weight);

						if (LOGGER.isLoggable(Level.FINEST)) {
							if (!neighbors.containsKey(sourceId)) {
								neighbors.put(sourceId, new ArrayList<Integer>());
							}
							neighbors.get(sourceId).add(targetId);

							if (!neighbors.containsKey(targetId)) {
								neighbors.put(targetId, new ArrayList<Integer>());
							}
							neighbors.get(targetId).add(sourceId);
						}
					}
				}
			}
		}

		if (LOGGER.isLoggable(Level.FINEST)) {
			for (Integer node : neighbors.keySet()) {
				LOGGER.finest("neighbors of " + node + ": " + neighbors.get(node));
			}
		}

		return graph;
	}

}
