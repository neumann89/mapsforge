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
package org.mapsforge.routing.preprocessing.data;

import java.lang.reflect.Array;

/**
 * Utility methods related to arrays.
 * 
 * @version $Id: ArrayUtils.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class ArrayUtils {

	/**
	 * Swaps the i-th and j-th element of the array.
	 * 
	 * @param arr
	 *            The array.
	 * @param i
	 *            The first index.
	 * @param j
	 *            The second index.
	 */
	public static void swap(final int[] arr, final int i, final int j) {
		final int tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

	/**
	 * Swaps the i-th and j-th element of the array.
	 * 
	 * @param <T>
	 *            Class type of the array's elements.
	 * @param arr
	 *            The array.
	 * @param i
	 *            The first index.
	 * @param j
	 *            The second index.
	 */
	public static <T> void swap(final T[] arr, final int i, final int j) {
		final T tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

	/**
	 * Returns an array, which contains all entries of the array {@code source} without the entries of
	 * the array {@code remove}.<br/>
	 * The arrays {@code remove} and {@code source} will not be modified.
	 * 
	 * @param remove
	 *            The entries, which has to be removed.
	 * @param source
	 *            The entries, from which the other ones have to be removed.
	 * @return An array, which contains all entries of the array {@code source} without the entries of
	 *         the array {@code remove}.
	 */
	public static int[] removeAllFrom(final int[] remove, final int[] source) {
		if (source.length == 0 || remove.length == 0) {
			return source.clone();
		}

		final int[] tmp = new int[source.length];

		int pos = 0;
		for (final int aSource : source) {
			boolean removeId = false;

			for (final int aRemove : remove) {
				if (aSource == aRemove) {
					removeId = true;
					break;
				}
			}

			if (!removeId) {
				tmp[pos++] = aSource;
			}
		}

		final int[] cleaned = new int[pos];
		System.arraycopy(tmp, 0, cleaned, 0, pos);

		return cleaned;
	}

	/**
	 * Returns the array's maximum value. If the array is empty or {@code null},
	 * {@link Integer#MIN_VALUE} will be used as maximum.
	 * 
	 * @param array
	 *            The array, for which the maximum is searched for.
	 * @return The maximum value of the array.
	 */
	public static int max(final int[] array) {
		int max = Integer.MIN_VALUE;

		if (array != null) {
			for (final int item : array) {
				if (item > max) {
					max = item;
				}
			}
		}

		return max;
	}

	/**
	 * Returns the (first) index of {@code item} in {@code array}.
	 * 
	 * @param item
	 *            The item, which has to be found in the array.
	 * @param array
	 *            The array, which should contain the item.
	 * @return The (first) index of {@code item} in {@code array}, or {@code -1}.
	 */
	public static int indexOf(int item, int[] array) {
		int index = -1;

		for (int i = 0; i < array.length; i++) {
			if (item == array[i]) {
				index = i;
				break;
			}
		}

		return index;
	}

	/**
	 * Combines both array to one array, which contains the elements from a and then the elements from
	 * b.
	 * 
	 * @param a
	 *            The first array, which has to be combined with the other one.
	 * @param b
	 *            The second array, which has to be combined with the other one.
	 * @param <T>
	 *            The elements' class.
	 * @return The combined array.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] combine(final T[] a, final T[] b) {
		final Class<? extends T[]> clazz = (Class<? extends T[]>) a.getClass();
		final T[] combined = ((Object) clazz == (Object) Object[].class)
				? (T[]) new Object[a.length + b.length]
				: (T[]) Array.newInstance(clazz.getComponentType(), a.length + b.length);

		System.arraycopy(a, 0, combined, 0, a.length);
		System.arraycopy(b, 0, combined, a.length, b.length);

		return combined;
	}

	/**
	 * Reverses the entries of an array.
	 * 
	 * @param array
	 *            The array, which has to be reversed.
	 * @param <T>
	 *            The entries' class.
	 */
	public static <T> void reverse(final T[] array) {
		final int lastIndex = array.length - 1;
		final int lastSwapIndex = array.length / 2 - 1;

		for (int i = 0; i <= lastSwapIndex; i++) {
			swap(array, i, lastIndex - i);
		}
	}

	/**
	 * Sums up all values of an array and returns that sum.
	 * 
	 * @param array
	 *            The array, containing the values, for which the sum is requested.
	 * @return The sum of all values of the array
	 */
	public static int sum(final int[] array) {
		int sum = 0;
		for (int value : array) {
			sum += value;
		}

		return sum;
	}

}
