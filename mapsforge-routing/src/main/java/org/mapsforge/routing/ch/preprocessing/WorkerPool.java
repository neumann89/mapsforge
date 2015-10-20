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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.routing.ch.preprocessing.graph.Graph;

/**
 * Pool, which handles the batch processing, done by worker threads.
 * 
 * @author Patrick Jungermann
 * @version $Id: WorkerPool.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class WorkerPool {

	/**
	 * Used class-level logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(WorkerPool.class.getName());

	/**
	 * Empty array used as indicator of the work's end.
	 */
	private static final int[] EMPTY = new int[0];

	/**
	 * Size of this pool. For every started work, this number of workers will be started to get the job
	 * done.
	 */
	private final int poolSize;

	/**
	 * The number of batches, used to get the job done.
	 */
	private final int numBatches;

	/**
	 * The related {@link Graph} of this pool.
	 */
	private final Graph graph;

	/**
	 * The settings, related to the preprocessing process.
	 */
	private final PreprocessorSettings settings;

	/**
	 * The current handled data.
	 */
	private int[] data;

	/**
	 * The size of a batch of the current data.
	 */
	private int batchSize;

	/**
	 * The current position.
	 */
	private int position;

	/**
	 * Counter used to track all {@link Thread}s, which should be alive.
	 */
	private int threadsAlive = 0;

	/**
	 * Constructor. Creates a worker pool with fixed size of the pool and a fixed number of batches.
	 * 
	 * @param graph
	 *            The related {@link Graph}.
	 * @param settings
	 *            The settings, related to the preprocessing process.
	 * @param poolSize
	 *            The size of the pool.
	 * @param numBatches
	 *            The number of batches.
	 */
	public WorkerPool(final Graph graph, final PreprocessorSettings settings, final int poolSize,
			final int numBatches) {
		this.graph = graph;
		this.settings = settings;
		this.poolSize = poolSize;
		this.numBatches = numBatches;
	}

	/**
	 * Starts the work for the given data, using the given worker class and the additional arguments.
	 * 
	 * @param data
	 *            The data, which has to be processed.
	 * @param workerThreadClass
	 *            The worker thread class, which is responsible for processing the data.
	 * @param additionalArguments
	 *            Any additional arguments, which have to be used for the instantiation.
	 * @param <T>
	 *            The class of the enclosing instance.
	 * @throws IllegalStateException
	 *             if the worker pool has not finished its last work, yet.
	 * @throws NoSuchMethodException
	 *             if there was no suitable constructor for the instantiation.
	 */
	public <T> void startWork(final int[] data,
			final Class<? extends AbstractWorkerThread> workerThreadClass,
			final Object... additionalArguments) throws IllegalStateException, NoSuchMethodException {
		startWork(null, data, workerThreadClass, additionalArguments);
	}

	/**
	 * Starts the work for the given data, using the given worker class and the additional arguments.
	 * 
	 * @param enclosingInstance
	 *            The enclosing instance. This has to be for internal worker classes, because this is
	 *            needed as first parameter for the instantiation of a worker.
	 * @param data
	 *            The data, which has to be processed.
	 * @param workerThreadClass
	 *            The worker thread class, which is responsible for processing the data.
	 * @param additionalArguments
	 *            Any additional arguments, which have to be used for the instantiation.
	 * @param <T>
	 *            The class of the enclosing instance.
	 * @throws IllegalStateException
	 *             if the worker pool has not finished its last work, yet.
	 * @throws NoSuchMethodException
	 *             if there was no suitable constructor for the instantiation.
	 */
	public <T> void startWork(final T enclosingInstance, final int[] data,
			final Class<? extends AbstractWorkerThread> workerThreadClass,
			final Object... additionalArguments) throws IllegalStateException, NoSuchMethodException {
		if (!isFinished()) {
			throw new IllegalStateException("The current work of the pool is not finished, yet.");
		}

		position = 0;

		this.data = data;
		batchSize = (int) Math.ceil(1d * data.length / numBatches);

		final int numFixedArguments = enclosingInstance == null ? 3 : 4;
		final Object[] arguments = new Object[additionalArguments.length + numFixedArguments];
		final Class[] argumentClasses = new Class[arguments.length];

		if (enclosingInstance != null) {
			arguments[0] = enclosingInstance;
			arguments[1] = this;
			arguments[2] = graph;
			arguments[3] = settings;
			argumentClasses[0] = enclosingInstance.getClass();
			argumentClasses[1] = this.getClass();
			argumentClasses[2] = graph.getClass();
			argumentClasses[3] = settings.getClass();

		} else {
			arguments[0] = this;
			arguments[1] = graph;
			arguments[2] = settings;
			argumentClasses[0] = this.getClass();
			argumentClasses[1] = graph.getClass();
			argumentClasses[2] = settings.getClass();
		}

		for (int i = 0; i < additionalArguments.length; i++) {
			arguments[i + numFixedArguments] = additionalArguments[i];
			argumentClasses[i + numFixedArguments] = additionalArguments[i].getClass();
		}

		try {
			final Constructor<? extends AbstractWorkerThread> constructor = getConstructor(
					workerThreadClass, argumentClasses);

			final int numThreads = Math.min(poolSize, (int) Math.ceil(1d * data.length / batchSize));
			threadsAlive = numThreads;
			for (int i = 0; i < numThreads; i++) {
				try {
					constructor.newInstance(arguments).start();

				} catch (InstantiationException e) {
					LOGGER.log(Level.SEVERE, "Failed to create an instance of class "
							+ workerThreadClass.getName(), e);
				} catch (IllegalAccessException e) {
					LOGGER.log(Level.SEVERE, "Failed to create an instance of class "
							+ workerThreadClass.getName(), e);
				} catch (InvocationTargetException e) {
					LOGGER.log(Level.SEVERE, "Failed to create an instance of class "
							+ workerThreadClass.getName(), e);
				}
			}

		} catch (NoSuchMethodException e) {
			LOGGER.log(Level.SEVERE, "Failed to find a suitable constructor for class "
					+ workerThreadClass.getName(), e);
			throw e;
		}
	}

	/**
	 * Returns the next processable batch.
	 * 
	 * @return The next processable batch.
	 */
	public synchronized int[] getNextBatch() {
		final int[] batch;

		if (position < data.length) {
			final int size = position + batchSize - 1 < data.length ? batchSize : data.length
					- position;
			batch = new int[size];
			System.arraycopy(data, position, batch, 0, size);

			position += size;
		} else {
			batch = EMPTY;
			threadsAlive--;
		}

		return batch;
	}

	/**
	 * Checks, if the former work was already finished, or not.
	 * 
	 * @return {@code true}, if the former work was finished, otherwise {@code false}.
	 */
	public boolean isFinished() {
		return data == null || position >= data.length && threadsAlive == 0;
	}

	/**
	 * Returns the <strong>first</strong>suitable constructor. <strong>It is not guaranteed to return
	 * the most suitable constructor.</strong> In general, it behaves similar to
	 * {@link Class#getConstructor(Class[])}, but also allows to use sub-classes of parameter types
	 * (e.g., auto-boxing is also not supported).
	 * 
	 * @param clazz
	 *            The {@link Class}, for which a suitable constructor has to be found.
	 * @param suppliedParameterTypes
	 *            The supplied parameter types, for which the constructor has to be found.
	 * @return The <string>first</string> suitable constructor.
	 * @throws NoSuchMethodException
	 *             if there is no suitable constructor for these parameter types.
	 */
	@SuppressWarnings("unchecked")
	private static <E> Constructor<E> getConstructor(final Class<E> clazz,
			final Class[] suppliedParameterTypes) throws NoSuchMethodException {
		Constructor result = null;

		Class[] parameterTypes;
		boolean isValid;
		for (final Constructor constructor : clazz.getConstructors()) {
			parameterTypes = constructor.getParameterTypes();

			if (parameterTypes.length == suppliedParameterTypes.length) {
				isValid = true;

				for (int i = 0; i < parameterTypes.length; i++) {
					if (!parameterTypes[i].isAssignableFrom(suppliedParameterTypes[i])) {
						isValid = false;
						break;
					}
				}

				if (isValid) {
					result = constructor;
					break;
				}
			}
		}

		if (result == null) {
			throw new NoSuchMethodException(clazz.getName() + ".<init>"
					+ Arrays.toString(suppliedParameterTypes) + " " + getConstructorSuggestions(clazz));
		}

		return result;
	}

	/**
	 * Returns an informative suggestion of possible constructors of the given {@link Class}, which
	 * could be displayed, if there was no (valid) constructor, so that it is more easier to find a good
	 * solution / the reason for that.
	 * 
	 * @param clazz
	 *            The class, for which all possible constructors has to be suggested.
	 * @return An informative suggestion of possible constructors.
	 */
	private static String getConstructorSuggestions(final Class<?> clazz) {
		final StringBuilder builder = new StringBuilder();
		final String separator = System.getProperty("line.separator");

		builder.append("Possible solutions: ");
		final Constructor<?>[] constructors = clazz.getConstructors();
		if (constructors.length == 0) {
			builder.append("No constructors for class ").append(clazz.getName());

		} else {
			for (final Constructor<?> constructor : constructors) {
				builder.append(separator).append("\t").append(clazz.getName())
						.append(Arrays.toString(constructor.getParameterTypes()));
			}
		}

		return builder.toString();
	}
}
