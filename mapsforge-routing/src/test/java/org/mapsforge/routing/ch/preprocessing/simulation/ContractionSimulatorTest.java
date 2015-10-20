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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mapsforge.routing.ch.preprocessing.PreprocessorSettings;
import org.mapsforge.routing.ch.preprocessing.graph.Graph;
import org.mapsforge.routing.ch.preprocessing.graph.TestGraph;

/**
 * Tests for {@link ContractionSimulator}.
 * 
 * @author Patrick Jungermann
 * @version $Id: ContractionSimulatorTest.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class ContractionSimulatorTest {
	// TODO: add a test case to check, if the implementation ignores already removed vertices and their
	// edges!

	/**
	 * Tests the simulation of the contraction of one vertex.
	 */
	@Test
	public void testSimulate() {
		// prepare test data
		TestGraph testGraph = new TestGraph(17, 19);

		// tested vertex: 0
		// ingoing edges:
		int e0 = testGraph.addEdge(1, 0, 10); // e0: (u1, v)
		int e1 = testGraph.addEdge(2, 0, 4); // e1: (u2, v)
		int e2 = testGraph.addEdge(3, 0, 12); // e2: (u3, v)
		// outgoing edges:
		int e3 = testGraph.addEdge(0, 4, 7); // e3: (v, w1)
		int e4 = testGraph.addEdge(0, 5, 13); // e4: (v, w2)
		int e5 = testGraph.addEdge(0, 6, 27); // e5: (v, w3)
		int e6 = testGraph.addEdge(0, 7, 9); // e6: (v, w4)

		PreprocessorSettings settings = new PreprocessorSettings();
		settings.searchSpaceHopLimit = 4;
		boolean[] processedVertices = new boolean[17];

		int vertexId = 0;
		Graph graph = testGraph.getGraph();
		assertEquals("The vertex has three ingoing edges.", 3,
				graph.getIngoingEdgesOfVertex(vertexId).length);
		assertEquals("The vertex has three outgoing edges.", 4,
				graph.getOutgoingEdgesOfVertex(vertexId).length);

		ContractionSimulationResult result = ContractionSimulator.simulate(settings, graph, vertexId,
				processedVertices);
		assertNotNull("The simulation result was <null>.", result);
		assertEquals("The number of shortcuts, which have to be created, is not as expected.",
				12, result.numOfShortcuts);

		// another, shorter path from u2 to w3
		testGraph.addEdge(2, 8, 3); // e7
		testGraph.addEdge(8, 9, 4); // e8
		testGraph.addEdge(9, 10, 5); // e9
		testGraph.addEdge(10, 6, 2); // e10

		result = ContractionSimulator.simulate(settings, graph, vertexId, processedVertices);
		assertNotNull("The simulation result was <null>.", result);
		assertEquals("The number of shortcuts, which have to be created, is not as expected.",
				11, result.numOfShortcuts);

		// another path from u1 to w4 with the same "length" (sum of weights) as the path <u1, v, w4>
		testGraph.addEdge(1, 11, 8); // e11
		testGraph.addEdge(11, 12, 4); // e12
		testGraph.addEdge(12, 7, 7); // e13

		result = ContractionSimulator.simulate(settings, graph, vertexId, processedVertices);
		assertNotNull("The simulation result was <null>.", result);
		assertEquals("The number of shortcuts, which have to be created, is not as expected.",
				10, result.numOfShortcuts);

		// another, shorter path from u3 to w2, which is not part of the search space -> shortcut is
		// allowed
		testGraph.addEdge(3, 13, 6); // e14
		testGraph.addEdge(13, 14, 2); // e15
		testGraph.addEdge(14, 15, 3); // e16
		testGraph.addEdge(15, 16, 1); // e17
		testGraph.addEdge(16, 5, 5); // e18

		result = ContractionSimulator.simulate(settings, graph, vertexId, processedVertices);

		assertNotNull("The simulation result was <null>.", result);
		assertEquals("The number of shortcuts, which have to be created, is not as expected.",
				10, result.numOfShortcuts);
		assertEquals("There are 7 original edges, which are part of a shortcut.", 7,
				result.originalEdgeCountRemoved);
		assertEquals("The original edge count should be increased by 20.", 20,
				result.originalEdgeCountAdded);
		assertEquals("There were 7 removed edges.", 7, result.numOfRemovedEdges);
		assertArrayEquals(
				"Incorrect shortcut calculation.\n"
						+ java.util.Arrays.deepToString(result.shortcutEdgePairs) + "\n", new int[][] {
						{ e0, e3, 1 }, { e0, e4, 1 }, { e0, e5, 1 },
						{ e1, e3, 1 }, { e1, e4, 1 }, { e1, e6, 1 },
						{ e2, e3, 1 }, { e2, e4, 1 }, { e2, e5, 1 }, { e2, e6, 1 }
				}, result.shortcutEdgePairs);
	}

	/**
	 * Tests the simulation of the contraction of one vertex with undirected edges as part of the graph.
	 */
	@Test
	public void testSimulateWithUndirectedEdges() {
		// prepare test data
		TestGraph testGraph = new TestGraph(17, 19);

		// tested vertex: 0
		int[] ingoing = new int[2];
		int[] outgoing = new int[3];
		// ingoing edges:
		ingoing[0] = outgoing[0] = testGraph.addEdge(1, 0, 1, true); // e0 [undirected]: (u1, v) + (v,
																		// w1 = u1)
		ingoing[1] = testGraph.addEdge(2, 0, 1); // e1: (u2, v)
		// outgoing edges:
		outgoing[1] = testGraph.addEdge(0, 3, 3); // e2: (v, w2)
		outgoing[2] = testGraph.addEdge(0, 4, 5); // e3: (v, w3)

		PreprocessorSettings settings = new PreprocessorSettings();
		settings.searchSpaceHopLimit = 4;
		boolean[] processedVertices = new boolean[17];

		int vertexId = 0;
		Graph graph = testGraph.getGraph();
		assertArrayEquals("Different edges expected as ingoing edges.", ingoing,
				graph.getIngoingEdgesOfVertex(vertexId));
		assertArrayEquals("Different edges expected as outgoing edges.", outgoing,
				graph.getOutgoingEdgesOfVertex(vertexId));

		int[][] expectedShortcuts = new int[][] {
				{ 0, 2, 1 }, { 0, 3, 1 }, { 1, 0, 1 }, { 1, 2, 1 }, { 1, 3, 1 }
		};
		ContractionSimulationResult result = ContractionSimulator.simulate(settings, graph, vertexId,
				processedVertices);
		assertNotNull("The simulation result was <null>.", result);
		assertArrayEquals("Different shortcuts were expected.", expectedShortcuts,
				result.shortcutEdgePairs);
		assertEquals("There are 5 original edges, which are part of a shortcut.", 5,
				result.originalEdgeCountRemoved);
		assertEquals("The original edge count should be increased by 10.", 10,
				result.originalEdgeCountAdded);
		assertEquals("There were 5 removed edges.", 5, result.numOfRemovedEdges);

		// another, shorter path from u1 to w2 and path of same length for u2 to w2
		testGraph.addEdge(1, 3, 2); // e4

		expectedShortcuts = new int[][] {
				{ 0, 3, 1 }, { 1, 0, 1 }, { 1, 3, 1 }
		};
		result = ContractionSimulator.simulate(settings, graph, vertexId, processedVertices);
		assertNotNull("The simulation result was <null>.", result);
		assertArrayEquals("Different shortcuts were expected.", expectedShortcuts,
				result.shortcutEdgePairs);
		assertEquals("There are 4 original edges, which are part of a shortcut.", 4,
				result.originalEdgeCountRemoved);
		assertEquals("The original edge count should be increased by 6.", 6,
				result.originalEdgeCountAdded);
		assertEquals("There were 4 removed edges.", 4, result.numOfRemovedEdges);

		// another path from u1 to w3, but with too many hops
		testGraph.addEdge(1, 5, 1);
		testGraph.addEdge(5, 6, 1);
		testGraph.addEdge(6, 7, 1);
		testGraph.addEdge(7, 8, 1);
		testGraph.addEdge(8, 4, 1);

		result = ContractionSimulator.simulate(settings, graph, vertexId, processedVertices);
		assertNotNull("The simulation result was <null>.", result);
		assertArrayEquals("Different shortcuts were expected.", expectedShortcuts,
				result.shortcutEdgePairs);
		assertEquals("There are 4 original edges, which are part of a shortcut.", 4,
				result.originalEdgeCountRemoved);
		assertEquals("The original edge count should be increased by 6.", 6,
				result.originalEdgeCountAdded);
		assertEquals("There were 4 removed edges.", 4, result.numOfRemovedEdges);

		// another, shorter path from u2 to w3
		testGraph.addEdge(2, 6, 2);

		expectedShortcuts = new int[][] {
				{ 0, 3, 1 }, { 1, 0, 1 }
		};
		result = ContractionSimulator.simulate(settings, graph, vertexId, processedVertices);
		assertNotNull("The simulation result was <null>.", result);
		assertArrayEquals("Different shortcuts were expected.", expectedShortcuts,
				result.shortcutEdgePairs);
		assertEquals("There are 4 original edges, which are part of a shortcut.", 4,
				result.originalEdgeCountRemoved);
		assertEquals("The original edge count should be increased by 4.", 4,
				result.originalEdgeCountAdded);
		assertEquals("There were 7 removed edges.", 4, result.numOfRemovedEdges);
	}
}
