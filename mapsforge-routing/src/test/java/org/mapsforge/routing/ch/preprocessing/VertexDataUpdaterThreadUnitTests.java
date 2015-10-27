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
import org.mapsforge.routing.ch.preprocessing.graph.ITestGraph;
import org.mapsforge.routing.ch.preprocessing.graph.TestGraphFactory;
import org.mapsforge.routing.ch.preprocessing.simulation.ContractionSimulationResult;

import static org.junit.Assert.assertEquals;

/**
 * Test related to {@link VertexDataUpdaterThread}.
 * 
 * @author Patrick Jungermann
 * @version $Id: VertexDataUpdaterThreadTest.java 1918 2012-03-13 19:15:41Z Patrick.Jungermann@googlemail.com $
 */
public class VertexDataUpdaterThreadUnitTests {

    ITestGraph graph;
    PreprocessorSettings settings;
    VertexDataUpdaterThread thread;
    ContractionSimulationResult simulationResult;
    int vertexId = 0;

    @Before
    public void setUp() {
        graph = TestGraphFactory.createGraph(100, 300);
        settings = new PreprocessorSettings();

        simulationResult = new ContractionSimulationResult();
        simulationResult.numOfShortcuts = 6;
        simulationResult.numOfRemovedEdges = 2;
        simulationResult.originalEdgeCountAdded = 4;
        simulationResult.originalEdgeCountRemoved = 4;

        // prepare the test instance
        thread = new VertexDataUpdaterThread(
                null, graph.getGraph(), settings, null, null, new boolean[0]);
    }


    @Test
    public void getPriority_positiveFactorForAllPriorityParts_returnWeightedPriority() {
        settings.edgeQuotientFactor = 2;
        settings.hierarchyDepthsFactor = 3;
        settings.originalEdgeQuotientFactor = 1;

        float priority = thread.getPriority(vertexId, simulationResult);
        assertEquals(10f, priority, 0f);
    }

    @Test
    public void getPriority_ignoreThEdgeQuotient_returnWeightedPriorityBasedOnTheOtherTwoParts() {
        settings.edgeQuotientFactor = 0;
        settings.hierarchyDepthsFactor = 3;
        settings.originalEdgeQuotientFactor = 1;

        float priority = thread.getPriority(vertexId, simulationResult);
        assertEquals(4f, priority, 0f);
    }

    @Test
    public void getPriority_ignoreTheHierarchyDepth_returnWeightedPriorityBasedOnTheOtherTwoParts() {
        settings.edgeQuotientFactor = 2;
        settings.hierarchyDepthsFactor = 0;
        settings.originalEdgeQuotientFactor = 1;

        float priority = thread.getPriority(vertexId, simulationResult);
        assertEquals(7f, priority, 0f);
    }

    @Test
    public void getPriority_ignoreTheOriginalEdgeQuotient_returnWeightedPriorityBasedOnTheOtherTwoParts() {
        settings.edgeQuotientFactor = 2;
        settings.hierarchyDepthsFactor = 3;
        settings.originalEdgeQuotientFactor = 0;

        float priority = thread.getPriority(vertexId, simulationResult);
        assertEquals(9f, priority, 0f);
    }

    @Test
    public void getPriority_higherOriginalEdgeCountForAddedOnes_returnWeightedPriority() {
        settings.edgeQuotientFactor = 2;
        settings.hierarchyDepthsFactor = 3;
        settings.originalEdgeQuotientFactor = 3;

        simulationResult.originalEdgeCountAdded = 5;

        float priority = thread.getPriority(vertexId, simulationResult);
        assertEquals(12.75f, priority, 0f);
    }

}
