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

/**
 * Settings related to the preprocessing of the Contraction Hierarchies algorithm.
 * 
 * @author Patrick Jungermann
 * @version $Id: PreprocessorSettings.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class PreprocessorSettings {

	/**
	 * Default value for {@link #numThreads}.
	 */
	private static final int DEFAULT_NUM_THREADS = 10 * Runtime.getRuntime().availableProcessors();

	/**
	 * The number of threads, which can be used for parallelization.
	 */
	public int numThreads = DEFAULT_NUM_THREADS;

	/**
	 * The max. number of hops for limiting the search space.
	 */
	public int searchSpaceHopLimit = 8;

	/**
	 * The k-neighborhood size (as a number of hops).
	 */
	public int kNeighborhood = 2;

	/**
	 * Factor for the edge quotient value, used for combine all heuristic values to a priority.
	 */
	public int edgeQuotientFactor = 2;

	/**
	 * Factor for the hierarchy depth value, used for combine all heuristic values to a priority.
	 */
	public int hierarchyDepthsFactor = 2;

	/**
	 * Factor for the original edge quotient value, used for combine all heuristic values to a priority.
	 */
	public int originalEdgeQuotientFactor = 1;

	/**
	 * Sets the number of threads ({@link #numThreads}) to the parsed {@link Integer} value, or to the
	 * default value {@link #DEFAULT_NUM_THREADS}, if no valid value could be extracted.
	 * 
	 * @param str
	 *            The {@link String}, from which the {@link Integer} value has to be extracted.
	 */
	public void setNumThreads(String str) {
		try {
			numThreads = Integer.parseInt(str, 10);

		} catch (NumberFormatException e) {
			numThreads = DEFAULT_NUM_THREADS;
		}
	}

    @Override
    public String toString() {
        return String.format(
                "PreprocessorSettings[%d]{%d-%d, %d-%d-%d}",
                numThreads,
                searchSpaceHopLimit, kNeighborhood,
                edgeQuotientFactor, hierarchyDepthsFactor, originalEdgeQuotientFactor);
    }
}
