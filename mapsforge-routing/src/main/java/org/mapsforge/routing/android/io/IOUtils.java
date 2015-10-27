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
package org.mapsforge.routing.android.io;

import gnu.trove.list.array.TByteArrayList;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Utility methods, related to I/O operations.
 * 
 * @author Patrick Jungermann
 * @version $Id: IOUtils.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class IOUtils {

	/**
	 * Symbol used to terminate strings inside of byte data.
	 */
	public static final byte TERMINATION_SYMBOL = (byte) 0;

	/**
	 * Returns the byte data of the next string, which was terminated by a zero (the termination symbol,
	 * {@see #TERMINATION_SYMBOL}).
	 * 
	 * @param data
	 *            The array, which contains the data, from which the string data has to be extracted.
	 * @param byteOffset
	 *            The byte offset, from which to start the search for the string.
	 * @return The byte data of the next string
	 */
	public static byte[] getZeroTerminatedString(final byte[] data, final int byteOffset) {
		if (data == null || byteOffset >= data.length) {
			return new byte[0];
		}

		// find end of the string
		int i = byteOffset;
		while (data[i] != TERMINATION_SYMBOL) {
			i++;

			if (i == data.length) {
				// no end / no termination symbol was found
				return new byte[0];
			}
		}

		final byte[] stringBytes = new byte[i - byteOffset];
		System.arraycopy(data, byteOffset, stringBytes, 0, stringBytes.length);

		return stringBytes;
	}

	/**
	 * Reads and returns the byte data of the next string and converts it with regards to the selected
	 * character set into a string.
	 * 
	 * @param dis
	 *            The input stream, from which the data will be retrieved.
	 * @param charsetName
	 *            The character set, which will be used for converting the byte data to a string.
	 * @return The byte data of the next string.
	 * @throws IOException
	 *             if there was a problem while reading the bytes from the stream.
	 */
	public static String getZeroTerminatedString(final DataInputStream dis, final String charsetName)
			throws IOException {
		final byte[] data = getZeroTerminatedStringData(dis);
		return data.length > 0 ? new String(data, charsetName) : null;
	}

	/**
	 * Reads and returns the byte data of the next string.
	 * 
	 * @param dis
	 *            The input stream, from which the data will be retrieved.
	 * @return The byte data of the next string.
	 * @throws IOException
	 *             if there was a problem while reading the bytes from the stream.
	 */
	public static byte[] getZeroTerminatedStringData(final DataInputStream dis) throws IOException {
		final TByteArrayList bytes = new TByteArrayList();

		byte b;
		while ((b = dis.readByte()) != TERMINATION_SYMBOL) {
			bytes.add(b);
		}

		return bytes.toArray();
	}
}
