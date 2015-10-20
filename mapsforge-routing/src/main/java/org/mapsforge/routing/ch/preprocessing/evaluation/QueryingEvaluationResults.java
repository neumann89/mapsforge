package org.mapsforge.routing.ch.preprocessing.evaluation;

/**
 * @author Patrick Jungermann
 * @version $Id$
 */
// TODO: documentation
class QueryingEvaluationResults {
	long minDuration = Long.MAX_VALUE;
	long maxDuration = Long.MIN_VALUE;
	double sumDurations = 0;

	int minBlockReads = Integer.MAX_VALUE;
	int maxBlockReads = Integer.MIN_VALUE;
	double sumBlockReads = 0;

	int minVisitedVertices = Integer.MAX_VALUE;
	int maxVisitedVertices = Integer.MIN_VALUE;
	double sumVisitedVertices = 0;
}
