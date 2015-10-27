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

import java.util.Iterator;

/**
 * Contraction Hierarchies graph.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHGraph.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public interface CHGraph extends org.mapsforge.routing.preprocessing.data.Graph {

	/**
	 * Returns the number of shortcut edges in this graph.
	 * 
	 * @return The number of shortcut edges in this graph.
	 */
	public int numShortcuts();// TODO: needed? remove?

	@Override
	public Iterator<? extends CHVertex> getVertices();

	@Override
	public CHVertex getVertex(int id);

	/**
	 * Returns all OSM street types. Each type's index within this array will be used by edges as
	 * reference to them.
	 * 
	 * @return All OSM street types.
	 */
	public String[] getOsmStreetTypes();
}
