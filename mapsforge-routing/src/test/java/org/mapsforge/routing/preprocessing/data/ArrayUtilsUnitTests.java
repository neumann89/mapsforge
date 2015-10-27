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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

/**
 * Tests related to {@link ArrayUtils}.
 * 
 * @author Patrick Jungermann
 * @version $Id: ArrayUtilsTest.java 1918 2012-03-13 19:15:41Z Patrick.Jungermann@googlemail.com $
 */
public class ArrayUtilsUnitTests {

	@Test
	public void removeAllFrom_itemsAreAllInTheMiddleOfSource_returnNewArrayWithoutTheseItems() {
		// prepare test data
		int[] source = new int[] { 1, 2, 3, 4, 5 };
		int[] remove = new int[] { 2, 3 };

		int[] result = ArrayUtils.removeAllFrom(remove, source);
		assertArrayEquals("'remove' should not be modified", new int[] { 2, 3 }, remove);
		assertArrayEquals("'source' should not be modified", new int[] { 1, 2, 3, 4, 5 }, source);
		assertArrayEquals(new int[] { 1, 4, 5 }, result);
	}

	@Test
	public void removeAllFrom_itemsAreAtTheEndPointsInDifferentOrder_returnNewArrayWithoutTheseItems() {
		// prepare test data
		int[] source = new int[] { 1, 2, 3, 4, 5 };
		int[] remove = new int[] { 5, 1 };

		int[] result = ArrayUtils.removeAllFrom(remove, source);
		assertArrayEquals("'remove' should not be modified", new int[] { 5, 1 }, remove);
		assertArrayEquals("'source' should not be modified", new int[] { 1, 2, 3, 4, 5 }, source);
		assertArrayEquals(new int[]{2, 3, 4}, result);
	}

	@Test
	public void removeAllFrom_noItemsToRemove_returnCloneOfSource() {
		// prepare test data
		int[] source = new int[] { 1, 2, 3, 4, 5 };
		int[] remove = new int[0];

		int[] result = ArrayUtils.removeAllFrom(remove, source);
		assertArrayEquals("'remove' should not be modified", new int[0], remove);
		assertArrayEquals("'source' should not be modified", new int[] { 1, 2, 3, 4, 5 }, source);
		assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
        assertNotSame(source, result);
	}

	@Test
	public void removeAllFrom_itemsAreMultipleTimesInSource_returnNewArrayWithoutAllOfThoseItems() {
		// prepare test data
		int[] source = new int[] { 1, 2, 1, 2, 3 };
		int[] remove = new int[] { 1, 2 };

		int[] result = ArrayUtils.removeAllFrom(remove, source);
		assertArrayEquals("'remove' should not be modified", new int[] { 1, 2 }, remove);
		assertArrayEquals("'source' should not be modified", new int[] { 1, 2, 1, 2, 3 }, source);
		assertArrayEquals(new int[]{3}, result);
	}

	@Test
	public void removeAllFrom_sourceIsEmpty_returnCloneOfSource() {
		// prepare test data
		int[] source = new int[0];
		int[] remove = new int[] { 1, 2 };

		int[] result = ArrayUtils.removeAllFrom(remove, source);
		assertArrayEquals("'remove' should not be modified", new int[] { 1, 2 }, remove);
		assertArrayEquals("'source' should not be modified", new int[0], source);
		assertArrayEquals(new int[0], result);
        assertNotSame(source, result);
	}

	@Test
	public void max_arrayIsNull_returnMinValue() {
		int max = ArrayUtils.max(null);

		assertEquals(Integer.MIN_VALUE, max);
	}

	@Test
	public void max_arrayIsEmpty_returnMinValue() {
		int max = ArrayUtils.max(new int[0]);

		assertEquals(Integer.MIN_VALUE, max);
	}

	@Test
	public void max_arrayIsNonEmpty_returnTheHighestValueOfAllItems() {
		int[] array = new int[] { 12, 14, -19, 1, -1129, 129219, 71, 1912 };

		int result = ArrayUtils.max(array);
		assertEquals(129219, result);
	}

	@Test
	public void swap_forIntArray_validIndexesAsc_itemsShouldBeSwapped() {
		int[] array = new int[] { 0, 1, 2, 3 };

		ArrayUtils.swap(array, 1, 2);
		assertArrayEquals(new int[]{
                0, 2, 1, 3
        }, array);
	}

	@Test
	public void swap_forIntArray_validIndexesDesc_itemsShouldBeSwapped() {
		int[] array = new int[] { 0, 1, 2, 3 };

		ArrayUtils.swap(array, 3, 0);
		assertArrayEquals(new int[]{
                3, 1, 2, 0
        }, array);
	}

	@Test
	public void swap_forIntArray_validIndexesAndBothTheSame_noChange() {
		int[] array = new int[] { 0, 1, 2, 3 };

		ArrayUtils.swap(array, 1, 1);
		assertArrayEquals(new int[] {
				0, 1, 2, 3
		}, array);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void swap_forIntArray_invalidIndex_throwException() {
		int[] array = new int[] { 0, 1, 2 };

		ArrayUtils.swap(array, 3, 0);
	}

	@Test(expected = NullPointerException.class)
	public void swap_forIntArray_arrayIsNull_throwException() {
		int[] array = null;

		ArrayUtils.swap(array, 0, 1);
	}

	@Test
	public void swap_forObjectArray_validIndexesAsc_itemsShouldBeSwapped() {
		Integer[] array = new Integer[] { 0, 1, 2, 3 };

		ArrayUtils.swap(array, 1, 2);
		assertArrayEquals(new Integer[]{
                0, 2, 1, 3
        }, array);
	}

	@Test
	public void swap_forObjectArray_validIndexesDesc_itemsShouldBeSwapped() {
		Integer[] array = new Integer[] { 0, 1, 2, 3 };

		ArrayUtils.swap(array, 3, 0);
		assertArrayEquals(new Integer[]{
                3, 1, 2, 0
        }, array);
	}

	@Test
	public void swap_forObjectArray_validIndexesAndBothTheSame_noChange() {
		Integer[] array = new Integer[] { 0, 1, 2, 3 };

		ArrayUtils.swap(array, 1, 1);
		assertArrayEquals(new Integer[] {
				0, 1, 2, 3
		}, array);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void swap_forObjectArray_invalidIndex_throwException() {
		ArrayUtils.swap(new Object[0], 0, 1);
	}

	@Test(expected = NullPointerException.class)
	public void swap_forObjectArray_arrayIsNull_throwException() {
		Object[] array = null;

        ArrayUtils.swap(array, 0, 1);
	}

	@Test
	public void indexOf_arrayContainsItem_returnIndex() {
		int[] array = new int[] { 0, 1, 2, 3 };

		assertEquals(1, ArrayUtils.indexOf(1, array));
	}

	@Test
	public void indexOf_arrayContainsItemTwice_returnFirstIndex() {
		int[] array = new int[] { 10, 11, 12, 12, 13 };

		assertEquals(2, ArrayUtils.indexOf(12, array));
	}

	@Test
	public void indexOf_itemNotInArray_returnMinusOne() {
		int[] array = new int[] { 10, 11, 12, 12, 13 };

		assertEquals(-1, ArrayUtils.indexOf(6, array));
	}

	@Test(expected = NullPointerException.class)
	public void indexOf_arrayIsNull_throwException() {
		ArrayUtils.indexOf(0, null);
	}

	@Test(expected = NullPointerException.class)
	public void combine_firstArrayIsNull_throwException() {
		ArrayUtils.combine(null, new Object[0]);
	}

	@Test(expected = NullPointerException.class)
	public void combine_secondArrayIsNull_throwException() {
		ArrayUtils.combine(new Object[0], null);
	}

	@Test
	public void combine_validArrays_returnCombinationWithItemsOfTheFirstFollowedByTheSecondArray() {
		Integer[] first = new Integer[] { 12, 13, 14 };
		Integer[] second = new Integer[] { 2, 3, 4 };

		Integer[] result = ArrayUtils.combine(first, second);
		assertArrayEquals(new Integer[] {
				12, 13, 14, 2, 3, 4
		}, result);
	}

	@Test(expected = NullPointerException.class)
	public void reverse_arrayIsNull_throwException() {
		ArrayUtils.reverse(null);
	}

	@Test
	public void reverse_arrayIsEmpty_doNothing() {
		Integer[] array = new Integer[0];
		ArrayUtils.reverse(array);

		assertArrayEquals(new Integer[0], array);
	}

	@Test
	public void reverse_arrayLengthIsEven_arrayIsReversed() {
		Integer[] array = new Integer[] { 1, 2, 3, 4 };
		ArrayUtils.reverse(array);

		assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, array);
	}

	@Test
	public void reverse_arrayLengthIsOdd_arrayIsReversed() {
		Integer[] array = new Integer[] { 1, 2, 3, 4, 5 };
		ArrayUtils.reverse(array);

		assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, array);
	}

}
