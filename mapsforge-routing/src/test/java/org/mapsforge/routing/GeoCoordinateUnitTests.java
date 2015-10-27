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
 * Unit tests related to {@link GeoCoordinate}.
 *
 * @author Patrick Jungermann
 * @author Eike Send
 */
public class GeoCoordinateUnitTests {

    // This is the length of earth's equatorial circumference in meters
    double earthEquatorCircumference = WGS84.EQUATORIAL_RADIUS * 2 * Math.PI;

    // Here is a fun fact:
    // The original meter was defined as 1/10.000.000 the distance between the north pole
    // and the equator on the Paris meridian. The prototype which created turned out to be
    // very good, only hundreds of years later, after the meter definition had changed, it
    // was found out, that the distance measured in this meter was actually more than
    // 10.000.000 meters
    double distancePoleToEquator = 10001966.0; // 10.001,966 km


    @Test(expected = IllegalArgumentException.class)
    public void validateLatitude_lowerThanMinValue_throwException() {
        GeoCoordinate.validateLatitude(GeoCoordinate.LATITUDE_MIN - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateLatitude_higherThanMaxValue_throwException() {
        GeoCoordinate.validateLatitude(GeoCoordinate.LATITUDE_MAX + 1);
    }

    @Test
    public void validateLatitude_legalValue_returnThatValue() {
        assertEquals(10d, GeoCoordinate.validateLatitude(10d), 0d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateLongitude_lowerThanMinValue_throwException() {
        GeoCoordinate.validateLatitude(GeoCoordinate.LONGITUDE_MIN - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateLongitude_higherThanMaxValue_throwException() {
        GeoCoordinate.validateLatitude(GeoCoordinate.LONGITUDE_MAX + 1);
    }

    @Test
    public void validateLongitude_legalValue_returnThatValue() {
        assertEquals(10d, GeoCoordinate.validateLongitude(10d), 0d);
    }

    @Test
    public void constructor_byDoubleValues_createInstance() {
        GeoCoordinate alex = new GeoCoordinate(52.52235, 13.4125);

        assertEquals(52.52235, alex.getLatitude(), 0d);
        assertEquals(13.4125, alex.getLongitude(), 0d);
    }

    @Test
    public void constructor_byIntValues_createInstance() {
        GeoCoordinate alex = new GeoCoordinate(52522350, 13412500);

        assertEquals(52522350, alex.getLatitudeE6());
        assertEquals(13412500, alex.getLongitudeE6());
    }

    @Test
    public void constructor_byWellKnownText_createInstance() {
        GeoCoordinate alex = new GeoCoordinate("POINT(13.4125 52.52235)");

        assertEquals(52.52235, alex.getLatitude(), 0d);
        assertEquals(13.4125, alex.getLongitude(), 0d);
    }

    @Test
    public void equals_sameInstance_returnTrue() {
        GeoCoordinate bundestag = new GeoCoordinate(52518590, 13375400);

        assertTrue(bundestag.equals(bundestag));
    }

    @Test
    public void equals_sameCoordinateValues_returnTrue() {
        GeoCoordinate bundestag = new GeoCoordinate(52518590, 13375400);
        GeoCoordinate bundestag2 = new GeoCoordinate(52.518590, 13.3754);

        assertTrue(bundestag.equals(bundestag2));
    }

    @Test
    public void equals_differentCoordinateValues_returnFalse() {
        GeoCoordinate alex = new GeoCoordinate(52522350, 13412500);
        GeoCoordinate bundestag = new GeoCoordinate(52518590, 13375400);

        assertFalse(bundestag.equals(alex));
    }

    @Test
    public void equals_differentObjectType_returnFalse() {
        GeoCoordinate geoCoordinate = new GeoCoordinate(52518590, 13375400);

        assertFalse(geoCoordinate.equals(new Object()));
    }

    @Test
    public void sphericalDistance_originToNearOfSriLanka_returnQuarterOfEarthEquatorCircumference() {
        // This is the origin of the WGS-84 reference system
        GeoCoordinate zeroZero = new GeoCoordinate(0d, 0d);
        // These coordinates are 1/4 Earth circumference from zeroZero on the equator
        GeoCoordinate nearSriLanka = new GeoCoordinate(0d, 90d);

        double spherical = GeoCoordinate.sphericalDistance(zeroZero, nearSriLanka);
        assertEquals(earthEquatorCircumference / 4, spherical, 0d);
    }

    @Test
    public void vincentyDistance_originToNearOfSriLanka_returnQuarterOfEarthEquatorCircumference() {
        // This is the origin of the WGS-84 reference system
        GeoCoordinate zeroZero = new GeoCoordinate(0d, 0d);
        // These coordinates are 1/4 Earth circumference from zeroZero on the equator
        GeoCoordinate nearSriLanka = new GeoCoordinate(0d, 90d);

        double vincenty = GeoCoordinate.vincentyDistance(zeroZero, nearSriLanka);
        assertEquals(earthEquatorCircumference / 4, vincenty, 1E-4);
    }

    @Test
    public void sphericalDistanceAndVincentyDistance_originToNearOfSriLanka_bothShouldBeNearlyTheSame() {
        // This is the origin of the WGS-84 reference system
        GeoCoordinate zeroZero = new GeoCoordinate(0d, 0d);
        // These coordinates are 1/4 Earth circumference from zeroZero on the equator
        GeoCoordinate nearSriLanka = new GeoCoordinate(0d, 90d);

        // On the equator the result of the different distance calculation methods should be
        // about the same
        double spherical = GeoCoordinate.sphericalDistance(zeroZero, nearSriLanka);
        double vincenty = GeoCoordinate.vincentyDistance(zeroZero, nearSriLanka);
        assertEquals(spherical, vincenty, 1E-4);
    }

    @Test
    public void sphericalDistance_originToIslaGenovesa_returnQuarterOfEarthEquatorCircumference() {
        // This is the origin of the WGS-84 reference system
        GeoCoordinate zeroZero = new GeoCoordinate(0d, 0d);
        // These coordinates are also 1/4 Earth circumference from zero on the equator
        GeoCoordinate islaGenovesa = new GeoCoordinate(0d, -90d);

        double spherical = GeoCoordinate.sphericalDistance(zeroZero, islaGenovesa);
        assertEquals(earthEquatorCircumference / 4, spherical, 0d);
    }

    @Test
    public void sphericalDistance_nearOfSriLankaToIslaGenovesa_returnHalfOfEarthEquatorCircumference() {
        // These coordinates are 1/4 Earth circumference from zeroZero on the equator
        GeoCoordinate nearSriLanka = new GeoCoordinate(0d, 90d);
        // These coordinates are also 1/4 Earth circumference from zero on the equator
        GeoCoordinate islaGenovesa = new GeoCoordinate(0d, -90d);

        // These points are as far apart as they could be, half way around the earth
        double spherical = GeoCoordinate.sphericalDistance(nearSriLanka, islaGenovesa);
        assertEquals(earthEquatorCircumference / 2, spherical, 0d);
    }

    @Test
    public void sphericalDistance_originToNorthPole_returnQuarterOfEarthEquatorCircumference() {
        // This is the origin of the WGS-84 reference system
        GeoCoordinate zeroZero = new GeoCoordinate(0d, 0d);
        // Calculating the distance between the north pole and the equator
        GeoCoordinate northPole = new GeoCoordinate(90d, 0d);

        double spherical = GeoCoordinate.sphericalDistance(zeroZero, northPole);
        assertEquals(earthEquatorCircumference / 4, spherical, 0d);
    }

    @Test
    public void vincentyDistance_originToNorthPole_returnDistanceFromPoleToEquator() {
        // This is the origin of the WGS-84 reference system
        GeoCoordinate zeroZero = new GeoCoordinate(0d, 0d);
        // Calculating the distance between the north pole and the equator
        GeoCoordinate northPole = new GeoCoordinate(90d, 0d);

        double vincenty = GeoCoordinate.vincentyDistance(zeroZero, northPole);
        assertEquals(distancePoleToEquator, vincenty, 1);
    }

    @Test
    public void vincentyDistance_southPoleToNorthPole_returnTwiceOfDistanceFromPoleToEquator() {
        // Calculating the distance between the north pole and the equator
        GeoCoordinate northPole = new GeoCoordinate(90d, 0d);
        // Check if the distance from pole to pole works as well in the vincentyDistance
        GeoCoordinate southPole = new GeoCoordinate(-90d, 0d);

        double vincenty = GeoCoordinate.vincentyDistance(southPole, northPole);
        assertEquals(2 * distancePoleToEquator, vincenty, 1);
    }

}
