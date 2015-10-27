package org.mapsforge.routing.ch.preprocessing.evaluation;

import java.util.Properties;

import org.mapsforge.routing.preprocessing.data.clustering.ClusteringAlgorithm;

/**
 * @author Patrick Jungermann
 * @version $Id$
 */
class EvaluationSettings {
	int[] searchSpaceHopLimits;
	int[] kNeighborhood;
	int[] edgeQuotientFactor;
	int[] hierarchyDepthsFactor;
	int[] originalEdgeQuotientFactor;
	ClusteringAlgorithm[] algorithms;
	int[] clusterSizeThresholds;
	int[] oversamplingFactors;

	static EvaluationSettings create(Properties config) {
		EvaluationSettings settings = new EvaluationSettings();

		String searchSpaceHopLimits = config.getProperty("searchSpaceHopLimits", "5,8,15");
		settings.searchSpaceHopLimits = toIntArray(searchSpaceHopLimits);

		String kNeighborhood = config.getProperty("kNeighborhood", "2,4,8");
		settings.kNeighborhood = toIntArray(kNeighborhood);

		String edgeQuotientFactor = config.getProperty("edgeQuotientFactor", "1,2,4");
		settings.edgeQuotientFactor = toIntArray(edgeQuotientFactor);

		String hierarchyDepthsFactor = config.getProperty("hierarchyDepthsFactor", "1,2,4");
		settings.hierarchyDepthsFactor = toIntArray(hierarchyDepthsFactor);

		String originalEdgeQuotientFactor = config.getProperty("originalEdgeQuotientFactor", "1,2,4");
		settings.originalEdgeQuotientFactor = toIntArray(originalEdgeQuotientFactor);

		String algorithms = config.getProperty("algorithms", "K_CENTER,QUAD_TREE,TOPOLOGICAL_ORDER");
		String[] parts = algorithms.split("\\s*,\\s*");
		settings.algorithms = new ClusteringAlgorithm[parts.length];
		for (int i = 0; i < parts.length; i++) {
			settings.algorithms[i] = ClusteringAlgorithm.valueOf(parts[i].toUpperCase());
		}

		String clusterSizeThresholds = config.getProperty("clusterSizeThresholds", "20,40,60");
		settings.clusterSizeThresholds = toIntArray(clusterSizeThresholds);

		String oversamplingFactors = config.getProperty("oversamplingFactors", "6,8,10,15");
		settings.oversamplingFactors = toIntArray(oversamplingFactors);

		return settings;
	}

	static int[] toIntArray(String str) {
		String[] parts = str.split("\\s*,\\s*");
		int[] array = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			array[i] = Integer.parseInt(parts[i], 10);
		}

		return array;
	}
}
