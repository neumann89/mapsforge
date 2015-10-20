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

import gnu.trove.map.hash.TIntObjectHashMap;

import org.mapsforge.routing.ch.preprocessing.graph.Graph;
import org.mapsforge.routing.ch.preprocessing.simulation.ContractionSimulationResult;
import org.mapsforge.routing.ch.preprocessing.simulation.ContractionSimulator;

/**
 * Thread for bulk updating of the data of vertices.
 * 
 * @author Patrick Jungermann
 * @version $Id: VertexDataUpdaterThread.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
class VertexDataUpdaterThread extends AbstractWorkerThread {

	/**
	 * The simulation results of all vertices.
	 */
	private final ContractionSimulationResult[] simulationResults;
	/**
	 * The priorities of all vertices.
	 */
	private final float[] priorities;
	/**
	 * All processed vertices.
	 */
	private final boolean[] processedVertices;

	/**
	 * Creates a new updater thread as part of the given worker thread pool.
	 * 
	 * @param pool
	 *            The worker thread pool, from which this thread will receive its data batches.
	 * @param graph
	 *            The related graph.
	 * @param settings
	 *            The settings, related to the preprocessing process.
	 * @param simulationResults
	 *            The simulation results of all vertices, which will be updated.
	 * @param priorities
	 *            The priorities of all vertices, which will be updated.
	 * @param processedVertices
	 *            All processed vertices.
	 */
	public VertexDataUpdaterThread(final WorkerPool pool, final Graph graph,
			final PreprocessorSettings settings,
			final ContractionSimulationResult[] simulationResults,
			final float[] priorities, final boolean[] processedVertices) {
		super(pool, graph, settings);

		this.simulationResults = simulationResults;
		this.priorities = priorities;
		this.processedVertices = processedVertices;
	}

	/**
	 * Runs the thread and updates the data for each batch as long as the pool provides a new one.
	 */
	@Override
	public void run() {
		int[] batch;
		while (true) {
			batch = pool.getNextBatch();
			if (batch.length == 0) {
				break;
			}

			update(batch);
		}
	}

	/**
	 * Updates the data of all vertices of the batch.
	 * 
	 * @param batch
	 *            The batch of vertex identifiers.
	 */
	protected void update(final int[] batch) {
		final TIntObjectHashMap<ContractionSimulationResult> results = ContractionSimulator
				.simulateBatch(settings, graph, batch, processedVertices);

		ContractionSimulationResult result;
		for (final int id : batch) {
			result = results.get(id);
			simulationResults[id] = result;

			priorities[id] = getPriority(id, result);
		}
	}

	/**
	 * Returns the priority value for the given vertex.
	 * 
	 * @param vertexId
	 *            The vertex' identifier.
	 * @param simulationResult
	 *            The result of the last contraction simulation for this vertex.
	 * @return The priority value for the given vertex.
	 */
	protected float getPriority(final int vertexId, final ContractionSimulationResult simulationResult) {
		// used to prevent adding too many new edges
		final float edgeQuotient = simulationResult.numOfRemovedEdges == 0 ? 0f
				: simulationResult.numOfShortcuts / (float) simulationResult.numOfRemovedEdges;
		// used for a better uniformity of the contractions
		final float hierarchyDepth = graph.getHierarchyDepthOfVertex(vertexId);
		// used to get not too many shortcut levels, which would be bad for unpacking the path
		final float originalEdgeQuotient = simulationResult.originalEdgeCountRemoved == 0 ? 0f
				: simulationResult.originalEdgeCountAdded
						/ (float) simulationResult.originalEdgeCountRemoved;

		// return the weighted priority
		return settings.edgeQuotientFactor * edgeQuotient + settings.hierarchyDepthsFactor
				* hierarchyDepth + settings.originalEdgeQuotientFactor * originalEdgeQuotient;
	}
}
