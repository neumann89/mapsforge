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

import org.mapsforge.routing.ch.preprocessing.graph.Graph;

/**
 * Abstract implementation of a worker thread, which could be used at a {@link WorkerPool}.
 * 
 * @author Patrick Jungermann
 * @version $Id: AbstractWorkerThread.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public abstract class AbstractWorkerThread extends Thread {

	/**
	 * The pool, from which this thread gets its work.
	 */
	protected final WorkerPool pool;

	/**
	 * The graph, related to the work.
	 */
	protected final Graph graph;

	/**
	 * The settings, related to the preprocessing process.
	 */
	protected final PreprocessorSettings settings;

	/**
	 * Creates a worker thread for the given pool and graph.
	 * 
	 * @param pool
	 *            The pool, from which this thread gets its work.
	 * @param graph
	 *            The graph, related to the work.
	 * @param settings
	 *            The settings, related to the preprocessing process.
	 */
	public AbstractWorkerThread(final WorkerPool pool, final Graph graph,
			final PreprocessorSettings settings) {
		this.pool = pool;
		this.graph = graph;
		this.settings = settings;
	}
}
