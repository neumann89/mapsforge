package org.mapsforge.routing.preprocessing.data.clustering;

/**
 * A predictor, used by clustering algorithms to predict the size of a cluster.
 * 
 * @author Patrick Jungermann
 * @version $Id: SizePredictor.java 1918 2012-03-13 19:15:41Z Patrick.Jungermann@googlemail.com $
 */
public interface SizePredictor {

	/**
	 * Predicts the serialized size of a set of vertices.
	 * 
	 * @param vertexIds
	 *            The vertices' identifiers.
	 * @return The predicted size.
	 */
	public int predictSize(final int[] vertexIds);

	/**
	 * Returns the number of constraint violations, after which the further execution has to be stopped
	 * and the next cluster has to be created.
	 * 
	 * @return The number of constraint violations, after which the further execution has to be stopped
	 *         and the next cluster has to be created.
	 */
	public int numOfConstraintViolationsToStopAfter();
}
