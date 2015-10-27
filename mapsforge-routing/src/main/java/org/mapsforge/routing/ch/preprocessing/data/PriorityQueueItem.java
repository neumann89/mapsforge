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
package org.mapsforge.routing.ch.preprocessing.data;

/**
 * An item used at a priority queue.
 * 
 * @author Patrick Jungermann
 * @version $Id: PriorityQueueItem.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class PriorityQueueItem<E extends Comparable<E>, K> extends
		org.mapsforge.routing.preprocessing.data.PriorityQueueItem<E> {

	/**
	 * The items identifier. Used to identify the related object.
	 */
	private int id;
	/**
	 * The payload of this item.
	 */
	private K payload = null;

	/**
	 * Constructor. Creates an item, which can be used for at a priority queue.
	 * 
	 * @param id
	 *            The identifier for this item.
	 * @param priority
	 *            The priority of this item.
	 */
	public PriorityQueueItem(int id, E priority) {
		this.id = id;
		setPriority(priority);
	}

	/**
	 * Returns its identifier.
	 * 
	 * @return Its identifier.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns its payload.
	 * 
	 * @return Its payload.
	 */
	public K getPayload() {
		return payload;
	}

	/**
	 * Sets its payload.
	 * 
	 * @param payload
	 *            Its new payload.
	 */
	public void setPayLoad(K payload) {
		this.payload = payload;
	}
}
