package org.mapsforge.routing.preprocessing.data.clustering;

/**
 * Settings related to the data clustering.
 * 
 * @author Patrick Jungermann
 * @version $Id: ClusteringSettings.java 1918 2012-03-13 19:15:41Z Patrick.Jungermann@googlemail.com $
 */
public class ClusteringSettings {

	/**
	 * The algorithm, which has to be used for the data's clustering.
	 */
	public ClusteringAlgorithm algorithm;

	/**
	 * The threshold for each cluster's size.
	 */
	public int clusterSizeThreshold;

	/**
	 * A factor, needed for the {@link ClusteringAlgorithm#K_CENTER K-Center clustering algorithm}.
	 */
	public int oversamplingFactor;

	/**
	 * Creates a clustering settings object.
	 */
	public ClusteringSettings() {
		this(ClusteringAlgorithm.K_CENTER, 1, 1);
	}

	/**
	 * Creates the clustering settings with the given values.
	 * 
	 * @param algorithm
	 *            The algorithm, which has to be used for the data's clustering.
	 * @param clusterSizeThreshold
	 *            The threshold for each cluster's size.
	 * @param oversamplingFactor
	 *            A factor, needed for the {@link ClusteringAlgorithm#K_CENTER K-Center clustering
	 *            algorithm}.
	 */
	public ClusteringSettings(final ClusteringAlgorithm algorithm, final int clusterSizeThreshold,
			final int oversamplingFactor) {
		this.algorithm = algorithm;
		this.clusterSizeThreshold = clusterSizeThreshold;
		this.oversamplingFactor = oversamplingFactor;
	}

    @Override
    public String toString() {
        return String.format("%s{%s, %d, %d)",
                this.getClass().getSimpleName(),
                algorithm.name(), clusterSizeThreshold, oversamplingFactor);
    }

}
