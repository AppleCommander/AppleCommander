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
package org.applecommander.converter;

import org.applecommander.util.DataBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASCIIConverterTest {
    @Test
    public void testHighASCII() {
        ASCIIConverter converter = new ASCIIConverter(true);
        final String text = "HELLO WORLD\n";
        final DataBuffer expected = DataBuffer.wrap(0xc8, 0xc5, 0xcc, 0xcc, 0xcf, 0xa0, 0xd7, 0xcf, 0xd2, 0xcc, 0xc4, 0x8d);
        DataBuffer actual = converter.toData(text);
        assertEquals(expected, actual);
    }

    @Test
    public void testLowASCII() {
        ASCIIConverter converter = new ASCIIConverter(false);
        final String text = "HELLO WORLD\n";
        final DataBuffer expected = DataBuffer.wrap('H', 'E', 'L', 'L', 'O', ' ', 'W', 'O', 'R', 'L', 'D', 0x0d);
        DataBuffer actual = converter.toData(text);
        assertEquals(expected, actual);
    }
}
