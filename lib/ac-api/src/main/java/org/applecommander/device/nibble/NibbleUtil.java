/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package org.applecommander.device.nibble;

import org.applecommander.util.DataBuffer;

public class NibbleUtil {
    /**
     * Decode odd-even bytes as stored on disk.  The format will be
     * in two bytes.  They are stored as such:<pre>
     *     XX = 1d1d1d1d (odd data bits)
     *     YY = 1d1d1d1d (even data bits)
     * </pre>
     * XX is then shifted by a bit and ANDed with YY to get the data byte.
     * See page 3-12 in Beneath Apple DOS for more information.
     */
    public static int decodeOddEven(DataBuffer rawData, int offset) {
        int b1 = rawData.getUnsignedByte(offset);
        int b2 = rawData.getUnsignedByte(offset+1);
        return (b1 << 1 | 0x01) & b2;
    }

    /**
     * Encode odd-even bytes to be stored on disk.  See decodeOddEven
     * for the format.
     * @see #decodeOddEven(DataBuffer, int)
     */
    public static void encodeOddEven(DataBuffer data, int offset, int value) {
        int b1 = (value >> 1) | 0xaa;
        int b2 = value | 0xaa;
        data.putBytes(offset, b1, b2);
    }
}
