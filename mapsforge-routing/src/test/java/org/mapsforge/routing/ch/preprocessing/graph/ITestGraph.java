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
 * Interface for a graph, used for testing purposes.
 * 
 * @author Patrick Jungermann
 * @version $Id: ITestGraph.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public interface ITestGraph {

	/**
	 * Returns the pure graph object, which is needed by the (tested) methods.
	 * 
	 * @return The pure graph object.
	 */
	public Graph getGraph();

	/**
	 * Adds a directed edge with a given weight to the underlying graph object from the source to the
	 * target, both given by their identifiers.
	 * 
	 * @param sourceId
	 *            The source's identifier.
	 * @param targetId
	 *            The target's identifier.
	 * @param weight
	 *            The weight of the new edge.
	 * @return The new edge's identifier.
	 */
	public int addEdge(final int sourceId, final int targetId, final int weight);

	/**
	 * Adds a undirected or directed edge with a given weight to the underlying graph object from the
	 * source to the target, both given by their identifiers.
	 * 
	 * @param sourceId
	 *            The source's identifier.
	 * @param targetId
	 *            The target's identifier.
	 * @param weight
	 *            The weight of the new edge.
	 * @param undirected
	 *            Whether the edge should be undirected or not.
	 * @return The new edge's identifier.
	 */
	public int addEdge(final int sourceId, final int targetId, final int weight,
			final boolean undirected);

	/**
	 * Returns the minimal weight for all edge from the source to the target, both given by their
	 * identifiers.
	 * 
	 * @param sourceId
	 *            The source's identifier.
	 * @param targetId
	 *            The target's identifier.
	 * @return The minimal edge weight between source and target.
	 */
	public int getMinEdgeWeight(final int sourceId, final int targetId);
}
