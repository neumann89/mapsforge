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
package org.mapsforge.routing.preprocessing.data;

import org.junit.Test;

import java.util.Random;
import java.util.Stack;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link Serializer}.
 *
 * @author Frank Viernau
 */
public class SerializerUnitTests {

	private final static int N = 10000;

	/**
	 * verify using random writes and random reads.
	 */
	@Test
	public void testByteRW() {
		long seed = System.currentTimeMillis();
		byte[] buff = new byte[N];
		int bitOffset = 1;
		int[] byteOffsets = getRandomOffsets(N - 1, 1);

		Random rnd = new Random(seed);
        for (int byteOffset : byteOffsets) {
            Serializer.writeByte((byte) rnd.nextInt(), buff, byteOffset, bitOffset);
        }

		rnd = new Random(seed);
        for (int byteOffset : byteOffsets) {
            byte val = (byte) rnd.nextInt();
            byte val_ = Serializer.readByte(buff, byteOffset, bitOffset);
            assertEquals(val, val_);
        }
	}

	/**
	 * verify using random writes and random reads.
	 */
	@Test
	public void testShortRW() {
		long seed = System.currentTimeMillis();
		int alignment = 2;
		byte[] buff = new byte[N * alignment];
		int[] byteOffsets = getRandomOffsets(N - 1, alignment);

		for (int bitOffset = 0; bitOffset < 8; bitOffset++) {
			Random rnd = new Random(seed);
            for (int byteOffset : byteOffsets) {
                Serializer
                        .writeShort((short) rnd.nextInt(), buff, byteOffset, bitOffset);
            }

			rnd = new Random(seed);
            for (int byteOffset : byteOffsets) {
                short val = (short) rnd.nextInt();
                short val_ = Serializer.readShort(buff, byteOffset, bitOffset);
                assertEquals(val, val_);
            }
		}
	}

	/**
	 * verify using random writes and random reads.
	 */
	@Test
	public void testIntRW() {
		long seed = System.currentTimeMillis();
		int alignment = 4;
		byte[] buff = new byte[N * alignment];
		int[] byteOffsets = getRandomOffsets(N - 1, alignment);

		for (int bitOffset = 0; bitOffset < 8; bitOffset++) {
			Random rnd = new Random(seed);
            for (int byteOffset : byteOffsets) {
                Serializer.writeInt(rnd.nextInt(), buff, byteOffset, bitOffset);
            }

			rnd = new Random(seed);
            for (int byteOffset : byteOffsets) {
                int val = rnd.nextInt();
                int val_ = Serializer.readInt(buff, byteOffset, bitOffset);
                assertEquals(val, val_);
            }
		}
	}

	/**
	 * verify using random writes and random reads.
	 */
	@Test
	public void testLongRW() {
		long seed = System.currentTimeMillis();
		int alignment = 8;
		byte[] buff = new byte[N * alignment];
		int[] byteOffsets = getRandomOffsets(N - 1, alignment);

		for (int bitOffset = 0; bitOffset < 8; bitOffset++) {
			Random rnd = new Random(seed);
            for (int byteOffset : byteOffsets) {
                Serializer.writeLong(rnd.nextLong(), buff, byteOffset, bitOffset);
            }

			rnd = new Random(seed);
            for (int byteOffset : byteOffsets) {
                long val = rnd.nextLong();
                long val_ = Serializer.readLong(buff, byteOffset, bitOffset);
                assertEquals(val, val_);
            }
		}
	}

	/**
	 * verify using random writes and random reads.
	 */
	@Test
	public void testUIntRW() {
		int[] values = getRandomIntValues(N);
		byte[] buff = new byte[N * 64];
		int[] byteOffset = new int[N];
		int[] bitOffset = new int[N];
		byteOffset[0] = 0;
		bitOffset[0] = 0;
		for (int i = 1; i < values.length; i++) {
			int nBits = (int) Math.floor(Math.log(values[i - 1]) / Math.log(2)) + 1;
			byteOffset[i] = byteOffset[i - 1] + ((bitOffset[i - 1] + nBits) / 8);
			bitOffset[i] = (bitOffset[i - 1] + nBits) % 8;
		}

		int[] order = getRandomOffsets(N, 1);
		for (int i = 1; i < order.length; i++) {
			int nBits = (int) Math.floor(Math.log(values[order[i]]) / Math.log(2)) + 1;
			Serializer.writeUInt(values[order[i]], nBits, buff, byteOffset[order[i]],
					bitOffset[order[i]]);
			int val = (int) Serializer.readUInt(buff, nBits, byteOffset[order[i]],
					bitOffset[order[i]]);
			assertEquals(values[order[i]], val);
		}
		for (int i = 1; i < order.length; i++) {
			int nBits = (int) Math.floor(Math.log(values[order[i]]) / Math.log(2)) + 1;
			int val = (int) Serializer.readUInt(buff, nBits, byteOffset[order[i]],
					bitOffset[order[i]]);
			assertEquals(values[order[i]], val);
		}
	}

	/**
	 * verify using random writes and random reads.
	 */
	@Test
	public void testBitRW() {
		byte[] buff = new byte[(N / 8) + 1];
		boolean[] bits = getRandomBooleanValues(N);
		int byteOffset = 0;
		int bitOffset = 0;
        for (boolean bit : bits) {
            Serializer.writeBit(bit, buff, byteOffset, bitOffset);
            bitOffset++;
            if (bitOffset == 8) {
                byteOffset++;
                bitOffset = 0;
            }
        }
		byteOffset = 0;
		bitOffset = 0;
        for (boolean bit : bits) {
            boolean b = Serializer.readBit(buff, byteOffset, bitOffset);
            assertEquals(bit, b);
            bitOffset++;
            if (bitOffset == 8) {
                byteOffset++;
                bitOffset = 0;
            }
        }
	}

	/**
	 * Generate random boolean array.
	 * 
	 * @param n
	 *            number of booleans to generate.
	 * @return the random array.
	 */
	private boolean[] getRandomBooleanValues(int n) {
		boolean[] b = new boolean[n];
		Random rnd = new Random();
		for (int i = 0; i < b.length; i++) {
			b[i] = rnd.nextBoolean();
		}
		return b;
	}

	/**
	 * Generate random offsets in the range of [0 .. (n-1) * alignment]
	 * 
	 * @param n
	 *            number of values to generate
	 * @param alignment
	 *            alignment of reads an writes.
	 * @return the random values.
	 */
	private int[] getRandomOffsets(int n, int alignment) {
		Random rnd = new Random();
		Stack<Integer> stack = new Stack<Integer>();
		for (int i = 0; i < n; i++) {
			stack.push(i * alignment);
		}
		int j = 0;
		int[] randomOffsets = new int[n];
		while (!stack.isEmpty()) {
			int x = rnd.nextInt(stack.size());
			randomOffsets[j++] = stack.get(x);
			stack.remove(x);
		}
		return randomOffsets;
	}

	/**
	 * Generate randomized int array.
	 * 
	 * @param n
	 *            number of ints to generate.
	 * @return randomized array.
	 */
	private int[] getRandomIntValues(int n) {
		Random rnd = new Random();
		int[] values = new int[n];
		double j = 1;
		for (int i = 0; i < n; i++) {
			values[i] = rnd.nextInt((int) Math.pow(2, j++));
			if (j == 30) {
				j = 3;
			}
			if (values[i] <= 2) {
				i--;
			}
		}
		return values;
	}

}
