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
package org.mapsforge.routing.evaluation;

import java.io.IOException;
import java.io.Serializable;

import org.mapsforge.routing.GeoCoordinate;

/**
 * Represents a generated route of the Dijkstra rank {@link #dijkstraRank}, starting from the source's
 * geographic position and finishing at the target's geographic position.<br/>
 * Uses geographic positions, because they are independent from all routing graphs. Additionally, it is
 * also the more general use case that we are searching for a route and we only know two geographic
 * positions.
 * 
 * @author Patrick Jungermann
 * @version $Id: Route.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class Route implements Serializable {

	private static final long serialVersionUID = 1717756201968385348L;

	/**
	 * The source's geographic position.
	 */
	public GeoCoordinate source;
	/**
	 * The target's geographic position.
	 */
	public GeoCoordinate target;
	/**
	 * The route's Dijkstra rank.
	 */
	public int dijkstraRank;

	/**
	 * Constructs a {@link Route route} of the specified Dijkstra rank between source's position and
	 * target's position.
	 * 
	 * @param source
	 *            The source's geographic position.
	 * @param target
	 *            The target's geographic position.
	 * @param dijkstraRank
	 *            The route's Dijkstra rank.
	 */
	public Route(final GeoCoordinate source, final GeoCoordinate target, final int dijkstraRank) {
		this.source = source;
		this.target = target;
		this.dijkstraRank = dijkstraRank;
	}

	/**
	 * Writes (serializes) the object to stream.
	 * 
	 * @param out
	 *            The target stream, to which the data has to be written.
	 * @throws IOException
	 *             if there was a problem with writing the data to the stream.
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeInt(source.getLatitudeE6());
		out.writeInt(source.getLongitudeE6());

		out.writeInt(target.getLatitudeE6());
		out.writeInt(target.getLongitudeE6());

		out.writeInt(dijkstraRank);
	}

	/**
	 * Reads a {@link Route} object from the stream.
	 * 
	 * @param in
	 *            The input stream, from which the object has to be read.
	 * @throws IOException
	 *             if there was a problem with reading the object from the stream.
	 * @throws ClassNotFoundException
	 *             if a related class could not be found.
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		final int sourceLatitude = in.readInt();
		final int sourceLongitude = in.readInt();
		source = new GeoCoordinate(sourceLatitude, sourceLongitude);

		final int targetLatitude = in.readInt();
		final int targetLongitude = in.readInt();
		target = new GeoCoordinate(targetLatitude, targetLongitude);

		dijkstraRank = in.readInt();
	}

	@Override
	public String toString() {
		return "Route[from: " + source.toString() + " -> to: " + target.toString() + "]";
	}

}
