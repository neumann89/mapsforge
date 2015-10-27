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
package org.mapsforge.routing.ch.preprocessing.data;

import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * Tests for the class {@link PriorityQueueItem}.
 *
 * @author Patrick Jungermann
 * @version $Id: PriorityQueueItemTest.java 1918 2012-03-13 19:15:41Z Patrick.Jungermann@googlemail.com $
 */
public class PriorityQueueItemUnitTests {

    @Test
    public void getPayload_noCustomPayloadUsed_returnNull() {
        PriorityQueueItem<Integer, Float> item = new PriorityQueueItem<Integer, Float>(1, 10);

        assertNull("<item> has no payload.", item.getPayload());
    }
}
