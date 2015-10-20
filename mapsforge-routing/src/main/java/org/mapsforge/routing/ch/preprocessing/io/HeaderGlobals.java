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
package org.mapsforge.routing.ch.preprocessing.io;

/**
 * Contains some header related values and header magic bytes, which will be written at the start of the
 * related header and can be used for checking, if a file is likely to contain a valid header or valid
 * entry / object.
 * 
 * @author Patrick Jungermann
 * @version $Id: HeaderGlobals.java 2090 2012-08-05 23:22:18Z Patrick.Jungermann@googlemail.com $
 */
public class HeaderGlobals {

	/**
	 * The header magic bytes written at the start of the header of a binary file. This could be used
	 * for checking, if a file is likely to be a valid binary file.
	 */
	public final static byte[] BINARY_FILE_HEADER_MAGIC = "#MAPSFORGE_CH_BINARY#".getBytes();

	/**
	 * The binary file's header's length.
	 */
	public final static int BINARY_FILE_HEADER_LENGTH = 4096;

	/**
	 * Length of the header of the binary file's part, which contains the graph blocks.
	 */
	public final static int GRAPH_BLOCKS_HEADER_LENGTH = 4096;

	/**
	 * The header magic bytes written at the start of the header of an R-tree. This could be used for
	 * checking, if a file is likely to contain a valid R-tree representation.
	 */
	public static final byte[] STATIC_R_TREE_HEADER_MAGIC = "#MAPSFORGE_CH_STATIC_R_TREE#".getBytes();
}
