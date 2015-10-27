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

/**
 * Implementation of the {@link ITestGraph} interface, which simply extends the {@link Graph} class.
 * 
 * @author Patrick Jungermann
 * @version $Id: TestGraph.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class TestGraph extends Graph implements ITestGraph {

	/**
	 * Constructor. Creates a new instance.
	 * 
	 * @param numVertices
	 *            The number of vertices, which belong to this graph.
	 * @param numEdges
	 *            The number of edges, which belong to this graph.
	 */
	public TestGraph(final int numVertices, final int numEdges) {
		super(numVertices, numEdges);
	}

	@Override
	public Graph getGraph() {
		return this;
	}

	@Override
	public int addEdge(final int sourceId, final int targetId, final int weight) {
		return addEdge(sourceId, targetId, weight, false);
	}

	@Override
	public int addEdge(final int sourceId, final int targetId, final int weight,
			final boolean undirected) {
		return super.addEdge(sourceId, targetId, weight, undirected);
	}

	@Override
	public int getMinEdgeWeight(final int sourceId, final int targetId) {
		return super.getMinEdgeWeight(sourceId, targetId);
	}
}
