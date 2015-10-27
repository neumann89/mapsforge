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
package org.mapsforge.routing;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Unit tests related to {@link Rect}.
 *
 * @author Patrick Jungermann
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GeoCoordinate.class})
public class RectUnitTests {

    @Test
    public void constructor_byCoordinate_usePositionForMinAndMaxValues() {
        GeoCoordinate position = new GeoCoordinate(14300000, 52400000);
        Rect rect = new Rect(position);

        assertEquals(position, rect.center());

        assertEquals(position.getLongitudeE6(), rect.getMinLongitudeE6());
        assertEquals(position.getLongitudeE6(), rect.getMaxLongitudeE6());
        assertEquals(position.getLatitudeE6(), rect.getMinLatitudeE6());
        assertEquals(position.getLatitudeE6(), rect.getMaxLatitudeE6());
    }

    @Test
    public void constructor_byCoordinateAndDistance_usePositionAsCenterAndDistanceAsDistanceInMetersToBorder() {
        GeoCoordinate position = new GeoCoordinate(14300000, 52400000);
        Rect rect = new Rect(position, 12500);

        GeoCoordinate center = rect.center();
        assertEquals(position.getLatitude(), center.getLatitude(), 0.0001d);
        assertEquals(position.getLongitude(), center.getLongitude(), 0.0001d);

        assertEquals(52284120, rect.getMinLongitudeE6());
        assertEquals(52515879, rect.getMaxLongitudeE6());
        assertEquals(14187710, rect.getMinLatitudeE6());
        assertEquals(14412289, rect.getMaxLatitudeE6());
    }

    @Test
    public void constructor_byMinAndMaxValuesE6_storeTheseValues() {
        Rect rect = new Rect(12300000, 15800000, 48200000, 53400000);

        assertEquals(12300000, rect.getMinLongitudeE6());
        assertEquals(15800000, rect.getMaxLongitudeE6());
        assertEquals(48200000, rect.getMinLatitudeE6());
        assertEquals(53400000, rect.getMaxLatitudeE6());
    }

    @Test
    public void constructor_byMinAndMaxValues_convertToE6AndStoreThat() {
        Rect rect = new Rect(12.3, 15.8, 48.2, 53.4);

        assertEquals(12300000, rect.getMinLongitudeE6());
        assertEquals(15800000, rect.getMaxLongitudeE6());
        assertEquals(48200000, rect.getMinLatitudeE6());
        assertEquals(53400000, rect.getMaxLatitudeE6());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_byMinAndMaxValuesE6AndMinLatGreaterThanMaxLat_validationFailedWithException() {
        new Rect(12300001, 12300000, 48200000, 53400000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_byMinAndMaxValuesE6AndMinLngGreaterThanMaxLng_validationFailedWithException() {
        new Rect(12300000, 15800000, 48200001, 48200000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_byMinAndMaxValuesAndMinLatGreaterThanMaxLat_validationFailedWithException() {
        new Rect(12.300001, 12.300000, 48.200000, 53.400000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_byMinAndMaxValuesAndMinLngGreaterThanMaxLng_validationFailedWithException() {
        new Rect(12.300000, 15.800000, 48.200001, 48.200000);
    }

    @Test
    public void constructor_byRect_useItsMinAndMaxValues() {
        Rect rect = new Rect(12300000, 15800000, 48200000, 53400000);
        Rect copy = new Rect(rect);

        assertEquals(rect.getMinLongitudeE6(), copy.getMinLongitudeE6());
        assertEquals(rect.getMaxLongitudeE6(), copy.getMaxLongitudeE6());
        assertEquals(rect.getMinLatitudeE6(), copy.getMinLatitudeE6());
        assertEquals(rect.getMaxLatitudeE6(), copy.getMaxLatitudeE6());
    }

    @Test
    public void center_returnCenterOfRect() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);

        assertEquals(new GeoCoordinate(50500000, 500000), rect.center());
    }

    @Test
    public void distance_overlapping_returnZero() {
        Rect rect = new Rect(1000000, 2000000, 1000000, 2000000);
        Rect other = new Rect(1500000, 6000000, 1500000, 6000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(true);

        assertEquals(0d, spyRect.distance(other), 0d);
    }

    @Test
    public void distance_notOverlappingAndOtherOnRightTop_returnSphericalDistanceFromTopRightCornerToBottomLeftCornerOfOther() {
        Rect rect = new Rect(1000000, 2000000, 1000000, 3000000);
        Rect other = new Rect(4000000, 6000000, 5000000, 6000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(false);

        mockStatic(GeoCoordinate.class);
        when(
                GeoCoordinate.sphericalDistance(2000000, 3000000, 4000000, 5000000)
        ).thenReturn(1000d);

        assertEquals(1000d, spyRect.distance(other), 0d);

        verifyStatic();
    }

    @Test
    public void distance_notOverlappingAndOtherOnRightBottom_returnSphericalDistanceFromBottomRightCornerToTopLeftCornerOfOther() {
        Rect rect = new Rect(4000000, 6000000, 1000000, 2000000);
        Rect other = new Rect(1000000, 2000000, 4000000, 6000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(false);

        mockStatic(GeoCoordinate.class);
        when(
                GeoCoordinate.sphericalDistance(4000000, 2000000, 2000000, 4000000)
        ).thenReturn(1000d);

        assertEquals(1000d, spyRect.distance(other), 0d);

        verifyStatic();
    }

    @Test
    public void distance_notOverlappingAndOtherOnRightWithOverlappingLonRange_returnSphericalDistanceWithHighestCommonLonValueForBothRefPoints() {
        Rect rect = new Rect(1000000, 4000000, 1000000, 3000000);
        Rect other = new Rect(2000000, 6000000, 5000000, 6000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(false);

        mockStatic(GeoCoordinate.class);
        when(
                GeoCoordinate.sphericalDistance(4000000, 3000000, 4000000, 5000000)
        ).thenReturn(1000d);

        assertEquals(1000d, spyRect.distance(other), 0d);

        verifyStatic();
    }

    @Test
    public void distance_notOverlappingAndOtherOnLeftTop_returnSphericalDistanceFromLeftTopCornerToRightBottomCornerOfOther() {
        Rect rect = new Rect(1000000, 2000000, 5000000, 6000000);
        Rect other = new Rect(4000000, 6000000, 1000000, 3000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(false);

        mockStatic(GeoCoordinate.class);
        when(
                GeoCoordinate.sphericalDistance(2000000, 5000000, 4000000, 3000000)
        ).thenReturn(1000d);

        assertEquals(1000d, spyRect.distance(other), 0d);

        verifyStatic();
    }

    @Test
    public void distance_notOverlappingAndOtherOnLeftBottom_returnSphericalDistanceFromLeftBottomCornerToRightTopCornerOfOther() {
        Rect rect = new Rect(4000000, 6000000, 5000000, 6000000);
        Rect other = new Rect(1000000, 2000000, 1000000, 3000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(false);

        mockStatic(GeoCoordinate.class);
        when(
                GeoCoordinate.sphericalDistance(4000000, 5000000, 2000000, 3000000)
        ).thenReturn(1000d);

        assertEquals(1000d, spyRect.distance(other), 0d);

        verifyStatic();
    }

    @Test
    public void distance_notOverlappingAndOtherOnLeftWithOverlappingLonRange_returnSphericalDistanceWithHighestCommonLonValueForBothRefPoints() {
        Rect rect = new Rect(1000000, 4000000, 5000000, 6000000);
        Rect other = new Rect(2000000, 6000000, 1000000, 3000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(false);

        mockStatic(GeoCoordinate.class);
        when(
                GeoCoordinate.sphericalDistance(4000000, 5000000, 4000000, 3000000)
        ).thenReturn(1000d);

        assertEquals(1000d, spyRect.distance(other), 0d);

        verifyStatic();
    }

    @Test
    public void distance_notOverlappingAndOtherOnTopWithOverlappingLatRange_returnSphericalDistanceWithHighestCommonLatValueForBothRefPoints() {
        Rect rect = new Rect(1000000, 2000000, 1000000, 5000000);
        Rect other = new Rect(4000000, 6000000, 3000000, 6000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(false);

        mockStatic(GeoCoordinate.class);
        when(
                GeoCoordinate.sphericalDistance(2000000, 5000000, 4000000, 5000000)
        ).thenReturn(1000d);

        assertEquals(1000d, spyRect.distance(other), 0d);

        verifyStatic();
    }

    @Test
    public void distance_notOverlappingOnBottomWithOverlappingLatRange_returnSphericalDistanceWitHighestCommonLatValueForBothRefPoints() {
        Rect rect = new Rect(4000000, 6000000, 1000000, 5000000);
        Rect other = new Rect(1000000, 2000000, 3000000, 6000000);

        Rect spyRect = spy(rect);
        when(
                spyRect.overlaps(other)
        ).thenReturn(false);

        mockStatic(GeoCoordinate.class);
        when(
                GeoCoordinate.sphericalDistance(4000000, 5000000, 2000000, 5000000)
        ).thenReturn(1000d);

        assertEquals(1000d, spyRect.distance(other), 0d);

        verifyStatic();
    }

    @Ignore
    @Test
    public void equals_() {
        fail();
    }

    @Ignore
    @Test
    public void expandToInclude_() {
        fail();
    }

    @Ignore
    @Test
    public void hashCode_() {
        fail();
    }

    @Ignore
    @Test
    public void includes_() {
        fail();
    }

    @Ignore
    @Test
    public void overlaps_() {
        fail();
    }

}
