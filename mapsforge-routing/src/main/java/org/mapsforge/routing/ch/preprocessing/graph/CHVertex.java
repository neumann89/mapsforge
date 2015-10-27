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

import org.mapsforge.routing.preprocessing.data.Vertex;

/**
 * A vertex related a Contraction Hierarchies graph.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHVertex.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public interface CHVertex extends Vertex {

	@Override
	public CHEdge[] getOutboundEdges();

	/**
	 * Returns all edges, which are outgoing edges to higher vertices or ingoing edges from higher
	 * vertices.
	 * 
	 * @return All edges, which are outgoing edges to higher vertices or ingoing edges from higher
	 *         vertices.
	 */
	public CHEdge[] getEdgesFromOrToHigherVertices();

	/**
	 * Returns the node's level. Edges will only lead to vertices with higher level.
	 * 
	 * @return The node's level.
	 */
	public int getLevel();
}
