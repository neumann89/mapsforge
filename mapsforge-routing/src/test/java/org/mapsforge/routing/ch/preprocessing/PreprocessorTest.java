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
package org.mapsforge.routing.ch.preprocessing;

import org.junit.Test;
import org.mapsforge.routing.ch.preprocessing.graph.ITestGraph;
import org.mapsforge.routing.ch.preprocessing.graph.TestGraph;
import org.mapsforge.routing.ch.preprocessing.graph.TestGraphFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Tests, related to the {@link Preprocessor} class.
 * 
 * @author Patrick Jungermann
 * @version $Id: PreprocessorTest.java 2054 2012-07-13 00:13:28Z Patrick.Jungermann@googlemail.com $
 */
public class PreprocessorTest {

	private static final Logger LOGGER = Logger.getLogger(PreprocessorTest.class.getName());

	@Test()
	public void isIndependent() {
		// prepare the test data
		PreprocessorSettings settings = new PreprocessorSettings();
		settings.kNeighborhood = 2;
		ITestGraph graph = new TestGraph(10, 10);
		graph.addEdge(0, 1, 1);
		graph.addEdge(0, 2, 1, true);
		graph.addEdge(5, 0, 1);
		graph.addEdge(1, 4, 1);
		graph.addEdge(1, 6, 1);
		graph.addEdge(2, 3, 1);
		graph.addEdge(7, 4, 1, true);
		graph.addEdge(8, 2, 1);
		graph.addEdge(8, 3, 1);
		graph.addEdge(9, 6, 1);
		float[] priorities = new float[] {
				5f, 3f, 7f, 2f, 9f, 3f, 4f, 6f, 3f, 1f
		};
		boolean[] processed = new boolean[] {
				false, false, false, false, false,
				false, false, false, false, false
		};

		boolean nodeIsIndependent = Preprocessor.isIndependent(settings, graph.getGraph(), 0,
				priorities, processed);
		assertFalse("Node 0 should depend on nodes 1, 3, 5, 6, 8 (lower priority).",
				nodeIsIndependent);

		processed[1] = true;
		nodeIsIndependent = Preprocessor.isIndependent(settings, graph.getGraph(), 0, priorities,
				processed);
		assertFalse("Node 0 should depend on nodes 3, 5, 6, 8 (lower priority).",
				nodeIsIndependent);

		processed[3] = true;
		nodeIsIndependent = Preprocessor.isIndependent(settings, graph.getGraph(), 0, priorities,
				processed);
		assertFalse("Node 0 should depend on nodes 5, 6, 8 (lower priority).", nodeIsIndependent);

		processed[5] = true;
		nodeIsIndependent = Preprocessor.isIndependent(settings, graph.getGraph(), 0, priorities,
				processed);
		assertFalse("Node 0 should depend on nodes 6, 8 (lower priority).", nodeIsIndependent);

		processed[6] = true;
		nodeIsIndependent = Preprocessor.isIndependent(settings, graph.getGraph(), 0, priorities,
				processed);
		assertFalse("Node 0 should depend on node 8 (lower priority).", nodeIsIndependent);

		processed[8] = true;
		nodeIsIndependent = Preprocessor.isIndependent(settings, graph.getGraph(), 0, priorities,
				processed);
		assertTrue("Node should have no dependencies.", nodeIsIndependent);

		settings.kNeighborhood = 3;
		nodeIsIndependent = Preprocessor.isIndependent(settings, graph.getGraph(), 0, priorities,
				processed);
		assertFalse("Node should depend on node 9.", nodeIsIndependent);
	}

	/**
	 * Tests the execution of the preprocessing, done at
	 * {@link Preprocessor#execute(org.mapsforge.routing.ch.preprocessing.graph.Graph)}
	 * .
	 */
	@Test
	public void execute() {
		ITestGraph graph = TestGraphFactory.createGraph(100, 300);
		final int[][] allPairsShortestPathsPre = getFloydWarshallAPSP(graph);
		try {
			new Preprocessor(new PreprocessorSettings()).execute(graph.getGraph());

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to execute the preprocessing - " + e.getClass().getName()
					+ ": " + e.getMessage(), e);
			fail("Failed to execute the preprocessing - " + e.getClass().getName() + ": "
					+ e.getMessage());
		}

		final int[][] allPairsShortestPathsPost = getFloydWarshallAPSP(graph);
		assertEquals(
				"The first dimensions of both all-pairs shortest paths results have to be the same.",
				allPairsShortestPathsPre.length, allPairsShortestPathsPost.length);

		for (int i = 0; i < allPairsShortestPathsPre.length; i++) {
			assertArrayEquals(
					"The \"length\" (sum of weights) of each path starting from source " + i
							+ " was expected to be the same after the preprocessing.",
					allPairsShortestPathsPre[i], allPairsShortestPathsPost[i]);
		}
	}

	/**
	 * Implementation of the Floyd-Warshall algorithm (all pairs shortest paths (APSP) algorithm), used
	 * for the validation of the {@link Preprocessor}'s result.
	 * 
	 * @param graph
	 *            The graph, for which the APSP result has to be calculated.
	 * @return The APSP result.
	 */
	private int[][] getFloydWarshallAPSP(final ITestGraph graph) {
		final int numVertices = graph.getGraph().getNumOfVertices();
		final int[][] path = new int[numVertices][numVertices];
		int i, j, k;

		// initialization
		for (i = 0; i < numVertices; i++) {
			for (j = 0; j < numVertices; j++) {
				// implicit: path[i][j] = 0;
				if (i != j) {
					path[i][j] = graph.getMinEdgeWeight(i, j);
				}
			}
		}

		for (k = 0; k < numVertices; k++) {
			for (i = 0; i < numVertices; i++) {
				for (j = 0; j < numVertices; j++) {
					path[i][j] = Math.min(path[i][j], boundedSum(path[i][k], path[k][j]));
				}
			}
		}

		return path;
	}

	/**
	 * Workaround for Java's sum calculation on {@link Integer} values, which will not respect its
	 * boundaries. If the sum would be greater than {@link Integer#MAX_VALUE} or lower than
	 * {@link Integer#MIN_VALUE}, these values will be returned.
	 * 
	 * @param a
	 *            First addend.
	 * @param b
	 *            Second addend.
	 * @return The sum of both addends, respecting the boundaries of {@link Integer}.
	 */
	private int boundedSum(final int a, final int b) {
		final int sum;

		if (a >= 0 && b >= 0) {
			sum = b > Integer.MAX_VALUE - a ? Integer.MAX_VALUE : a + b;

		} else if (a < 0 && b < 0) {
			sum = b < Integer.MIN_VALUE - a ? Integer.MIN_VALUE : a + b;

		} else {
			sum = a + b;
		}

		return sum;
	}

}
