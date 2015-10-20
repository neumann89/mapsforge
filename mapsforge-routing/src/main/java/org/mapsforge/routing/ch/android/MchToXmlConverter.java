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
package org.mapsforge.routing.ch.android;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.mapsforge.routing.android.data.AddressLookupTable;
import org.mapsforge.routing.android.data.Deserializer;
import org.mapsforge.routing.android.data.Offset;
import org.mapsforge.routing.android.data.Pointer;
import org.mapsforge.routing.android.data.StaticRTree;
import org.mapsforge.routing.android.io.IOUtils;
import org.mapsforge.routing.ch.preprocessing.io.HeaderGlobals;
import org.mapsforge.routing.preprocessing.data.ArrayUtils;

/**
 * A converter for converting an MCH file (binary file format for Mobile Contraction Hierarchies
 * routing) into an XML format representation. The result is not an XML format, optimized for the
 * contained data itself but for representing the MCH file, its content and structure. E.g., most of the
 * tags have an attribute <tt>offset</tt>, which contains the offset of the data within the file or the
 * related block.
 * 
 * @author Patrick Jungermann
 * @version $Id: MchToXmlConverter.java 1746 2012-01-16 22:38:34Z Patrick.Jungermann@googlemail.com $
 */
public class MchToXmlConverter {

	/**
	 * Example use of the converter, converting the file <tt>berlin.mch</tt> at the directory
	 * <tt>data/binary</tt> to the target file <tt>berlin.xml</tt>.
	 * 
	 * @param args
	 *            Arguments. Not used here.
	 * @throws IOException
	 *             if there was a problem at the conversion.
	 */
	public static void main(String[] args) throws IOException {
		File dir = new File("data/binary");

		convertToXml(new File(dir, "berlin.mch"), new File(dir, "berlin_200.xml"), new int[] { 200 });
		convertToXml(new File(dir, "berlin.mch"), new File(dir, "berlin.xml"));
	}

	/**
	 * Converts an MCH file (binary file format for Mobile Contraction Hierarchies routing) into an XML
	 * format representation.<br/>
	 * This is not an XML format optimized for representing the data itself but for representing the
	 * file, its content and structure. It also contains offsets of the different data in the form of
	 * <tt>byteOffset:bitOffset</tt>.
	 * 
	 * @param mch
	 *            The MCH file.
	 * @param target
	 *            The target file, to which the XML data has to be written.
	 * @throws IOException
	 *             if there is a problem with writing the data to the file.
	 * @see #convert(java.io.File)
	 */
	public static void convertToXml(final File mch, final File target) throws IOException {
		convertToXml(mch, target, null);
	}

	/**
	 * Converts an MCH file (binary file format for Mobile Contraction Hierarchies routing) into an XML
	 * format representation.<br/>
	 * This is not an XML format optimized for representing the data itself but for representing the
	 * file, its content and structure. It also contains offsets of the different data in the form of
	 * <tt>byteOffset:bitOffset</tt>.
	 * 
	 * @param mch
	 *            The MCH file.
	 * @param target
	 *            The target file, to which the XML data has to be written.
	 * @param blockIds
	 *            The identifiers of all blocks, which have to be converted and written to the file. All
	 *            other blocks will be ignored.
	 * @throws IOException
	 *             if there is a problem with writing the data to the file.
	 * @see #convert(java.io.File)
	 */
	public static void convertToXml(final File mch, final File target, final int[] blockIds)
			throws IOException {
		new MchToXmlConverter(mch).convert(target, blockIds);
	}

	private final RandomAccessFile raf;
	/**
	 * Start address of the graph within the MCH file.
	 */
	private final long startAddressGraph;
	/**
	 * Start address of the block index (address lookup table) within the MCH file.
	 */
	private final long startAddressIndex;
	/**
	 * Start address of the R-tree within the MCH file.
	 */
	private final long startAddressRTree;
	/**
	 * Start address of the graph's blocks within the MCH file.
	 */
	private final long startAddressGraphBlocks;
	/**
	 * Whether the file contains debug information or not.
	 */
	private final boolean debug;
	/**
	 * Number of bits used to encode one block identifier.
	 */
	private final byte bitsPerBlockId;
	/**
	 * Number of bits used to encode one vertex offset.
	 */
	private final byte bitsPerVertexOffset;
	/**
	 * Number of bits used to encode one edge weight.
	 */
	private final byte bitsPerEdgeWeight;
	/**
	 * Number of bits used to encode one street type identifier.
	 */
	private final byte bitsPerStreetType;
	/**
	 * All street types.
	 */
	private final String[] streetTypes;
	/**
	 * The block index (address lookup table) of the MCH file.
	 */
	private final AddressLookupTable blockIndex;
	/**
	 * The R-tree of the MCH file.
	 */
	private final StaticRTree rTree;

	/**
	 * Creates a converter for converting the MCH file into an XML format.
	 * 
	 * @param mchFile
	 *            The MCH file.
	 * @throws IOException
	 *             if there was a problem with accessing or reading the MCH file.
	 */
	MchToXmlConverter(final File mchFile) throws IOException {
		// fetch header
		raf = new RandomAccessFile(mchFile, "r");
		byte[] header = new byte[HeaderGlobals.BINARY_FILE_HEADER_LENGTH];
		raf.seek(0);
		raf.readFully(header);

		DataInputStream dis = null;
		try {
			// read header
			dis = new DataInputStream(new ByteArrayInputStream(header));

			// verify header
			if (!isValidHeader(dis)) {
				throw new IOException("Invalid header.");
			}

			// get all start addresses
			startAddressGraph = dis.readLong();
			startAddressIndex = dis.readLong();
			startAddressRTree = dis.readLong();

			startAddressGraphBlocks = startAddressGraph + HeaderGlobals.GRAPH_BLOCKS_HEADER_LENGTH;

			dis.close();
			dis = null;

			// read the graph header
			raf.seek(startAddressGraph);
			header = new byte[HeaderGlobals.GRAPH_BLOCKS_HEADER_LENGTH];
			raf.readFully(header);

			// extract all information from the graph header.
			dis = new DataInputStream(new ByteArrayInputStream(header));
			debug = dis.readBoolean();
			bitsPerBlockId = dis.readByte();
			bitsPerVertexOffset = dis.readByte();
			bitsPerEdgeWeight = dis.readByte();
			bitsPerStreetType = dis.readByte();
			streetTypes = readStreetTypes(dis);

			// basic components
			rTree = new StaticRTree(mchFile, startAddressRTree, raf.length(),
					HeaderGlobals.STATIC_R_TREE_HEADER_MAGIC);
			blockIndex = new AddressLookupTable(startAddressIndex, startAddressRTree, mchFile);

		} finally {
			if (dis != null) {
				dis.close();
			}
		}
	}

	/**
	 * Verifies the binary file's header based on its header magic.
	 * 
	 * @param dis
	 *            The input stream, containing the data of the header.
	 * @return {@code true}, if it's a valid header, otherwise {@code false}.
	 * @throws IOException
	 *             if there was any problem related to the reading of the data from the input stream.
	 * @see CHGraph#isValidHeader(java.io.DataInputStream)
	 */
	protected boolean isValidHeader(final DataInputStream dis) throws IOException {
		byte[] headerMagic = new byte[HeaderGlobals.BINARY_FILE_HEADER_MAGIC.length];

		return headerMagic.length == dis.read(headerMagic)
				&& Arrays.equals(headerMagic, HeaderGlobals.BINARY_FILE_HEADER_MAGIC);
	}

	/**
	 * Reads and returns all the street types from the input stream.
	 * 
	 * @param dis
	 *            The input stream, which contains the street types.
	 * @return All the extracted street types.
	 * @throws IOException
	 *             if there was a problem with reading the street types.
	 * @see CHGraph#isValidHeader(java.io.DataInputStream)
	 */
	protected String[] readStreetTypes(final DataInputStream dis) throws IOException {
		final int num = dis.readByte();
		final String[] types = new String[num];

		for (int i = 0; i < num; i++) {
			types[i] = IOUtils.getZeroTerminatedString(dis, "UTF-8");
		}

		return types;
	}

	/**
	 * Returns the XML-encoded {@link String string}, which can than be used as value of XML tags.
	 * 
	 * @param str
	 *            The {@link String string}, which has to be encoded.
	 * @return The XML-encoded {@link String string}, which can than be used as value of XML tags.
	 */
	protected String encodeAsXml(String str) {
		return str.replace("&", "&#38;").replace("<", "&#60;").replace(">", "&#62;")
				.replace("\"", "&#34;").replace("'", "&#39;");
	}

	/**
	 * Writes an XML tag with the name and value to the stream.
	 * 
	 * @param out
	 *            The target stream.
	 * @param name
	 *            The tag's name.
	 * @param value
	 *            The tag's value.
	 * @throws IOException
	 *             if there was a problem with writing the tag to the stream.
	 */
	protected void writeTag(final OutputStreamWriter out, final String name, final Object value)
			throws IOException {
		out.write(String.format("<%s>%s</%s>", name, encodeAsXml(value.toString()), name));
	}

	/**
	 * Converts an MCH file (binary file format for Mobile Contraction Hierarchies routing) into an XML
	 * format representation.<br/>
	 * This is not an XML format optimized for representing the data itself but for representing the
	 * file, its content and structure. It also contains offsets of the different data in the form of
	 * <tt>byteOffset:bitOffset</tt>.
	 * 
	 * @param target
	 *            The target file, to which the XML data has to be written.
	 * @throws IOException
	 *             if there is a problem with reading the data from the MCH file or writing the data to
	 *             the target file.
	 * @see #convert(java.io.File, int[])
	 */
	protected void convert(final File target) throws IOException {
		convert(target, null);
	}

	/**
	 * Converts an MCH file (binary file format for Mobile Contraction Hierarchies routing) into an XML
	 * format representation.<br/>
	 * This is not an XML format optimized for representing the data itself but for representing the
	 * file, its content and structure. It also contains offsets of the different data in the form of
	 * <tt>byteOffset:bitOffset</tt>.
	 * 
	 * @param target
	 *            The target file, to which the XML data has to be written.
	 * @param blockIds
	 *            The identifiers of all blocks, which have to be converted and written to the file. All
	 *            other blocks will be ignored.
	 * @throws IOException
	 *             if there is a problem with reading the data from the MCH file or writing the data to
	 *             the target file.
	 */
	protected void convert(final File target, final int[] blockIds) throws IOException {
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(target), "UTF-8");

		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		out.write(String.format("<mch length=\"%d\">", raf.length()));

		writeHeader(out);
		writeGraph(out, blockIds);
		writeAddressLookupTable(out);
		writeRTree(out);

		out.write("</mch>");

		out.close();
	}

	/**
	 * Writes the header tag, which contains the header data of the MCH file.
	 * 
	 * @param out
	 *            The target stream.
	 * @throws IOException
	 *             if there was a problem with writing the data to the stream.
	 */
	private void writeHeader(final OutputStreamWriter out) throws IOException {
		out.write(String.format("<header length=\"%d\">", HeaderGlobals.BINARY_FILE_HEADER_LENGTH));

		writeTag(out, "magic", new String(HeaderGlobals.BINARY_FILE_HEADER_MAGIC));
		writeTag(out, "startAddressGraph", startAddressGraph);
		writeTag(out, "startAddressGraphBlocks", startAddressGraphBlocks);
		writeTag(out, "startAddressIndex", startAddressIndex);
		writeTag(out, "startAddressRTree", startAddressRTree);

		out.write("</header>");
	}

	/**
	 * Writes the graph tag to the file, which represents the graph part of the MCH file.
	 * 
	 * @param out
	 *            The target file.
	 * @param blockIds
	 *            The identifiers of all blocks, which have to be converted and written to the file. All
	 *            other blocks will be ignored.
	 * @throws IOException
	 *             if there was a problem with writing the data to the stream.
	 */
	private void writeGraph(final OutputStreamWriter out, final int[] blockIds) throws IOException {
		out.write(String.format("<graph length=\"%d\">", startAddressIndex - startAddressGraph));

		writeGraphHeader(out);

		final int numBlocks = blockIndex.size();
		out.write(String.format("<blocks count=\"%d\">", numBlocks));

		for (int blockId = 0; blockId < numBlocks; blockId++) {
			if (blockIds == null || ArrayUtils.indexOf(blockId, blockIds) >= 0) {
				new MchBlockToXmlConverter(blockId).convert(out);
			}
		}

		out.write("</blocks>");
		out.write("</graph>");
	}

	/**
	 * Writes the header tag to the file, which represents the graph's header and its data.
	 * 
	 * @param out
	 *            The target file.
	 * @throws IOException
	 *             if there was a problem with writing the data to the stream.
	 */
	protected void writeGraphHeader(final OutputStreamWriter out) throws IOException {
		out.write(String.format("<header length=\"%d\">", HeaderGlobals.GRAPH_BLOCKS_HEADER_LENGTH));

		writeTag(out, "debug", debug);
		writeTag(out, "bitsPerBlockId", bitsPerBlockId);
		writeTag(out, "bitsPerVertexOffset", bitsPerVertexOffset);
		writeTag(out, "bitsPerEdgeWeight", bitsPerEdgeWeight);
		writeTag(out, "bitsPerStreetType", bitsPerStreetType);

		out.write(String.format("<streetTypes count=\"%d\">", streetTypes.length));
		for (int streetTypeId = 0; streetTypeId < streetTypes.length; streetTypeId++) {
			out.write(String.format("<streetType streetTypeId=\"%d\">%s</streetType>",
					streetTypeId, encodeAsXml(streetTypes[streetTypeId])));
		}
		out.write("</streetTypes>");

		out.write("</header>");
	}

	/**
	 * Writes the tag, which represents the address lookup table of the MCH file.
	 * 
	 * @param out
	 *            The target output stream.
	 * @throws IOException
	 *             if there was a problem with writing the data to the stream.
	 */
	protected void writeAddressLookupTable(final OutputStreamWriter out) throws IOException {
		out.write("<addressLookupTable>");

		// TODO: write its data

		out.write("</addressLookupTable>");
	}

	/**
	 * Writes the tag, which represents the R-tree of the MCH file.
	 * 
	 * @param out
	 *            The target stream.
	 * @throws IOException
	 *             if there was a problem with writing the data to the stream.
	 */
	private void writeRTree(final OutputStreamWriter out) throws IOException {
		out.write("<rTree>");

		// TODO: write its data

		out.write("</rTree>");
	}

	/**
	 * Converter for converting MCH file's graph's blocks into an XML format.
	 * 
	 * @author Patrick Jungermann
	 * @version $Id: MchToXmlConverter.java 1660 2011-12-29 19:19:44Z Patrick.Jungermann@googlemail.com
	 *          $
	 */
	class MchBlockToXmlConverter {

		/**
		 * Need to read 4 bytes too much since the Deserializer requires that.
		 */
		private static final int OVERHEAD_DESERIALIZER = 4;

		/**
		 * The block's identifier.
		 */
		private final int blockId;
		/**
		 * The block's start address.
		 */
		private final long startAddress;
		/**
		 * The block's binary data, extracted from the MCH file.
		 */
		private final byte[] data;
		/**
		 * Stores the current offset, used for reading the data from the block's data.
		 */
		private final Offset offset;
		/**
		 * The number of vertices.
		 */
		private short numVertices;
		/**
		 * The min. latitude.
		 */
		private int minLatitude;
		/**
		 * The min. longitude.
		 */
		private int minLongitude;
		/**
		 * Number of bits used to encode one coordinate value.
		 */
		private byte bitsPerCoordinate;
		/**
		 * Number of bits used to encode one offset to a vertex' edges.
		 */
		private byte bitsPerVertexEdgesOffset;
		/**
		 * Number of bits used to encode one offset to a street name's data.
		 */
		private byte bitsPerStreetNamesOffset;
		/**
		 * Number of bits used to encode one shortcut path's offset.
		 */
		private byte bitsPerShortcutPathOffset;
		/**
		 * The number of bits used to encode one shortcut path's length.
		 */
		private byte bitsPerShortcutPathLength;
		/**
		 * The number of bits used to encode one edge's offset.
		 */
		private byte bitsPerEdgeOffset;
		/**
		 * The offset to all street names.
		 */
		private int streetNamesOffset;
		/**
		 * The start offset of the shortcut data section of the block.
		 */
		private int startShortcuts = Integer.MAX_VALUE;
		/**
		 * The end offset of the shortcut data section of the block.
		 */
		private int endShortcuts = Integer.MIN_VALUE;

		/**
		 * Creates a converter for converting MCH file's graph's blocks into an XML format.
		 * 
		 * @param blockId
		 *            The block's identifier.
		 * @throws IOException
		 *             if there was a problem with reading the block's data from the MCH file.
		 */
		public MchBlockToXmlConverter(final int blockId) throws IOException {
			this.blockId = blockId;

			final Pointer pointer = blockIndex.getPointer(blockId);

			startAddress = startAddressGraphBlocks + pointer.startAddr;
			// need to read to much since the Deserializer requires that.
			final int numBytes = pointer.lengthBytes + OVERHEAD_DESERIALIZER;

			data = new byte[numBytes];
			raf.seek(startAddress);
			raf.readFully(data);

			offset = new Offset();
			offset.set(0, 0);
		}

		/**
		 * Converts the MCH file's block into an XML format.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		public void convert(final OutputStreamWriter out) throws IOException {
			out.write(String.format("<block blockId=\"%d\" length=\"%d\" start=\"%d\">",
					blockId, dataLength(), startAddress));

			writeHeader(out);
			out.flush();
			writeVertices(out);
			out.flush();
			writeEdges(out);
			out.flush();
			writeStreetNames(out);

			out.write("</block>");
		}

		/**
		 * Writes the tag for the block's header to the stream.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeHeader(final OutputStreamWriter out) throws IOException {
			out.write(String.format("<header offset=\"%s\">", offset.toString()));

			// #vertices
			numVertices = Deserializer.readShort(data, offset);
			writeTag(out, "numVertices", numVertices);

			// min. latitude and longitude
			minLatitude = Deserializer.readInt(data, offset);
			minLongitude = Deserializer.readInt(data, offset);
			writeTag(out, "minLatitude", minLatitude);
			writeTag(out, "minLongitude", minLongitude);

			// #bits used for the different kinds of data
			bitsPerCoordinate = Deserializer.readByte(data, offset);
			writeTag(out, "bitsPerCoordinate", bitsPerCoordinate);
			bitsPerVertexEdgesOffset = Deserializer.readByte(data, offset);
			writeTag(out, "bitsPerVertexEdgesOffset", bitsPerVertexEdgesOffset);
			bitsPerStreetNamesOffset = Deserializer.readByte(data, offset);
			writeTag(out, "bitsPerStreetNamesOffset", bitsPerStreetNamesOffset);
			bitsPerShortcutPathOffset = Deserializer.readByte(data, offset);
			writeTag(out, "bitsPerShortcutPathOffset", bitsPerShortcutPathOffset);
			bitsPerShortcutPathLength = Deserializer.readByte(data, offset);
			writeTag(out, "bitsPerShortcutPathLength", bitsPerShortcutPathLength);
			bitsPerEdgeOffset = Deserializer.readByte(data, offset);
			writeTag(out, "bitsPerEdgeOffset", bitsPerEdgeOffset);

			streetNamesOffset = Deserializer.readUInt(data, 24, offset);
			writeTag(out, "streetNamesOffset", streetNamesOffset);

			out.write("</header>");
		}

		/**
		 * Writes the tag for the block's vertices to the stream.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeVertices(final OutputStreamWriter out) throws IOException {
			out.write(String.format("<vertices count=\"%d\" offset=\"%s\">", numVertices,
					encodeAsXml(offset.toString())));

			for (int vertexOffset = 0; vertexOffset < numVertices; vertexOffset++) {
				writeVertex(out, vertexOffset);
			}

			out.write("</vertices>");
		}

		/**
		 * Writes the tag for a block's vertex to the stream.
		 * 
		 * @param out
		 *            The target stream.
		 * @param vertexOffset
		 *            The vertex' offset. (identifier within this block).
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeVertex(final OutputStreamWriter out, final int vertexOffset)
				throws IOException {
			out.write(String.format("<vertex vertexOffset=\"%d\" offset=\"%s\">", vertexOffset,
					encodeAsXml(offset.toString())));

			writeCoordinate(out);

			final int edgesOffset = Deserializer.readUInt(data, bitsPerVertexEdgesOffset, offset);
			writeTag(out, "edgesOffset", edgesOffset);

			if (debug) {
				writeTag(out, "originalId", Deserializer.readInt(data, offset));
			}

			out.write("</vertex>");
		}

		/**
		 * Writes all edges related data to the stream.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeEdges(final OutputStreamWriter out) throws IOException {
			writeEdgesFromOrToHigherVertices(out);
			out.flush();
			writeExternalEdges(out);
			out.flush();
			writeShortcutPaths(out);
		}

		/**
		 * Writes the tag to the stream, which represents all block's edges from/to higher vertices.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeEdgesFromOrToHigherVertices(final OutputStreamWriter out)
				throws IOException {
			out.write(String.format("<edgesFromToHigherVertices offset=\"%s\">",
					encodeAsXml(offset.toString())));

			for (int vertexOffset = 0; vertexOffset < numVertices; vertexOffset++) {
				offset.alignToBytes();
				String offsetCount = offset.toString();
				final int num = readEscapableNum(24);

				out.write(String.format(
						"<edges vertexOffset=\"%d\" offsetCount=\"%s\" count=\"%d\" offset=\"%s\">",
						vertexOffset, offsetCount, num, encodeAsXml(offset.toString())));

				for (int i = 0; i < num; i++) {
					writeEdge(out);
				}

				out.write("</edges>");
			}

			out.write("</edgesFromToHigherVertices>");
		}

		/**
		 * Writes the tag representing all external edges of the MCH file.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeExternalEdges(final OutputStreamWriter out) throws IOException {
			if (startShortcuts != Integer.MAX_VALUE) {
				out.write(String.format("<externalEdges offset=\"%s\">", encodeAsXml(offset.toString())));

				final int startBytes = startShortcuts / 8;
				final int startBits = startShortcuts % 8;

				while (offset.getByteOffset() < startBytes
						|| (offset.getByteOffset() == startBytes && offset.getBitOffset() < startBits)) {
					writeEdge(out);
				}

				out.write("</externalEdges>");

			} else {
				out.write("<externalEdges/>");
			}
		}

		/**
		 * Writes an edge tag to the stream, representing a MCH file's edge.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with the stream.
		 */
		protected void writeEdge(final OutputStreamWriter out) throws IOException {
			out.write(String.format("<edge offset=\"%s\">", encodeAsXml(offset.toString())));

			if (debug) {
				writeTag(out, "originalId", Deserializer.readInt(data, offset));
			}

			writeTag(out, "forward", Deserializer.readBit(data, offset));
			writeTag(out, "backward", Deserializer.readBit(data, offset));

			out.write("<lowestVertex>");
			writeTag(out, "blockId", Deserializer.readUInt(data, bitsPerBlockId, offset));
			writeTag(out, "vertexOffset", Deserializer.readUInt(data, bitsPerVertexOffset, offset));
			out.write("</lowestVertex>");

			out.write("<highestVertex>");
			writeTag(out, "blockId", Deserializer.readUInt(data, bitsPerBlockId, offset));
			writeTag(out, "vertexOffset", Deserializer.readUInt(data, bitsPerVertexOffset, offset));
			out.write("</highestVertex>");

			writeTag(out, "weight", Deserializer.readUInt(data, bitsPerEdgeWeight, offset));

			final boolean shortcut = Deserializer.readBit(data, offset);
			writeTag(out, "shortcut", shortcut);

			if (shortcut) {
				final boolean external = Deserializer.readBit(data, offset);
				writeTag(out, "external", external);

				if (external) {
					out.write("<shortcutPath>");
					final int pathOffset = Deserializer.readUInt(data, bitsPerShortcutPathOffset,
							offset);
					final int pathLength = readEscapableNum(bitsPerShortcutPathLength);
					out.write(String.format("<offset type=\"bitOffset\">%d</offset>", pathOffset));
					writeTag(out, "length", pathLength);
					out.write("</shortcutPath>");

					startShortcuts = Math.min(startShortcuts, pathOffset);
					endShortcuts = Math.max(endShortcuts, pathOffset + pathLength * bitsPerEdgeOffset);

				} else {
					out.write("<bypassedVertex>");
					writeTag(out, "blockId",
							Deserializer.readUInt(data, bitsPerBlockId, offset));
					writeTag(out, "vertexOffset",
							Deserializer.readUInt(data, bitsPerVertexOffset, offset));
					out.write("</bypassedVertex>");
				}

			} else {
				// "normal" edge data
				writeTag(out, "streetTypeId", Deserializer.readUInt(data, bitsPerStreetType, offset));
				writeTag(out, "roundabout", Deserializer.readBit(data, offset));

				// street name and ref
				final boolean hasName = Deserializer.readBit(data, offset);
				writeTag(out, "hasName", hasName);
				final boolean hasRef = Deserializer.readBit(data, offset);
				writeTag(out, "hasRef", hasRef);

				if (hasName) {
					writeTag(out, "streetNameOffset",
							Deserializer.readUInt(data, bitsPerStreetNamesOffset, offset));
				}
				if (hasRef) {
					writeTag(out, "refOffset",
							Deserializer.readUInt(data, bitsPerStreetNamesOffset, offset));
				}

				writeWaypoints(out);
			}

			out.write("</edge>");
		}

		/**
		 * Writes tag, representing the MCH file's shortcut paths to the stream.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeShortcutPaths(final OutputStreamWriter out) throws IOException {
			if (startShortcuts != Integer.MAX_VALUE) {
				final int bytes = startShortcuts / 8;
				final int bits = startShortcuts % 8;
				if (offset.getByteOffset() != bytes || offset.getBitOffset() != bits) {
					out.flush();
					throw new RuntimeException(String.format(
							"%d: %s / %d:%d <-> %d:%d (expected)",
							blockId, offset.toString(), offset.getByteOffset(), offset.getBitOffset(),
							bytes, bits
							));
				}

				final String startOffset = offset.toString();

				final StringBuilder builder = new StringBuilder();
				int num = 0;

				final int endBytes = endShortcuts / 8;
				final int endBits = endShortcuts % 8;

				while (offset.getByteOffset() < endBytes
						|| (offset.getByteOffset() == endBytes && offset.getBitOffset() < endBits)) {
					num++;
					builder.append("<edgeOffset offset=\"").append(encodeAsXml(offset.toString()))
							.append("\" type=\"bitOffset\">")
							.append(Deserializer.readUInt(data, bitsPerEdgeOffset, offset))
							.append("</edgeOffset>");
				}

				out.write(String.format("<shortcutPaths offset=\"%s\" count=\"%d\">",
						encodeAsXml(startOffset), num));
				out.write(builder.toString());
				out.write("</shortcutPaths>");

			} else {
				out.write("<shortcutPaths/>");
			}
		}

		/**
		 * Writes the waypoints tag to the stream, which represents one MCH file's edge's waypoints
		 * data.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeWaypoints(final OutputStreamWriter out) throws IOException {
			final int num = readEscapableNum(16);

			out.write(String.format("<waypoints count=\"%d\" offset=\"%s\">", num,
					encodeAsXml(offset.toString())));

			for (int i = 0; i < num; i++) {
				out.flush();
				writeCoordinate(out);
			}

			out.write("</waypoints>");
		}

		/**
		 * Writes a coordinate tag to the stream, which represents an MCH file's coordinate.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeCoordinate(final OutputStreamWriter out) throws IOException {
			out.write(String.format("<coordinate offset=\"%s\">", encodeAsXml(offset.toString())));

			final int latitudeDiff = Deserializer.readUInt(data, bitsPerCoordinate, offset);
			final int longitudeDiff = Deserializer.readUInt(data, bitsPerCoordinate, offset);

			out.write(String.format("<latitude diffToMin=\"%d\">%d</latitude>",
					latitudeDiff, minLatitude + latitudeDiff));
			out.write(String.format("<longitude diffToMin=\"%d\">%d</longitude>",
					longitudeDiff, minLongitude + longitudeDiff));

			out.write("</coordinate>");
		}

		/**
		 * Reads and returns the number from the current position, which might be escaped, depending on
		 * itself.
		 * 
		 * @param bitsForEscapedVariant
		 *            The number of bits, used for the escaped variant.
		 * @return The number.
		 * @see Block#readEscapableNum(org.mapsforge.routing.android.data.Offset, int)
		 */
		protected int readEscapableNum(final int bitsForEscapedVariant) {
			int num = Deserializer.readUInt(data, 4, offset);
			if (num == 15) { // escaped by 0xFF=15
				num = Deserializer.readUInt(data, bitsForEscapedVariant, offset);
			}

			return num;
		}

		/**
		 * Writes the tag containing all the block's street names.
		 * 
		 * @param out
		 *            The target stream.
		 * @throws IOException
		 *             if there was a problem with writing the data to the stream.
		 */
		protected void writeStreetNames(final OutputStreamWriter out) throws IOException {
			offset.alignToBytes();

			out.write(String.format("<streetNames offset=\"%s\">", encodeAsXml(offset.toString())));

			while (offset.getByteOffset() < dataLength()) {
				final byte[] streetNameData = IOUtils.getZeroTerminatedString(data,
						offset.getByteOffset());
				out.write(String.format(
						"<streetName offset=\"%s\">%s</streetName>",
						encodeAsXml(offset.toString()),
						encodeAsXml(new String(streetNameData, "UTF-8"))
						));
				offset.add((streetNameData.length + 1) * 8);
			}

			out.write("</streetNames>");
		}

		/**
		 * Returns the real length of the data without the overhead needed for the deserializer.
		 * 
		 * @return The real length of the data without the overhead needed for the deserializer.
		 */
		protected int dataLength() {
			return data.length - OVERHEAD_DESERIALIZER;
		}

	}

}
