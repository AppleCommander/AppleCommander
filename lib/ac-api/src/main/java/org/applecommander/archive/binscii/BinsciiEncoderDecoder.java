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
package org.applecommander.archive.binscii;

import org.applecommander.util.DataBuffer;

import java.io.IOException;

/// The BinSCII encode/decoder handles an individual line of data.
public class BinsciiEncoderDecoder {
	public static final String BINSCII_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789()";
	
	private final String alphabet;
	
	public BinsciiEncoderDecoder(String alphabet) throws IOException {
		if (alphabet == null || alphabet.length() != 64) {
			throw new IOException("Invalid alphabet -- must be 64 characters long");
		}
		this.alphabet = alphabet;
	}

	/// Decode a line of text into the associated data. Note that input is a multiple of 4
	/// while the output is a multiple of 3.
	public DataBuffer decode(String encodedText) throws IOException {
		if (encodedText == null || encodedText.length() % 4 != 0) {
			throw new IOException("Invalid encoded text -- must be multiple of 4");
		}
		DataBuffer dataBuffer = DataBuffer.create((encodedText.length() / 4) * 3);
		for (int i=0; i<encodedText.length(); i+= 4) {
			long value = 0;
			for (int j=3; j>=0; j--) {
				char ch = encodedText.charAt(i+j);
				int pos = alphabet.indexOf(ch);
				if (j > 63) {
					throw new IOException(String.format("Invalid character found for current alphabet -- '%c'", ch));
				}
				value <<= 6;
				value |= pos;
			}
			dataBuffer.writeByte((int)value >> 16);
			dataBuffer.writeByte((int)value >> 8);
			dataBuffer.writeByte((int)value);
		}
		return dataBuffer;
	}

	/// Encode data into the textual representation. Input is forced to be a muliple of 3
	/// while the output is a multiple of 4.
	public String encode(DataBuffer data) throws IOException {
		if (data.limit() % 3 != 0) {
			data = DataBuffer.resize(data, data.limit() + 3 - (data.limit() % 3));
		}
		StringBuilder text = new StringBuilder();
		while (data.hasRemaining()) {
			long value = ((long) data.readUnsignedByte() << 16)
					| ((long) data.readUnsignedByte() << 8)
					| data.readUnsignedByte();
			text.append(alphabet.charAt((int)value & 0x3f));
			text.append(alphabet.charAt((int)(value >> 6) & 0x3f));
			text.append(alphabet.charAt((int)(value >> 12) & 0x3f));
			text.append(alphabet.charAt((int)(value >> 18) & 0x3f));
		}
		return text.toString();
	}
}
