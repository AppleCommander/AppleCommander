/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.applecommander.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HexDumpUtilTest {
	@Test
	public void testToBytes() {
		assertArrayEquals(new byte[] { 0xf, 0x0, 0x0, 0xd }, HexDumpUtil.toBytes("f 0 0 d"));
		assertArrayEquals(new byte[] { 0xf, 0x0, 0x0, 0xd }, HexDumpUtil.toBytes("\tF 0\t0\nD  "));
		assertArrayEquals(new byte[] { 0xf, 0x0, 0x0, 0xd }, HexDumpUtil.toBytes("   0F00000D\n"));
		assertArrayEquals(new byte[] { 0xf, 0x0, 0x0, 0xd }, HexDumpUtil.toBytes("   0F00 000D\n"));
		assertArrayEquals(new byte[] { (byte)0xff }, HexDumpUtil.toBytes(" ff "));
	}

	@Test
	public void testLoadByteSampleFromText() throws IOException {
		final String hex = """
				# comment
				01020304
				
				05 06 07 08
				 090a b c d\te ff
				""";
		byte[] expected = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7,
				0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, (byte)0xff };
		byte[] actual = HexDumpUtil.loadByteSampleFromText(hex);
		assertArrayEquals(expected,  actual);
	}
	
	@Test
	public void testHexDumpMode1() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String line = "00000010 ff fe fd fc 01 02 03 04 |........|";
		byte[] expected = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				(byte)0xff, (byte)0xfe, (byte)0xfd, (byte)0xfc, 0x01, 0x02, 0x03, 0x04 };
		
		HexDumpUtil.handleHexDumpMode(line, out);
		byte[] actual = out.toByteArray();
		assertArrayEquals(expected, actual);
	}
	
	@Test
	public void testLoadByteSampleFromHexdump() throws IOException {
        final String hex = """
                # This is a comment
                .hexdump
                00000080 48 45 4c 4c 4f             | Hello |
                *
                00001000 01 02 03                   | ... |
                """;
		final byte[] expected_0080 = { 0x48, 0x45, 0x4c, 0x4c, 0x4f };
		final byte[] expected_1000 = { 0x01, 0x02, 0x03 };
		final int expectedLength = 0x1003;
		final int expectedZeros = expectedLength - expected_0080.length - expected_1000.length;
		
		byte[] actual = HexDumpUtil.loadByteSampleFromText(hex);
		int actualZeros = 0;
		for (byte b : actual) {
			if (b == 0x00) actualZeros++;
		}
		
		assertEquals(expectedLength, actual.length);
		assertEquals(expectedZeros, actualZeros);
		assertArrayEquals(expected_0080, Arrays.copyOfRange(actual, 0x80, 0x80 + expected_0080.length));
		assertArrayEquals(expected_1000, Arrays.copyOfRange(actual, 0x1000, 0x1000 + expected_1000.length));
	}
}
