package org.mapsforge.poi.searching.polygon;

import org.mapsforge.routing.GeoCoordinate;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Converts longitude values to the same dimension
 * as the latitudes'. (same value/distance proportion)
 * The given geographical coordinate is then represented
 * as a simplified Cartesian coordinate.
 * Note: Not really useful for large areas, because the 
 * spherical distances between longitude values at different
 * latitudes vary.
 * TODO Find a better conversion method which works well with JTS buffer.
 */
public class CoordinateConverter {
	private Coordinate first;
	private Coordinate last;
	private double proportion;
	
	/*
	 * Give two initial coordinates to determine the lat/lon
	 * proportion for conversion.
	 */
	public CoordinateConverter(Coordinate c1, Coordinate c2) {
		this.first = c1;
		this.last = c2;
		
		GeoCoordinate lat1 = new GeoCoordinate(first.x, first.y);
		GeoCoordinate lat2 = new GeoCoordinate(last.x, first.y);
		double latDifference = lat1.sphericalDistance(lat2);
		
		GeoCoordinate lon1 = new GeoCoordinate(first.x, first.y);
		GeoCoordinate lon2 = new GeoCoordinate(first.x, last.y);
		double lonDifference = lon1.sphericalDistance(lon2);
		
		this.proportion = latDifference / lonDifference;
	}
	
	public Coordinate[] geoToCartesian(Coordinate[] geoCoords) {
		Coordinate[] cartesianCoords = null;
		if(geoCoords.length > 0){
			cartesianCoords = new Coordinate[geoCoords.length];
			
			for(int i=0; i<geoCoords.length; ++i){
				cartesianCoords[i] = new Coordinate(geoCoords[i].x, geoCoords[i].y * proportion);
			}
		}
		return cartesianCoords;
	}
	
	public Coordinate[] cartesianToGeo(Coordinate[] cartesianCoords) {
		Coordinate[] geoCoords = null;
		if(cartesianCoords.length > 0){
			geoCoords = new Coordinate[cartesianCoords.length];
			for(int i=0; i<geoCoords.length; ++i){
				geoCoords[i] = new Coordinate(cartesianCoords[i].x, cartesianCoords[i].y / proportion);
			}
		}
		return geoCoords;
	}
	
}
