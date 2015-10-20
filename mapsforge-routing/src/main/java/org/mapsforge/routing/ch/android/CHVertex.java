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
package org.mapsforge.routing.ch.android;

import org.mapsforge.routing.android.data.ObjectPool;

/**
 * A Contraction Hierarchies graph's vertex.
 * 
 * @author Patrick Jungermann
 * @version $Id: CHVertex.java 1662 2011-12-30 12:08:08Z Patrick.Jungermann@googlemail.com $
 */
class CHVertex implements ObjectPool.Poolable {

	/**
	 * Whether it was released or not.
	 */
	private boolean released;

	/**
	 * The vertex' identifier.
	 */
	public int id;
	/**
	 * The vertex' latitude in E6 format.
	 */
	public int latitudeE6;
	/**
	 * The vertex' longitude in E6 format.
	 */
	public int longitudeE6;

	@Override
	public boolean isReleased() {
		return released;
	}

	@Override
	public void setReleased(boolean released) {
		this.released = released;
	}

}
