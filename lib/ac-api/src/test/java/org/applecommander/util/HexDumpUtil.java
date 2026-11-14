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

import java.io.*;

/// This class provides hex dump reading methods to help with test cases.
///
/// The intent is to support copying and pasting from the hexdump utility.
/// Note that when extracting via `acx`, a simple
/// `acx dump -d DISK FILE --binary | hexdump -C` will generate compatible
/// output. Note that hexdump mode should be enabled with the `.hexdump`
/// directive.
public class HexDumpUtil {
	public static final String HEXDUMP_DIRECTIVE = ".hexdump";

	private HexDumpUtil() {}
	
	public static byte[] loadByteSampleFromText(final String content) {
		return loadByteSampleFromInputStream(new ByteArrayInputStream(content.getBytes()));
	}
	
	/// Read in a textual representation of bytes.  This is useful for testing when the source
	/// does not have a nice format; for instance, it is used in nibble image testing where the
	/// byte sequences used as test cases are embedded part way into the file.
	///
	/// Basic processing shared by all modes is to allow a "#" to start a comment line
	/// and to skip any line that is blank.
	///
	/// For basic processing, see concrete Mode implementations.
	public static byte[] loadByteSampleFromInputStream(final InputStream inputStream) {
		try (
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		) {
			Mode mode = null;	// Once mode is assigned, we've committed to a format
			String line = null;
			do {
				line = br.readLine();
				if (line != null) {
					if (line.startsWith("#") || line.trim().isEmpty()) {
						// skip comments and blank lines
						continue;
					}
					if (mode == null) {
						if (HEXDUMP_DIRECTIVE.equals(line)) {
							mode = HexDumpUtil::handleHexDumpMode;
							// skip further processing of the directive line
							continue;
						} else {
							mode = HexDumpUtil::handleRawDataMode;
							// intentionally falling through
						}
					}
					mode.process(line, out);
				}
			} while (line != null);
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/// Parse a given string to a series of bytes. String can use white-space to separate bytes
	/// or denote each byte by two hex digits. This is a lot more loose than
	/// `HexFomrat.of().parseHex(...)`.
	public static byte[] toBytes(String bytes) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int value = 0;
		int digits = 0;
		for (char ch : bytes.toCharArray()) {
			int digit = Character.digit(ch, 16);
			if (digit != -1) {
				value <<= 4;	// x 16
				value += digit;
				digits += 1;
			}
			if (Character.isWhitespace(ch) || digits == 2) {
				if (digits > 0) {
					out.write(value);
				}
				digits = value = 0;
			}
		}
		if (digits > 0) {
			out.write(value);
		}
		return out.toByteArray();
	}
	
	/// A Mode is a helper interface to defer specific formats into a unique class
	/// and support extension.
	@FunctionalInterface
	public interface Mode {
		void process(String line, ByteArrayOutputStream out) throws IOException;
	}

	/// Default processing of the file assumes `#` to initiate a comment followed by hex
	/// bytes as such:
	///
	/// ```
	/// # This is a comment
	/// 01 02 03 04
	/// ```
    public static void handleRawDataMode(String line, ByteArrayOutputStream out) throws IOException {
        out.write(toBytes(line));
    }

	/// The ".hexdump" directive initiates support of the BSD `hexdump` format.
	/// Sample:
	///
	/// ```
	/// # This is a comment
	/// .hexdump
	/// 00000080 48 45 4c 4c 4f             | Hello |
	/// *
	/// 00001000 01 02 03                   | ... |
	/// ```
	///
	/// This would create a memory image that is 0x1002 bytes long; anything not explicitly
	/// in the file is defaulted to 0x00.
	public static void handleHexDumpMode(String line, ByteArrayOutputStream out) throws IOException {
		if ("*".equals(line)) return;		// We can skip the gap marker

		// Where do we think we are?
		int offset = Integer.parseInt(line.substring(0, 8), 16);
		if (offset < out.size()) {
			throw new IOException("Offset indicator does not match current size of byte array; check input file");
		}

		// Fill with 0x00 until we get to our expected position
		while (out.size() < offset) {
			out.write(0x00);
		}

		// Extract hex bytes
		int start = 9;
		if (line.length() < start) {
			// Last line is typically just the last address.
			return;
		}
		int finish = line.indexOf('|');
		if (finish == -1) {
			line = line.substring(start);
		} else {
			line = line.substring(start, finish);
		}
		out.write(toBytes(line));
	}
}
