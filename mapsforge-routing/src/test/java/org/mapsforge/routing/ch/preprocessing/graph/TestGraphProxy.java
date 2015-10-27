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
 * An implementation of the {@link ITestGraph} interface, which is a proxy for the underlying
 * {@link Graph}. This is needed to get access to some of its protected methods, which are needed for
 * testing purposes.<br/>
 * Using this proxy class, it is possible to use normal {@link Graph} instance (e.g., loaded from a JDBC
 * connection by using {@link Graph#loadGraph(java.sql.Connection)}) for testing purposes.
 * 
 * @author Patrick Jungermann
 * @version $Id: TestGraphProxy.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class TestGraphProxy implements ITestGraph {

	/**
	 * The underlying pure graph.
	 */
	private final Graph graph;

	/**
	 * Constructor. Creates an instance of this test graph, which is a proxy of the given graph.
	 * 
	 * @param graph
	 *            The pure graph, for which the proxy should be created.
	 */
	public TestGraphProxy(Graph graph) {
		this.graph = graph;
	}

	@Override
	public Graph getGraph() {
		return graph;
	}

	@Override
	public int addEdge(int sourceId, int targetId, int weight) {
		return addEdge(sourceId, targetId, weight, false);
	}

	@Override
	public int addEdge(int sourceId, int targetId, int weight, boolean undirected) {
		return graph.addEdge(sourceId, targetId, weight, undirected);
	}

	@Override
	public int getMinEdgeWeight(final int sourceId, final int targetId) {
		return graph.getMinEdgeWeight(sourceId, targetId);
	}
}
