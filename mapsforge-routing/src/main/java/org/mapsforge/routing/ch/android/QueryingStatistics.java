package org.mapsforge.routing.ch.android;

import gnu.trove.set.hash.TIntHashSet;

/**
 * @author Patrick Jungermann
 * @version $Id: QueryingStatistics.java 1746 2012-01-16 22:38:34Z Patrick.Jungermann@googlemail.com $
 */
// TODO: documentation
public class QueryingStatistics {
	private static QueryingStatistics instance = null;

	private boolean enabled = false;

	public int numBlockReads;
	public int numCacheHits;
	private int numVisitedVertices;
	private TIntHashSet visitedVertices = new TIntHashSet();

	private QueryingStatistics() {
	}

	public void enable(boolean enable) {
		enabled = enable;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void reset() {
		numBlockReads = 0;
		numCacheHits = 0;
		numVisitedVertices = 0;
		visitedVertices.clear();
	}

	public void addVisitedVertex(final int id) {
		numVisitedVertices = -1;
		visitedVertices.add(id);
	}

	public int getNumVisitedVertices() {
		if (numVisitedVertices == -1) {
			numVisitedVertices = visitedVertices.size();
		}

		return numVisitedVertices;
	}

	public static QueryingStatistics getInstance() {
		if (instance == null) {
			instance = new QueryingStatistics();
		}

		return instance;
	}

}
