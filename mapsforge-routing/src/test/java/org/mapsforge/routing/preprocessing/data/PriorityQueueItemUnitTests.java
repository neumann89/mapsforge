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

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * Tests for the class {@link PriorityQueueItem}.
 *
 * @author Patrick Jungermann
 * @version $Id: PriorityQueueItemTest.java 1918 2012-03-13 19:15:41Z Patrick.Jungermann@googlemail.com $
 */
public class PriorityQueueItemUnitTests {// TODO: refactoring? (splitting up of test etc.)

    @Test
    public void constructor_priorityIsInitializedWithNull() {
        PriorityQueueItem<Integer> item = new PriorityQueueItem<Integer>();

        assertNull("Priority has to be null after initialization.", item.getPriority());
    }

    @Test
    public void compareTo() {
        PriorityQueueItem<Integer> item1 = new PriorityQueueItem<Integer>();
        PriorityQueueItem<Integer> item2 = new PriorityQueueItem<Integer>();

        Assert.assertEquals("compareTo has to return <-1> as result, if <null> will be passed as argument.", -1, item1.compareTo(null));
        Assert.assertEquals("Both items have <null> as priority. The method has to return <0> as result.", 0, item1.compareTo(item2));

        item1.setPriority(10);
        Assert.assertEquals("<item2> has <null> as priority whereas <item1> has not.", -1, item1.compareTo(item2));

        item2.setPriority(10);
        Assert.assertEquals("Both items have the same priority value.", 0, item1.compareTo(item2));
        Assert.assertEquals("Both items have the same priority value.", 0, item2.compareTo(item1));

        item2.setPriority(12);
        Assert.assertEquals("The priority of <item1> is lower than the of <item2>.", -1, item1.compareTo(item2));
        Assert.assertEquals("The priority of <item2> is greater than the of <item1>.", 1, item2.compareTo(item1));

        // test negative values
        item2.setPriority(-2);
        Assert.assertEquals("<item2> has a negative priority and because of that it lower than <item1>.", 1, item1.compareTo(item2));
        Assert.assertEquals("<item2> has a negative priority and because of that it lower than <item1>.", -1, item2.compareTo(item1));

        item1.setPriority(-10);
        Assert.assertEquals("<item1> and <item2> have negative priorities, but <item1>'s is lower than <item2>'s.", -1, item1.compareTo(item2));
        Assert.assertEquals("<item1> and <item2> have negative priorities, but <item2>'s is greater than <item1>'s.", 1, item2.compareTo(item1));
    }
}
