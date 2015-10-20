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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration tests related to {@link Rect}.
 *
 * @author Patrick Jungermann
 */
public class RectIntegrationTests {

    @Test(expected = IllegalArgumentException.class)
    public void fromString_notFourCommaSeparatedArguments_throwException() {
        Rect.fromString("one,two");
    }

    @Test
    public void fromString_validString_returnRect() {
        Rect rect = Rect.fromString("52.3,13.4,52.4,13.6");

        assertEquals(52300000, rect.getMinLatitudeE6());
        assertEquals(13400000, rect.getMinLongitudeE6());
        assertEquals(52400000, rect.getMaxLatitudeE6());
        assertEquals(13600000, rect.getMaxLongitudeE6());
    }

    @Test
    public void constructor_byCenterAndDistance_rectReturnsTheSameCenter() {
        GeoCoordinate center = new GeoCoordinate(52200000, 13400000);
        int distance = 200;
        Rect rect = new Rect(center, distance);

        assertEquals("Wrong rectangular's center: Wrong latitude value.",
                center.getLatitude(), rect.center().getLatitude(), 0.0001);
        assertEquals("Wrong rectangular's center: Wrong longitude value.",
                center.getLongitude(), rect.center().getLongitude(), 0.0001);
    }

    @Test
    public void constructor_byCenterAndDistance_rectReturnsTheExpectedBoundary() {
        GeoCoordinate center = new GeoCoordinate(52200000, 13400000);
        int distance = 200;
        Rect rect = new Rect(center, distance);

        double alphaLat = GeoCoordinate.latitudeDistance(distance);
        int minLat = GeoCoordinate.doubleToInt(center.getLatitude() - alphaLat);
        int maxLat = GeoCoordinate.doubleToInt(center.getLatitude() + alphaLat);
        assertEquals("Wrong min. latitude.", minLat, rect.getMinLatitudeE6());
        assertEquals("Wrong max. latitude.", maxLat, rect.getMaxLatitudeE6());

        double alphaLon = GeoCoordinate.longitudeDistance(distance, center.getLatitude());
        int minLon = GeoCoordinate.doubleToInt(center.getLongitude() - alphaLon);
        int maxLon = GeoCoordinate.doubleToInt(center.getLongitude() + alphaLon);
        assertEquals("Wrong min. longitude.", minLon, rect.getMinLongitudeE6());
        assertEquals("Wrong max. longitude.", maxLon, rect.getMaxLongitudeE6());
    }

    @Test
    public void center_returnCenterOfRect() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);

        assertEquals(new GeoCoordinate(50500000, 500000), rect.center());
    }

    @Test
    public void distance_sameRect_returnZero() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(0, 1000000, 50000000, 51000000);

        assertEquals(0d, rect.distance(other), 0d);
    }

    @Test
    public void distance_singleBoundaryInCommon_returnZero() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(-1000000, 0, 45000000, 55000000);

        assertEquals(0d, rect.distance(other), 0d);
    }

    @Test
    public void distance_inclusion_returnZero() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(-1000000, 2000000, 45000000, 55000000);

        assertEquals(0, rect.distance(other), 0);
    }

    @Test
    public void distance_otherOnTopRight_returnDistanceBetweenTopRightCornerAndBottomLeftCornerOfOther() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(2000000, 3000000, 52000000, 53000000);

        double expected = GeoCoordinate.sphericalDistance(1000000, 51000000, 2000000, 52000000);
        assertEquals(expected, rect.distance(other), 0d);
    }

    @Test
    public void distance_otherOnBottomLeft_returnDistanceBetweenBottomLeftCornerAndTopRightCornerOfOther() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(-2000000, -1000000, 48000000, 49000000);

        double expected = GeoCoordinate.sphericalDistance(0, 50000000, -1000000, 49000000);
        assertEquals(expected, rect.distance(other), 0d);
    }

    @Test
    public void distance_otherOnBottomAndLarger_returnDistanceBetweenBottomAndTopOfOtherWithHighestCommonLonValue() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(-2000000, 2000000, 48000000, 49000000);

        double expected = GeoCoordinate.sphericalDistance(0, 50000000, 0, 49000000);
        assertEquals(expected, rect.distance(other), 0d);
    }

    @Test
    public void expandToInclude_forGeoCoordinate_onBoundary_noExpansion() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        GeoCoordinate geoCoordinate = new GeoCoordinate(50000000, 0);

        rect.expandToInclude(geoCoordinate);

        Rect expected = new Rect(0, 1000000, 50000000, 51000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forGeoCoordinate_inBoundary_noExpansion() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        GeoCoordinate geoCoordinate = new GeoCoordinate(50500000, 500000);

        rect.expandToInclude(geoCoordinate);

        Rect expected = new Rect(0, 1000000, 50000000, 51000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forGeoCoordinate_leftOfBoundary_expansionAtLeft() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        GeoCoordinate geoCoordinate = new GeoCoordinate(49000000, 500000);

        rect.expandToInclude(geoCoordinate);

        Rect expected = new Rect(0, 1000000, 49000000, 51000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forGeoCoordinate_rightOfBoundary_expansionAtRight() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        GeoCoordinate geoCoordinate = new GeoCoordinate(52000000, 500000);

        rect.expandToInclude(geoCoordinate);

        Rect expected = new Rect(0, 1000000, 50000000, 52000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forGeoCoordinate_topOfBoundary_expansionAtTheTop() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        GeoCoordinate geoCoordinate = new GeoCoordinate(50500000, 1100000);

        rect.expandToInclude(geoCoordinate);

        Rect expected = new Rect(0, 1100000, 50000000, 51000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forGeoCoordinate_bottomOfBoundary_expansionAtTheBottom() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        GeoCoordinate geoCoordinate = new GeoCoordinate(50500000, -1000000);

        rect.expandToInclude(geoCoordinate);

        Rect expected = new Rect(-1000000, 1000000, 50000000, 51000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forRect_overlapping_expansionToIncludeThePartAtTheOutside() {
        Rect rect = new Rect(0, 1000000, 50000000, 56000000);
        Rect other = new Rect(-1000000, 1000000, 50000000, 51000000);

        rect.expandToInclude(other);

        Rect expected = new Rect(-1000000, 1000000, 50000000, 56000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forRect_includesOther_noExpansion() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(250000, 750000, 50250000, 50750000);

        rect.expandToInclude(other);

        Rect expected = new Rect(0, 1000000, 50000000, 51000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forRect_includedByOther_expandToBoundaryOfOther() {
        Rect rect = new Rect(250000, 750000, 50250000, 50750000);
        Rect other = new Rect(0, 1000000, 50000000, 51000000);

        rect.expandToInclude(other);

        assertEquals(other, rect);
    }

    @Test
    public void expandToInclude_forRect_sameRect_noExpansion() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(0, 1000000, 50000000, 51000000);

        rect.expandToInclude(other);

        Rect expected = new Rect(0, 1000000, 50000000, 51000000);
        assertEquals(expected, rect);
    }

    @Test
    public void expandToInclude_forRect_atTheOutside_expandToIncludeOther() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(-2000000, -1000000, 45000000, 49000000);

        rect.expandToInclude(other);

        Rect expected = new Rect(-2000000, 1000000, 45000000, 51000000);
        assertEquals(expected, rect);
    }

    @Test
    public void overlaps_sameRect_returnTrue() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(0, 1000000, 50000000, 51000000);

        assertTrue(rect.overlaps(other));
    }

    @Test
    public void overlaps_singleBoundaryInCommon_returnTrue() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(-1000000, 0, 45000000, 55000000);

        assertTrue(rect.overlaps(other));
    }

    @Test
    public void overlaps_includedByOther_returnTrue() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(-1000000, 2000000, 45000000, 55000000);

        assertTrue(rect.overlaps(other));
    }

    @Test
    public void overlaps_includesOther_returnTrue() {
        Rect rect = new Rect(-1000000, 2000000, 45000000, 55000000);
        Rect other = new Rect(0, 1000000, 50000000, 51000000);

        assertTrue(rect.overlaps(other));
    }

    @Test
    public void overlaps_atTheOutside_returnFalse() {
        Rect rect = new Rect(0, 1000000, 50000000, 51000000);
        Rect other = new Rect(0, 1000000, 55000000, 56000000);

        assertFalse(rect.overlaps(other));
    }

}
