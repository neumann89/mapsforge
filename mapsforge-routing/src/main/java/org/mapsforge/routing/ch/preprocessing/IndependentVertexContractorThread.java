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

import org.mapsforge.routing.ch.preprocessing.graph.Graph;
import org.mapsforge.routing.ch.preprocessing.simulation.ContractionSimulationResult;

/**
 * Thread for bulk contraction vertices.
 * 
 * @author Patrick Jungermann
 * @version $Id: IndependentVertexContractorThread.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
class IndependentVertexContractorThread extends AbstractWorkerThread {

	/**
	 * The simulation results of all vertices.
	 */
	private final ContractionSimulationResult[] simulationResults;
	/**
	 * All processed vertices.
	 */
	private final boolean[] processedVertices;

	/**
	 * Creates a new contractor thread as part of the given worker thread pool.
	 * 
	 * @param pool
	 *            The worker thread pool, from which this thread will receive its data batches.
	 * @param graph
	 *            The related graph.
	 * @param settings
	 *            The settings, related to the preprocessing process.
	 * @param simulationResults
	 *            The simulation results of all vertices, which will be updated.
	 * @param processedVertices
	 *            All processed vertices. Will be updated for each contracted vertex.
	 */
	public IndependentVertexContractorThread(final WorkerPool pool, final Graph graph,
			final PreprocessorSettings settings,
			final ContractionSimulationResult[] simulationResults,
			final boolean[] processedVertices) {
		super(pool, graph, settings);

		this.simulationResults = simulationResults;
		this.processedVertices = processedVertices;
	}

	/**
	 * Runs the thread and contracts each batch as long as the pool provides a new one.
	 */
	@Override
	public void run() {
		int[] batch;
		while (true) {
			batch = pool.getNextBatch();
			if (batch.length == 0) {
				break;
			}

			contractVertices(batch);
		}
	}

	/**
	 * Contracts all given vertices.
	 * 
	 * @param independentVertices
	 *            All independent vertices, which have to be contracted.
	 */
	protected void contractVertices(final int[] independentVertices) {
		// for each node in I: add necessary shortcuts (contraction)
		for (final int vertexId : independentVertices) {
			// add shortcuts
			for (final int[] shortcutEdgePair : simulationResults[vertexId].shortcutEdgePairs) {
				graph.addShortcut(shortcutEdgePair[0], shortcutEdgePair[1], shortcutEdgePair[2] == 2);
			}

			graph.updateHierarchyDepths(vertexId);

			processedVertices[vertexId] = true;
		}
	}
}
