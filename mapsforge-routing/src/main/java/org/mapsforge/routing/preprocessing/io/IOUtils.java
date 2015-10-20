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
package org.mapsforge.routing.preprocessing.io;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Utility methods, related to I/O operations.
 * 
 * @author Patrick Jungermann
 * @version $Id: IOUtils.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class IOUtils {

	/**
	 * Default buffer size for writing (copying) a file to a stream.
	 */
	private final static int DEFAULT_FILE_COPY_BUFFER_SIZE = 16384 * 1000;

	/**
	 * How many bits needed to encode values of the given range.
	 * 
	 * @param minVal
	 *            minimum value
	 * @param maxVal
	 *            maximum value
	 * @return number of bits required.
	 */
	public static byte numBitsToEncode(final int minVal, final int maxVal) {
		final int interval = maxVal - minVal;
		return (byte) (Math.floor(Math.log(interval) / Math.log(2)) + 1);
	}

	/**
	 * Writes the content of the given {@link File} into the given {@link OutputStream}.
	 * 
	 * @param out
	 *            The target for the data.
	 * @param files
	 *            All files, from which the data has to be appended to the stream. This will be done in
	 *            the order of this array.
	 * @throws IOException
	 *             if there was a problem with writing the data into the stream.
	 */
	public static void writeFilesToStream(final OutputStream out, final File[] files)
			throws IOException {
		writeFilesToStream(out, files, DEFAULT_FILE_COPY_BUFFER_SIZE);
	}

	/**
	 * Writes the content of the given {@link File} into the given {@link OutputStream}.
	 * 
	 * @param out
	 *            The target for the data.
	 * @param files
	 *            All files, from which the data has to be appended to the stream. This will be done in
	 *            the order of this array.
	 * @param bufferSize
	 *            The size of the used buffer, while writing the data into the stream.
	 * @throws IOException
	 *             if there was a problem with writing the data into the stream.
	 */
	public static void writeFilesToStream(final OutputStream out, final File[] files,
			final int bufferSize) throws IOException {
		for (final File file : files) {
			writeFileToStream(out, file, bufferSize);
		}
	}

	/**
	 * Writes the content of the given {@link File} into the given {@link OutputStream}.
	 * 
	 * @param out
	 *            The target for the data.
	 * @param file
	 *            The file, from which the data has to be appended to the stream.
	 * @throws IOException
	 *             if there was a problem with writing the data into the stream.
	 */
	public static void writeFileToStream(final OutputStream out, final File file) throws IOException {
		writeFileToStream(out, file, DEFAULT_FILE_COPY_BUFFER_SIZE);
	}

	/**
	 * Writes the content of the given {@link File} into the given {@link OutputStream}.
	 * 
	 * @param out
	 *            The target for the data.
	 * @param file
	 *            The file, from which the data has to be appended to the stream.
	 * @param bufferSize
	 *            The size of the used buffer, while writing the data into the stream.
	 * @throws IOException
	 *             if there was a problem with writing the data into the stream.
	 */
	public static void writeFileToStream(final OutputStream out, final File file, final int bufferSize)
			throws IOException {
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
			final byte[] buffer = new byte[bufferSize];

			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Fills up the header with empty data, if the header is not of the needed length.
	 * 
	 * @param out
	 *            The output stream, which contains only the header data.<br/>
	 *            (If there is more data inside of it, then you have to add its length to the
	 *            <code>headerLength</code>.)
	 * @param headerLength
	 *            The needed header length.
	 * @throws IOException
	 *             if there was a problem, while writing the empty data to the stream.
	 */
	public static void finalizeHeader(final DataOutputStream out, final int headerLength)
			throws IOException {
		if (out.size() <= headerLength) {
			out.write(new byte[headerLength - out.size()]);

		} else {
			throw new RuntimeException(
					"Not enough space within the header. Possible solution: Increase its length.");
		}
	}
}
