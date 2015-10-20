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

import org.mapsforge.routing.preprocessing.data.Edge;

/**
 * An edge related to Contraction Hierarchies graph.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHEdge.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public interface CHEdge extends Edge {

	/**
	 * Returns the edge's identifier.
	 * 
	 * @return The edge's identifier.
	 */
	public int getId();// TODO: needed? remove?

	@Override
	public CHVertex getSource();

	@Override
	public CHVertex getTarget();

	/**
	 * Returns the source's identifier.
	 * 
	 * @return The source's identifier.
	 */
	public int getSourceId();

	/**
	 * Returns the target's identifier.
	 * 
	 * @return The target's identifier.
	 */
	public int getTargetId();

	/**
	 * Returns the edge's source or target vertex' identifier, depending on which of them is at the
	 * higher level (zero at the top).
	 * 
	 * @return The identifier of the vertex with the highest level out of source and target.
	 */
	public int getHighestVertexId();

	/**
	 * Returns the edge's source or target vertex' identifier, depending on which of them is at the
	 * lower level (zero at the top).
	 * 
	 * @return The identifier of the vertex with the lowest level out of source and target.
	 */
	public int getLowestVertexId();

	/**
	 * Whether this edge is undirected or not.
	 * 
	 * @return Whether this edge is undirected or not.
	 */
	public boolean isUndirected();

	/**
	 * Returns the street name.
	 * 
	 * @return The street name.
	 */
	public String getName();

	/**
	 * Returns the reference name.
	 * 
	 * @return The reference name.
	 */
	public String getRef();

	/**
	 * Returns the street type reference (identifier), or {@code -1}, if it is unknown.
	 * 
	 * @return The street type reference (identifier), or {@code -1}, if it is unknown.
	 */
	public int getType();

	/**
	 * Returns, whether this edge is a roundabout or not.
	 * 
	 * @return whether this edge is a roundabout or not.
	 */
	public boolean isRoundabout();

	/**
	 * Returns, whether the edge is a shortcut, or not.
	 * 
	 * @return whether the edge is a shortcut, or not.
	 */
	public boolean isShortcut();

	/**
	 * Returns the bypassed vertex' identifier between source and target, if its a shortcut edge,
	 * otherwise {@code -1}.
	 * 
	 * @return The bypassed vertex' identifier, if its a shortcut edge, otherwise {@code -1}.
	 */
	public int getBypassedVertexId();

	/**
	 * Unpacks a shortcut edge. The result will not contain any shortcuts. If the edge is no shortcut,
	 * the result array contains only this element itself. The given source vertex will be used as the
	 * path's start.<br/>
	 * Be aware of undirected edges within the unpacked path. There might be no match between the target
	 * of one edge and the source of the next one.
	 * 
	 * @param startVertexId
	 *            The path's start.
	 * @return The unpacked path of the shortcut.
	 */
	public ShortcutPath unpack(final int startVertexId);
}
