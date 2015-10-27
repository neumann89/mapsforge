package org.mapsforge.routing.ch.preprocessing.evaluation;

/**
 * @author Patrick Jungermann
 * @version $Id$
 */
public class Statistics {
	private static Statistics instance = null;

	private boolean enabled = false;

	public Preprocessing preprocessing;
	public Clustering clustering;

	private Statistics() {
		reset();
	}

	public void reset() {
		preprocessing = new Preprocessing();
		clustering = new Clustering();
	}

	public void enable(boolean enable) {
		enabled = enable;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public static Statistics getInstance() {
		if (instance == null) {
			instance = new Statistics();
		}

		return instance;
	}

	public static class Preprocessing {
		public long durationInNs;

		public int numVertices;
		public int numEdges;
		public int numNormalEdges;
		public int numShortcutEdges;

		public float avgLevel;
		public int maxLevel;

		public float avgHierarchyDepth;
		public int maxHierarchyDepth;

		public int minOriginalEdgeCountOfShortcuts;
		public float avgOriginalEdgeCountOfShortcuts;
		public int maxOriginalEdgeCountOfShortcuts;
	}

	public static class Clustering {
		public long durationInNs;

		public int numClusters;

		public int minBlockSize;
		public float avgBlockSize;
		public int maxBlockSize;
	}

}
