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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests related to {@link Graph}.
 * 
 * @author Patrick Jungermann
 * @version $Id: GraphTest.java 2054 2012-07-13 00:13:28Z Patrick.Jungermann@googlemail.com $
 */
public class GraphTest {

	/**
	 * Test related to the method {@link Graph#addShortcut(int, int, boolean)}.
	 */
	@Test
	public void addShortcut() {
		Graph graph = new Graph(4, 3);
		int e0 = graph.addEdge(0, 1, 10, false);
		int e1 = graph.addEdge(1, 2, 13, true);
		int e2 = graph.addEdge(3, 2, 12, false);

		int s0 = graph.addShortcut(e0, e1, false);
		assertEquals("The graph is expected to have 4 edges.", 4, graph.getNumOfEdges());
		assertEquals("The graph is expected to have 1 shortcut edge.", 1,
				graph.getNumOfShortcuts());
		assertEquals(
				"The shortcut should have the sum of both original edges' weights as weight.", 23,
				graph.getWeightOfEdge(s0));
		assertArrayEquals("Vertex 0 got the new shortcut as outgoing edge.",
				new int[] { e0, s0 }, graph.getOutgoingEdgesOfVertex(0));

		int s1 = graph.addShortcut(e2, e1, true);
		assertEquals("The graph is expected to have 5 edges.", 5, graph.getNumOfEdges());
		assertEquals("The graph is expected to have 2 shortcut edge.", 2,
				graph.getNumOfShortcuts());
		assertEquals(
				"The shortcut should have the sum of both original edges' weights as weight.", 25,
				graph.getWeightOfEdge(s1));
		assertArrayEquals("Vertex 3 got the new shortcut as outgoing edge.",
				new int[] { e2, s1 }, graph.getOutgoingEdgesOfVertex(3));
		assertArrayEquals("Vertex 1 got the new shortcut as outgoing edge.",
				new int[] { e1, s1 }, graph.getOutgoingEdgesOfVertex(1));
	}

	/**
	 * Test related to the method {@link Graph#addShortcut(int, int, boolean)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addShortcut_whenIllegalArguments_thenException() {
		Graph graph = new Graph(4, 2);
		int e0 = graph.addEdge(0, 1, 10, false);
		int e1 = graph.addEdge(3, 2, 12, false);

		graph.addShortcut(e0, e1, false);
	}

	/**
	 * Test related to the method {@link Graph#getHierarchyDepthOfVertex(int)}.
	 */
	@Test
	public void getHierarchyDepthOfVertex() {
		Graph graph = new Graph(4, 2);
		graph.addEdge(0, 1, 2, false);
		graph.addEdge(0, 2, 3, true);
		graph.addEdge(0, 3, 2, false);
		graph.addEdge(2, 3, 1, true);

		assertEquals("Should be initialized with <1>.", 1, graph.getHierarchyDepthOfVertex(0));
		assertEquals("Should be initialized with <1>.", 1, graph.getHierarchyDepthOfVertex(1));
		assertEquals("Should be initialized with <1>.", 1, graph.getHierarchyDepthOfVertex(2));
		assertEquals("Should be initialized with <1>.", 1, graph.getHierarchyDepthOfVertex(3));

		graph.updateHierarchyDepths(0);

		assertEquals("Should be unchanged.", 1, graph.getHierarchyDepthOfVertex(0));
		assertEquals("Should be updated to <2>.", 2, graph.getHierarchyDepthOfVertex(1));
		assertEquals("Should be updated to <2>.", 2, graph.getHierarchyDepthOfVertex(2));
		assertEquals("Should be updated to <2>.", 2, graph.getHierarchyDepthOfVertex(3));

		// invalid input
		assertEquals("<-1> expected as return value for an invalid vertex ID.", -1,
				graph.getHierarchyDepthOfVertex(10));
	}

	/**
	 * Test related to the method {@link Graph#getIngoingEdgesOfVertex(int)}.
	 */
	@Test
	public void getIngoingEdgesOfVertex() {
		Graph graph = new Graph(6, 10);
		int e0 = graph.addEdge(1, 0, 10, false);
		int e1 = graph.addEdge(2, 0, 4, false);
		int e2 = graph.addEdge(3, 0, 7, false);

		assertArrayEquals("Other edges expected here.", new int[] { e0, e1, e2 },
				graph.getIngoingEdgesOfVertex(0));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(1));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(2));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(3));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(4));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(5));
		assertArrayEquals("Empty array for invalid ID.", new int[0],
				graph.getIngoingEdgesOfVertex(6));

		int e3 = graph.addEdge(0, 4, 14, false);

		assertArrayEquals("Other edges expected here.", new int[] { e0, e1, e2 },
				graph.getIngoingEdgesOfVertex(0));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(1));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(2));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(3));
		assertArrayEquals("Other edges expected here.", new int[] { e3 },
				graph.getIngoingEdgesOfVertex(4));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(5));
		assertArrayEquals("Empty array for invalid ID.", new int[0],
				graph.getIngoingEdgesOfVertex(6));

		// undirected edge
		int e4 = graph.addEdge(0, 5, 13, true);

		assertArrayEquals("Other edges expected here.", new int[] { e0, e1, e2, e4 },
				graph.getIngoingEdgesOfVertex(0));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(1));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(2));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getIngoingEdgesOfVertex(3));
		assertArrayEquals("Other edges expected here.", new int[] { e3 },
				graph.getIngoingEdgesOfVertex(4));
		assertArrayEquals("Other edges expected here.", new int[] { e4 },
				graph.getIngoingEdgesOfVertex(5));
		assertArrayEquals("Empty array for invalid ID.", new int[0],
				graph.getIngoingEdgesOfVertex(6));
	}

	/**
	 * Test related to the method {@link Graph#getMinEdgeWeight(int, int)}.
	 */
	@Test
	public void getMinEdgeWeight() {
		Graph graph = new Graph(3, 5);

		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(0, 1));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(1, 0));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(0, 2));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(2, 0));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(1, 2));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(2, 1));

		graph.addEdge(0, 1, 10, false);
		graph.addEdge(0, 1, 7, false);
		graph.addEdge(0, 1, 13, false);

		assertEquals("<7> should be the min. weight.", 7, graph.getMinEdgeWeight(0, 1));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(1, 0));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(0, 2));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(2, 0));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(1, 2));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(2, 1));

		graph.addEdge(1, 2, 4, true);

		assertEquals("<7> should be the min. weight.", 7, graph.getMinEdgeWeight(0, 1));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(1, 0));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(0, 2));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(2, 0));
		assertEquals("<4> should be the min. weight.", 4, graph.getMinEdgeWeight(1, 2));
		assertEquals("<4> should be the min. weight.", 4, graph.getMinEdgeWeight(2, 1));

		graph.addEdge(2, 1, 2, false);

		assertEquals("<7> should be the min. weight.", 7, graph.getMinEdgeWeight(0, 1));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(1, 0));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(0, 2));
		assertEquals("There should be no edge.", Integer.MAX_VALUE, graph.getMinEdgeWeight(2, 0));
		assertEquals("<4> should be the min. weight.", 4, graph.getMinEdgeWeight(1, 2));
		assertEquals("<2> should be the min. weight.", 2, graph.getMinEdgeWeight(2, 1));
	}

	/**
	 * Test related to the method {@link Graph#getNumOfEdges()}.
	 */
	@Test
	public void getNumOfEdges() {
		Graph graph = new Graph(4, 10);
		assertEquals("No edges created so far.", 0, graph.getNumOfEdges());

		int e0 = graph.addEdge(0, 1, 10, false);
		assertEquals("Contains one directed edge.", 1, graph.getNumOfEdges());

		int e1 = graph.addEdge(1, 2, 5, true);
		assertEquals("Another undirected edge was added.", 2, graph.getNumOfEdges());

		graph.addShortcut(e0, e1, false);
		assertEquals("One shortcut was added.", 3, graph.getNumOfEdges());
	}

	/**
	 * Test related to the method {@link Graph#getNumOfShortcuts()}.
	 */
	@Test
	public void getNumOfShortcuts() {
		Graph graph = new Graph(4, 10);
		int e0 = graph.addEdge(0, 1, 10, false);
		int e1 = graph.addEdge(1, 2, 20, false);

		assertEquals("No shortcuts were added.", 0, graph.getNumOfShortcuts());

		graph.addShortcut(e0, e1, true);

		assertEquals("One shortcut was added.", 1, graph.getNumOfShortcuts());
	}

	/**
	 * Test related to the method {@link Graph#getNumOfVertices()}.
	 */
	@Test
	public void getNumOfVertices() {
		Graph graph = new Graph(4, 10);
		assertEquals("<4> expected.", 4, graph.getNumOfVertices());

		graph = new Graph(100, 300);
		assertEquals("<100> expected.", 100, graph.getNumOfVertices());
	}

	/**
	 * Test related to the method {@link Graph#getOriginalEdgeCountOfEdge(int)}.
	 */
	@Test
	public void getOriginalEdgeCountOfEdge() {
		Graph graph = new Graph(4, 2);
		int e0 = graph.addEdge(0, 1, 5, false);
		int e1 = graph.addEdge(1, 2, 6, true);
		int e2 = graph.addEdge(2, 3, 7, false);

		assertEquals("Should return <-1> for invalid edges.", -1,
				graph.getOriginalEdgeCountOfEdge(100));

		assertEquals("Should return <1> for the normal edge.", 1,
				graph.getOriginalEdgeCountOfEdge(e0));
		assertEquals("Should return <1> for the normal edge.", 1,
				graph.getOriginalEdgeCountOfEdge(e1));
		assertEquals("Should return <1> for the normal edge.", 1,
				graph.getOriginalEdgeCountOfEdge(e2));

		int s0 = graph.addShortcut(e0, e1, false);
		int s1 = graph.addShortcut(e1, e2, false);

		assertEquals("Should return <2> for the shortcut, made out of normal edges.", 2,
				graph.getOriginalEdgeCountOfEdge(s0));
		assertEquals("Should return <2> for the shortcut, made out of normal edges.", 2,
				graph.getOriginalEdgeCountOfEdge(s1));

		int s2 = graph.addShortcut(s0, e2, false);

		assertEquals(
				"Should return <3> for the shortcut, made out of one shortcut [2] and one normal edge [1].",
				3, graph.getOriginalEdgeCountOfEdge(s2));
	}

	/**
	 * Test related to the method {@link Graph#getOtherVertexOfEdge(int, int)}.
	 */
	@Test
	public void getOtherVertexOfEdge() {
		Graph graph = new Graph(3, 2);
		int e0 = graph.addEdge(0, 1, 2, false);
		int e1 = graph.addEdge(1, 2, 4, true);

		assertEquals("Should return <1> as the other end of the edge.", 1,
				graph.getOtherVertexOfEdge(e0, 0));
		assertEquals("Should return <0> as the other end of the edge.", 0,
				graph.getOtherVertexOfEdge(e0, 1));
		assertEquals("Should return <2> as the other end of the edge.", 2,
				graph.getOtherVertexOfEdge(e1, 1));
		assertEquals("Should return <1> as the other end of the edge.", 1,
				graph.getOtherVertexOfEdge(e1, 2));
		assertEquals("Should return <-1> for invalid edge IDs.", -1,
				graph.getOtherVertexOfEdge(100, 0));
		assertEquals(
				"Should return <0> as it was used as for the \"sourceId\" parameter, if the vertex ID is invalid.",
				0, graph.getOtherVertexOfEdge(e0, 100));
		assertEquals(
				"Should return <1> as it was used as for the \"sourceId\" parameter, if the vertex ID is invalid.",
				1, graph.getOtherVertexOfEdge(e1, 100));
	}

	/**
	 * Test related to the method {@link Graph#getOutgoingEdgesOfVertex(int)}.
	 */
	@Test
	public void getOutgoingEdgesOfVertex() {
		Graph graph = new Graph(6, 10);
		int e0 = graph.addEdge(0, 1, 10, false);
		int e1 = graph.addEdge(0, 2, 4, false);
		int e2 = graph.addEdge(0, 3, 7, false);

		assertArrayEquals("Other edges expected here.", new int[] { e0, e1, e2 },
				graph.getOutgoingEdgesOfVertex(0));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(1));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(2));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(3));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(4));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(5));
		assertArrayEquals("Empty array for invalid ID.", new int[0],
				graph.getOutgoingEdgesOfVertex(6));

		int e3 = graph.addEdge(4, 0, 14, false);

		assertArrayEquals("Other edges expected here.", new int[] { e0, e1, e2 },
				graph.getOutgoingEdgesOfVertex(0));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(1));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(2));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(3));
		assertArrayEquals("Other edges expected here.", new int[] { e3 },
				graph.getOutgoingEdgesOfVertex(4));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(5));
		assertArrayEquals("Empty array for invalid ID.", new int[0],
				graph.getOutgoingEdgesOfVertex(6));

		// undirected edge
		int e4 = graph.addEdge(5, 0, 13, true);

		assertArrayEquals("Other edges expected here.", new int[] { e0, e1, e2, e4 },
				graph.getOutgoingEdgesOfVertex(0));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(1));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(2));
		assertArrayEquals("Other edges expected here.", new int[0],
				graph.getOutgoingEdgesOfVertex(3));
		assertArrayEquals("Other edges expected here.", new int[] { e3 },
				graph.getOutgoingEdgesOfVertex(4));
		assertArrayEquals("Other edges expected here.", new int[] { e4 },
				graph.getOutgoingEdgesOfVertex(5));
		assertArrayEquals("Empty array for invalid ID.", new int[0],
				graph.getOutgoingEdgesOfVertex(6));
	}

	/**
	 * Test related to the method {@link Graph#getVertexIds()}.
	 */
	@Test
	public void getVertexIds() {
		Graph graph = new Graph(10, 20);

		assertArrayEquals("Wrong vertex identifiers delivered.", new int[] {
				0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, graph.getVertexIds());

		graph = new Graph(5, 10);

		assertArrayEquals("Wrong vertex identifiers delivered.", new int[] {
				0, 1, 2, 3, 4 }, graph.getVertexIds());
	}

	/**
	 * Test related to the method {@link Graph#getWeightOfEdge(int)}.
	 */
	@Test
	public void getWeightOfEdge() {
		Graph graph = new Graph(3, 2);
		int w0 = 5, w1 = 7;
		int e0 = graph.addEdge(0, 1, w0, false);
		int e1 = graph.addEdge(1, 2, w1, false);

		assertEquals("The weight of the first edge was not the expected one.", w0,
				graph.getWeightOfEdge(e0));
		assertEquals("The weight of the second edge was not the expected one.", w1,
				graph.getWeightOfEdge(e1));

		int s0 = graph.addShortcut(e0, e1, false);

		assertEquals("The weight of the shortcut was not the expected one.", w0 + w1,
				graph.getWeightOfEdge(s0));
	}

	/**
	 * Test related to the method {@link Graph#updateHierarchyDepth(int, int)}.
	 */
	@Test
	public void updateHierarchyDepth() {
		Graph graph = new Graph(4, 2);
		graph.addEdge(0, 1, 2, false);
		graph.addEdge(0, 2, 3, true);
		graph.addEdge(0, 3, 2, false);
		graph.addEdge(2, 3, 1, true);

		graph.updateHierarchyDepth(0, 2);

		assertEquals("Should be unchanged.", 1, graph.getHierarchyDepthOfVertex(0));
		assertEquals("Should be unchanged.", 1, graph.getHierarchyDepthOfVertex(1));
		assertEquals("Should be updated to <2>.", 2, graph.getHierarchyDepthOfVertex(2));
		assertEquals("Should be unchanged.", 1, graph.getHierarchyDepthOfVertex(3));

		graph.updateHierarchyDepth(2, 3);

		assertEquals("Should be unchanged", 1, graph.getHierarchyDepthOfVertex(0));
		assertEquals("Should be unchanged.", 1, graph.getHierarchyDepthOfVertex(1));
		assertEquals("Should be unchanged.", 2, graph.getHierarchyDepthOfVertex(2));
		assertEquals("Should be updated to <3>.", 3, graph.getHierarchyDepthOfVertex(3));

	}

	/***
	 * Test related to the method {@link Graph#updateHierarchyDepths(int)}.
	 */
	@Test
	public void updateHierarchyDepths() {
		Graph graph = new Graph(4, 2);
		graph.addEdge(0, 1, 2, false);
		graph.addEdge(0, 2, 3, true);
		graph.addEdge(0, 3, 2, false);
		graph.addEdge(2, 3, 1, true);

		graph.updateHierarchyDepths(0);

		assertEquals("Should be unchanged.", 1, graph.getHierarchyDepthOfVertex(0));
		assertEquals("Should be updated to <2>.", 2, graph.getHierarchyDepthOfVertex(1));
		assertEquals("Should be updated to <2>.", 2, graph.getHierarchyDepthOfVertex(2));
		assertEquals("Should be updated to <2>.", 2, graph.getHierarchyDepthOfVertex(3));

		graph.updateHierarchyDepths(2);

		assertEquals("Should be updated to <3>", 3, graph.getHierarchyDepthOfVertex(0));
		assertEquals("Should be unchanged.", 2, graph.getHierarchyDepthOfVertex(1));
		assertEquals("Should be unchanged.", 2, graph.getHierarchyDepthOfVertex(2));
		assertEquals("Should be updated to <3>.", 3, graph.getHierarchyDepthOfVertex(3));

	}
}
