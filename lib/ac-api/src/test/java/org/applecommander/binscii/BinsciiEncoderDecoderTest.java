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
package org.applecommander.binscii;

import org.applecommander.archive.binscii.BinsciiEncoderDecoder;
import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinsciiEncoderDecoderTest {
	private final DataBuffer sampleInput = DataBuffer.wrap(0b00110010, 0b00000000, 0b00000000);
	private final String sampleOutput = "AAgM";

	@Test
	public void testEncode() throws IOException {
		BinsciiEncoderDecoder binscii = new BinsciiEncoderDecoder(BinsciiEncoderDecoder.BINSCII_ALPHABET);
		String result = binscii.encode(sampleInput);
		assertEquals(sampleOutput, result);
	}
	
	@Test
	public void testDecode() throws IOException {
		BinsciiEncoderDecoder binscii = new BinsciiEncoderDecoder(BinsciiEncoderDecoder.BINSCII_ALPHABET);
		DataBuffer original = binscii.decode(sampleOutput);
		assertArrayEquals(sampleInput.asBytes(), original.asBytes());
	}
}
