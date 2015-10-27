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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the class {@link BinaryMinHeap}.
 *
 * @author Patrick Jungermann
 * @version $Id: BinaryMinHeapTest.java 1918 2012-03-13 19:15:41Z Patrick.Jungermann@googlemail.com $
 */
public class BinaryMinHeapUnitTests {// TODO: refactoring? (splitting up of test etc.)
    /**
     * Heap, used for all tests.
     */
    private BinaryMinHeap<TestItem, Long> heap;

    /**
     * Setup for the following tests (initializing etc.).
     */
    @Before
    public void setUp() {
        // initializing
        heap = new BinaryMinHeap<TestItem, Long>(15);
    }

    /**
     * Cleanup of stuff, created during the tests.
     */
    @After
    public void tearDown() {
        heap.clear();
        heap = null;
    }

    /**
     * Tests {@link BinaryMinHeap#insert(IBinaryHeapItem)}.
     */
    @Test
    public void insert() {
        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());

        TestItem item1 = new TestItem(12L);
        TestItem item2 = new TestItem(23L);
        TestItem item3 = new TestItem(7L);

        heap.insert(item1);
        Assert.assertEquals("Heap was expected to contain exactly one item.", heap.size(), 1);
        Assert.assertEquals("The inserted item 1 was expected to be at heap index 0.", item1.getHeapIndex(), 0);

        heap.insert(item2);
        Assert.assertEquals("Heap was expected to contain exactly two items.", heap.size(), 2);
        Assert.assertEquals("The inserted item 2 was expected to be at heap index 1.", item2.getHeapIndex(), 1);

        heap.insert(item3);
        Assert.assertEquals("Heap was expected to contain exactly three items.", heap.size(), 3);
        Assert.assertEquals("The inserted item 3 was expected to be at heap index 0.", item3.getHeapIndex(), 0);
        Assert.assertEquals("The item 2 was expected to be now at heap index 1.", item2.getHeapIndex(), 1);
        Assert.assertEquals("The item 1 was expected to be now at heap index 2.", item1.getHeapIndex(), 2);

        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());
    }

    /**
     * Tests {@link BinaryMinHeap#decreaseKey(IBinaryHeapItem, Comparable)}, using a key, which is lower than the old one.
     */
    @Test
    public void decreaseKey_newKeyIsLower() {
        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());

        TestItem item1 = new TestItem(12L);
        TestItem item2 = new TestItem(23L);
        TestItem item3 = new TestItem(7L);
        TestItem item4 = new TestItem(16L);
        TestItem item5 = new TestItem(29L);
        TestItem item6 = new TestItem(609L);
        TestItem item7 = new TestItem(87L);
        TestItem item8 = new TestItem(12L);

        heap.insert(item1);
        heap.insert(item2);
        heap.insert(item3);
        heap.insert(item4);
        heap.insert(item5);
        heap.insert(item6);
        heap.insert(item7);
        heap.insert(item8);

        Assert.assertEquals("Heap was expected to contain exactly eight items.", heap.size(), 8);

        Assert.assertEquals("The inserted item 3 was expected to be at heap index 0 (layer 0).", item3.getHeapIndex(), 0);
        Assert.assertEquals("The inserted item 8 was expected to be at heap index 1 (layer 1).", item8.getHeapIndex(), 1);
        Assert.assertEquals("The inserted item 1 was expected to be at heap index 2 (layer 1).", item1.getHeapIndex(), 2);
        Assert.assertEquals("The inserted item 4 was expected to be at heap index 3 (layer 2).", item4.getHeapIndex(), 3);
        Assert.assertEquals("The inserted item 5 was expected to be at heap index 4 (layer 2).", item5.getHeapIndex(), 4);
        Assert.assertEquals("The inserted item 6 was expected to be at heap index 5 (layer 2).", item6.getHeapIndex(), 5);
        Assert.assertEquals("The inserted item 7 was expected to be at heap index 6 (layer 2).", item7.getHeapIndex(), 6);
        Assert.assertEquals("The inserted item 2 was expected to be at heap index 7 (layer 3).", item2.getHeapIndex(), 7);

        heap.decreaseKey(item2, 1L);
        Assert.assertEquals("The new key of item 2 was expected to be 1.", item2.getHeapKey(), (Long) 1L);
        Assert.assertEquals("After decreasing the key of item 2, item 2 was expected to be at heap index 0.", item2.getHeapIndex(), 0);
        Assert.assertEquals("After decreasing the key of item 2, item 3 was expected to be at heap index 1.", item3.getHeapIndex(), 1);
        Assert.assertEquals("After decreasing the key of item 2, item 8 was expected to be at heap index 3.", item8.getHeapIndex(), 3);
        Assert.assertEquals("After decreasing the key of item 2, item 4 was expected to be at heap index 7.", item4.getHeapIndex(), 7);

        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());
    }

    /**
     * Tests {@link BinaryMinHeap#decreaseKey(IBinaryHeapItem, Comparable)}, using keys, which are greater than or equals the old one.
     */
    @Test
    public void decreaseKey_greaterEqualKeys() {
        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());

        TestItem item1 = new TestItem(12L);
        TestItem item2 = new TestItem(23L);
        TestItem item3 = new TestItem(7L);

        heap.insert(item1);
        heap.insert(item2);
        heap.insert(item3);
        Assert.assertEquals("Heap was expected to contain exactly three items.", heap.size(), 3);
        Assert.assertEquals("The item 3 was expected to be at heap index 0.", item3.getHeapIndex(), 0);
        Assert.assertEquals("The item 2 was expected to be at heap index 1.", item2.getHeapIndex(), 1);
        Assert.assertEquals("The item 1 was expected to be at heap index 2.", item1.getHeapIndex(), 2);

        Long oldKey = item1.getHeapKey();
        int oldIndex = item1.getHeapIndex();
        heap.decreaseKey(item1, oldKey);
        Assert.assertEquals("After calling decreaseKey() for item 1 with the same key, the key of item 1 was expected to be unchanged.", item1.getHeapKey(), oldKey);
        Assert.assertEquals("After calling decreaseKey() for item 1 with the same key, item 1 was expected to be at the same index.", item1.getHeapIndex(), oldIndex);

        heap.decreaseKey(item1, 2 * oldKey);
        Assert.assertEquals("After calling decreaseKey() for item 1 with a greater key, the key of item 1 was expected to be unchanged.", item1.getHeapKey(), oldKey);
        Assert.assertEquals("After calling decreaseKey() for item 1 with a greater key, item 1 was expected to be at the same index.", item1.getHeapIndex(), oldIndex);

        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());
    }

    /**
     * Tests {@link BinaryMinHeap#changeKey(IBinaryHeapItem, Comparable)}, using a key, which is lower than the old one.
     */
    @Test
    public void changeKey_newKeyIsLower() {
        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());

        TestItem item1 = new TestItem(12L);
        TestItem item2 = new TestItem(23L);
        TestItem item3 = new TestItem(7L);
        TestItem item4 = new TestItem(16L);
        TestItem item5 = new TestItem(29L);
        TestItem item6 = new TestItem(609L);
        TestItem item7 = new TestItem(87L);
        TestItem item8 = new TestItem(12L);

        heap.insert(item1);
        heap.insert(item2);
        heap.insert(item3);
        heap.insert(item4);
        heap.insert(item5);
        heap.insert(item6);
        heap.insert(item7);
        heap.insert(item8);

        Assert.assertEquals("Heap was expected to contain exactly eight items.", heap.size(), 8);

        Assert.assertEquals("The inserted item 3 was expected to be at heap index 0 (layer 0).", item3.getHeapIndex(), 0);
        Assert.assertEquals("The inserted item 8 was expected to be at heap index 1 (layer 1).", item8.getHeapIndex(), 1);
        Assert.assertEquals("The inserted item 1 was expected to be at heap index 2 (layer 1).", item1.getHeapIndex(), 2);
        Assert.assertEquals("The inserted item 4 was expected to be at heap index 3 (layer 2).", item4.getHeapIndex(), 3);
        Assert.assertEquals("The inserted item 5 was expected to be at heap index 4 (layer 2).", item5.getHeapIndex(), 4);
        Assert.assertEquals("The inserted item 6 was expected to be at heap index 5 (layer 2).", item6.getHeapIndex(), 5);
        Assert.assertEquals("The inserted item 7 was expected to be at heap index 6 (layer 2).", item7.getHeapIndex(), 6);
        Assert.assertEquals("The inserted item 2 was expected to be at heap index 7 (layer 3).", item2.getHeapIndex(), 7);

        heap.changeKey(item2, 1L);
        Assert.assertEquals("The new key of item 2 was expected to be 1.", item2.getHeapKey(), (Long) 1L);
        Assert.assertEquals("After changing the key of item 2, item 2 was expected to be at heap index 0.", item2.getHeapIndex(), 0);
        Assert.assertEquals("After changing the key of item 2, item 3 was expected to be at heap index 1.", item3.getHeapIndex(), 1);
        Assert.assertEquals("After changing the key of item 2, item 8 was expected to be at heap index 3.", item8.getHeapIndex(), 3);
        Assert.assertEquals("After changing the key of item 2, item 4 was expected to be at heap index 7.", item4.getHeapIndex(), 7);

        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());
    }

    /**
     * Tests {@link BinaryMinHeap#changeKey(IBinaryHeapItem, Comparable)}, using a key, which is greater than the old one.
     */
    @Test
    public void changeKey_newKeyIsGreater() {
        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());

        TestItem item1 = new TestItem(12L);
        TestItem item2 = new TestItem(23L);
        TestItem item3 = new TestItem(7L);
        TestItem item4 = new TestItem(16L);
        TestItem item5 = new TestItem(29L);
        TestItem item6 = new TestItem(609L);
        TestItem item7 = new TestItem(87L);
        TestItem item8 = new TestItem(12L);

        heap.insert(item1);
        heap.insert(item2);
        heap.insert(item3);
        heap.insert(item4);
        heap.insert(item5);
        heap.insert(item6);
        heap.insert(item7);
        heap.insert(item8);

        Assert.assertEquals("Heap was expected to contain exactly eight items.", heap.size(), 8);

        Assert.assertEquals("The inserted item 3 was expected to be at heap index 0 (layer 0).", item3.getHeapIndex(), 0);
        Assert.assertEquals("The inserted item 8 was expected to be at heap index 1 (layer 1).", item8.getHeapIndex(), 1);
        Assert.assertEquals("The inserted item 1 was expected to be at heap index 2 (layer 1).", item1.getHeapIndex(), 2);
        Assert.assertEquals("The inserted item 4 was expected to be at heap index 3 (layer 2).", item4.getHeapIndex(), 3);
        Assert.assertEquals("The inserted item 5 was expected to be at heap index 4 (layer 2).", item5.getHeapIndex(), 4);
        Assert.assertEquals("The inserted item 6 was expected to be at heap index 5 (layer 2).", item6.getHeapIndex(), 5);
        Assert.assertEquals("The inserted item 7 was expected to be at heap index 6 (layer 2).", item7.getHeapIndex(), 6);
        Assert.assertEquals("The inserted item 2 was expected to be at heap index 7 (layer 3).", item2.getHeapIndex(), 7);

        heap.changeKey(item8, 24L);
        Assert.assertEquals("The new key of item 8 was expected to be 24.", item8.getHeapKey(), (Long) 24L);
        Assert.assertEquals("After changing the key of item 8, item 4 was expected to be at heap index 1.", item4.getHeapIndex(), 1);
        Assert.assertEquals("After changing the key of item 8, item 2 was expected to be at heap index 3.", item2.getHeapIndex(), 3);
        Assert.assertEquals("After changing the key of item 8, item 8 was expected to be at heap index 7.", item8.getHeapIndex(), 7);

        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());
    }

    /**
     * Tests {@link BinaryMinHeap#changeKey(IBinaryHeapItem, Comparable)}, using a key, which equals the old one.
     */
    @Test
    public void changeKey_newKeyIsEqual() {
        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());

        TestItem item1 = new TestItem(12L);
        TestItem item2 = new TestItem(23L);
        TestItem item3 = new TestItem(7L);

        heap.insert(item1);
        heap.insert(item2);
        heap.insert(item3);
        Assert.assertEquals("Heap was expected to contain exactly three items.", heap.size(), 3);
        Assert.assertEquals("The item 3 was expected to be at heap index 0.", item3.getHeapIndex(), 0);
        Assert.assertEquals("The item 2 was expected to be at heap index 1.", item2.getHeapIndex(), 1);
        Assert.assertEquals("The item 1 was expected to be at heap index 2.", item1.getHeapIndex(), 2);

        Long oldKey = item1.getHeapKey();
        int oldIndex = item1.getHeapIndex();
        heap.changeKey(item1, oldKey);
        Assert.assertEquals("After calling changeKey() for item 1 with the same key, the key of item 1 was expected to be unchanged.", item1.getHeapKey(), oldKey);
        Assert.assertEquals("After calling changeKey() for item 1 with the same key, item 1 was expected to be at the same index.", item1.getHeapIndex(), oldIndex);

        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());
    }

    /**
     * Tests {@link BinaryMinHeap#extractMin()}.
     */
    @Test
    public void extractMin() {
        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());

        TestItem item1 = new TestItem(13L);
        TestItem item2 = new TestItem(7L);

        heap.insert(item1);
        heap.insert(item2);
        Assert.assertEquals("The heap was expected to contain two items.", heap.size(), 2);

        TestItem minItem1 = heap.extractMin();
        Assert.assertEquals("Item 2 was expected as minimum.", minItem1, item2);
        Assert.assertEquals("The heap was expected to contain one item.", heap.size(), 1);

        TestItem minItem2 = heap.extractMin();
        Assert.assertNotSame("The returned item from the second call has to be not the same as the one from the first call.", minItem1, minItem2);
        Assert.assertTrue("The key of the minimum, extracted first, has to be lower than the key of the second one.", minItem1.getHeapKey().compareTo(minItem2.getHeapKey()) == -1);

        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());
    }

    /**
     * Tests {@link BinaryMinHeap#peek()}.
     */
    @Test
    public void peek() {
        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());

        TestItem item1 = new TestItem(13L);
        TestItem item2 = new TestItem(7L);

        heap.insert(item1);
        heap.insert(item2);

        TestItem peekItem1 = heap.peek();
        Assert.assertSame("Item 2 was expected as peek item.", peekItem1, item2);

        TestItem peekItem2 = heap.peek();
        Assert.assertSame("Two calls of peek() without changing the heap have to return the same item.", peekItem1, peekItem2);

        heap.clear();
        Assert.assertTrue("Heap was expected to be empty after calling clear().", heap.isEmpty());
    }

    /**
     * Simple implementation of the {@link IBinaryHeapItem} interface. It's instances are used within the parent class' tests.
     */
    private class TestItem implements IBinaryHeapItem<Long> {
        private Long key;
        private int index;

        /**
         * Constructor, which creates an instance of this class.
         *
         * @param key The key, which has to be used as the heap key for this heap item.
         */
        public TestItem(Long key) {
            this.key = key;
        }

        /**
         * @return index within the array of the heap.
         */
        @Override
        public int getHeapIndex() {
            return this.index;
        }

        /**
         * Must only be used by the heap while element is enqueued.
         *
         * @param idx position within the array based heap.
         */
        @Override
        public void setHeapIndex(int idx) {
            this.index = idx;
        }

        /**
         * Must only be used by the heap while element is enqueued.
         *
         * @param key key of the heap item.
         */
        @Override
        public void setHeapKey(Long key) {
            this.key = key;
        }

        /**
         * @return the key of this item.
         */
        @Override
        public Long getHeapKey() {
            return this.key;
        }
    }
}
