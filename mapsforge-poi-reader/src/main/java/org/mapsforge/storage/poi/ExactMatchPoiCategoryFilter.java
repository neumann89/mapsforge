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
package org.mapsforge.storage.poi;

import java.util.Collection;
import java.util.HashSet;

/**
 * A category filter that accepts all categories added to it. Unless {@link WhitelistPoiCategoryFilter} no child
 * categories of an added category are accepted.
 * 
 * @author Karsten Groll
 */
public class ExactMatchPoiCategoryFilter implements PoiCategoryFilter {
	private HashSet<PoiCategory> whiteList;

	/**
	 * Default constructor.
	 */
	public ExactMatchPoiCategoryFilter() {
		this.whiteList = new HashSet<PoiCategory>();
	}

	@Override
	public boolean isAcceptedCategory(PoiCategory category) {
		return this.whiteList.contains(category);
	}

	@Override
	public void addCategory(PoiCategory category) {
		this.whiteList.add(category);
	}

	@Override
	public Collection<PoiCategory> getAcceptedCategories() {
		return this.whiteList;
	}

	@Override
	public Collection<PoiCategory> getAcceptedSuperCategories() {
		return this.whiteList;
	}

}
