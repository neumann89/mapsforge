package org.mapsforge.routing.preprocessing.data.clustering;

/**
 * Simple implementation of a {@link SizePredictor}, which returns the number of vertices as the
 * predicted size.
 * 
 * @author Patrick Jungermann
 * @version $Id: CountSizePredictor.java 1918 2012-03-13 19:15:41Z Patrick.Jungermann@googlemail.com $
 */
public class CountSizePredictor implements SizePredictor {

	@Override
	public int predictSize(int[] vertexIds) {
		return vertexIds.length;
	}

	@Override
	public int numOfConstraintViolationsToStopAfter() {
		return 1;
	}

}
