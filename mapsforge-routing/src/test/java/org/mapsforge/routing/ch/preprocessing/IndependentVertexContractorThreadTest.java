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

import org.junit.Before;
import org.junit.Test;
import org.mapsforge.routing.ch.preprocessing.graph.Graph;
import org.mapsforge.routing.ch.preprocessing.graph.TestGraph;
import org.mapsforge.routing.ch.preprocessing.simulation.ContractionSimulationResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to {@link IndependentVertexContractorThread}.
 * 
 * @author Patrick Jungermann
 * @version $Id: IndependentVertexContractorThreadTest.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class IndependentVertexContractorThreadTest {

    TestGraph graph;
    ContractionSimulationResult[] results;
    boolean[] processed;
    FakePool pool;

    @Before
    public void setUp() {
		// prepare the test data
		graph = new TestGraph(10, 20);
		graph.addEdge(1, 0, 10);// e0
		graph.addEdge(2, 0, 12);// e1
		graph.addEdge(0, 3, 3);// e2
		graph.addEdge(0, 4, 30);// e3
		graph.addEdge(5, 2, 1);// e4
		graph.addEdge(6, 2, 2);// e5
		graph.addEdge(2, 7, 3);// e6
		graph.addEdge(5, 7, 2);// e7

		results = new ContractionSimulationResult[] {
				new ContractionSimulationResult(), null, new ContractionSimulationResult()
		};
		results[0].shortcutEdgePairs = new int[][] { { 0, 2, 1 } };// e8 == sc1
		results[2].shortcutEdgePairs = new int[][] { { 4, 1, 1 }, { 5, 6, 1 } };// e9 == sc2 + e10 ==
																				// sc3
		processed = new boolean[] { false, false, false };

        pool = new FakePool(null, null, 1, 1);
        pool.setBatches(new int[][]{ new int[]{ 0 }, new int[]{ 2 } });
    }

	/**
	 * Test for the method {@link IndependentVertexContractorThread#contractVertices(int[])}.
	 */
	@Test
	public void contractVertices() {
		// prepare the test instance
		final IndependentVertexContractorThread thread = new IndependentVertexContractorThread(
				null, graph, null, results, processed);

		final int numEdgesPre = graph.getNumOfEdges();

		// contract vertices 0 and 2
		thread.contractVertices(new int[] { 0, 2 });

		// verify the result
		assertTrue("Only the contracted vertices should be marked as prepared.", processed[0]
                && !processed[1] && processed[2]);
		final int numShortcuts = results[0].shortcutEdgePairs.length
				+ results[2].shortcutEdgePairs.length;
		assertEquals("Wrong number of shortcuts.", numShortcuts, graph.getNumOfShortcuts());
		assertEquals("Wrong number of edges.", numEdgesPre + numShortcuts, graph.getNumOfEdges());

		assertEquals("Wrong weight for the fst shortcut.",
				graph.getWeightOfEdge(0) + graph.getWeightOfEdge(2), graph.getWeightOfEdge(8));
		assertEquals("Wrong weight for the snd shortcut.",
				graph.getWeightOfEdge(4) + graph.getWeightOfEdge(1), graph.getWeightOfEdge(9));
		assertEquals("Wrong weight for the trd shortcut.",
				graph.getWeightOfEdge(5) + graph.getWeightOfEdge(6), graph.getWeightOfEdge(10));

		assertEquals("Wrong number of original edges, represented by this shortcut.", 2,
				graph.getOriginalEdgeCountOfEdge(8));
		assertEquals("Wrong number of original edges, represented by this shortcut.", 2,
				graph.getOriginalEdgeCountOfEdge(9));
		assertEquals("Wrong number of original edges, represented by this shortcut.", 2,
				graph.getOriginalEdgeCountOfEdge(10));
	}

    @Test
    public void run() {
        final IndependentVertexContractorThread thread = new IndependentVertexContractorThread(
				pool, graph, null, results, processed);

        final int numEdgesPre = graph.getNumOfEdges();


		// run and contract all batches (= vertices 0 and 2)
        thread.run();

		// verify the result
		assertTrue("Only the contracted vertices should be marked as prepared.", processed[0]
                && !processed[1] && processed[2]);
		final int numShortcuts = results[0].shortcutEdgePairs.length
				+ results[2].shortcutEdgePairs.length;
		assertEquals("Wrong number of shortcuts.", numShortcuts, graph.getNumOfShortcuts());
		assertEquals("Wrong number of edges.", numEdgesPre + numShortcuts, graph.getNumOfEdges());

		assertEquals("Wrong weight for the fst shortcut.",
				graph.getWeightOfEdge(0) + graph.getWeightOfEdge(2), graph.getWeightOfEdge(8));
		assertEquals("Wrong weight for the snd shortcut.",
				graph.getWeightOfEdge(4) + graph.getWeightOfEdge(1), graph.getWeightOfEdge(9));
		assertEquals("Wrong weight for the trd shortcut.",
				graph.getWeightOfEdge(5) + graph.getWeightOfEdge(6), graph.getWeightOfEdge(10));

		assertEquals("Wrong number of original edges, represented by this shortcut.", 2,
				graph.getOriginalEdgeCountOfEdge(8));
		assertEquals("Wrong number of original edges, represented by this shortcut.", 2,
				graph.getOriginalEdgeCountOfEdge(9));
		assertEquals("Wrong number of original edges, represented by this shortcut.", 2,
				graph.getOriginalEdgeCountOfEdge(10));
    }

    /**
     * Fake class for {@link WorkerPool}.
     */
    class FakePool extends WorkerPool {
        int[][] batches = new int[0][];
        int pos = 0;

        /**
         * Constructor. Creates a worker pool with fixed size of the pool and a fixed number of batches.
         *
         * @param graph      The related {@link org.mapsforge.routing.ch.preprocessing.graph.Graph}.
         * @param settings   The settings, related to the preprocessing process.
         * @param poolSize   The size of the pool.
         * @param numBatches The number of batches.
         */
        public FakePool(final Graph graph, final PreprocessorSettings settings, final int poolSize, final int numBatches) {
            super(graph, settings, poolSize, numBatches);
        }

        /**
         * Sets the next batches.
         *
         * @param batches All batches.
         */
        public void setBatches(int[][] batches) {
            this.batches = batches;
        }

        /**
         * Returns the pre-configured batch.
         *
         * @return The pre-configured batch.
         */
        @Override
        public int[] getNextBatch() {
            return (pos < this.batches.length) ? this.batches[pos++] : new int[0];
        }
    }

}
