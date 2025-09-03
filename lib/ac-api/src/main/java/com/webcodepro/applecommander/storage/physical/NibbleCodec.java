/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2025 by Robert Greene
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
package com.webcodepro.applecommander.storage.physical;

import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

public class NibbleCodec {
    private static final TextBundle textBundle = StorageBundle.getInstance();

    /**
     * This is the 6 and 3 write translate table, as given in Beneath
     * Apple DOS and Beneath Apple ProDOS 2020, pg 25.
     */
    private static final int[] writeTranslateTable53 = {
            //$0    $1    $2    $3    $4    $5    $6    $7
            0xab, 0xad, 0xae, 0xaf, 0xb5, 0xb6, 0xb7, 0xba, // +$00
            0xbb, 0xbd, 0xbe, 0xbf, 0xd6, 0xd7, 0xda, 0xdb, // +$08
            0xdd, 0xde, 0xdf, 0xea, 0xeb, 0xed, 0xee, 0xef, // +$10
            0xf5, 0xf6, 0xf7, 0xfa, 0xfb, 0xfd, 0xfe, 0xff  // +$18
    };
    /**
     * The 5&3 read translation table.  Constructed from the 5&3
     * write translate table.  Used to decode a disk byte into a
     * value from 0x00 to 0x1f which is further decoded...
     */
    private static final int[] readTranslateTable53;
    static {
        // Construct the read translation table:
        readTranslateTable53 = new int[256];
        for (int i = 0; i< writeTranslateTable53.length; i++) {
            readTranslateTable53[writeTranslateTable53[i]] = i;
        }
    }

    /**
     * This is the 6 and 2 write translate table, as given in Beneath
     * Apple DOS, pg 3-21.
     */
    private static final int[] writeTranslateTable62 = {
            //$0    $1    $2    $3    $4    $5    $6    $7
            0x96, 0x97, 0x9a, 0x9b, 0x9d, 0x9e, 0x9f, 0xa6,	// +$00
            0xa7, 0xab, 0xac, 0xad, 0xae, 0xaf, 0xb2, 0xb3, // +$08
            0xb4, 0xb5, 0xb6, 0xb7, 0xb9, 0xba, 0xbb, 0xbc, // +$10
            0xbd, 0xbe, 0xbf, 0xcb, 0xcd, 0xce, 0xcf, 0xd3, // +$18
            0xd6, 0xd7, 0xd9, 0xda, 0xdb, 0xdc, 0xdd, 0xde, // +$20
            0xdf, 0xe5,	0xe6, 0xe7, 0xe9, 0xea, 0xeb, 0xec, // +$28
            0xed, 0xee, 0xef, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, // +$30
            0xf7, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff  // +$38
    };
    /**
     * The 6&2 read translation table.  Constructed from the 6&2
     * write translate table.  Used to decode a disk byte into a
     * value from 0x00 to 0x3f which is further decoded...
     */
    private static final int[] readTranslateTable62;
    static {
        // Construct the read translation table:
        readTranslateTable62 = new int[256];
        for (int i = 0; i< writeTranslateTable62.length; i++) {
            readTranslateTable62[writeTranslateTable62[i]] = i;
        }
    }

    public static final int[] DOS32_ADDRESS_PROLOGUE = { 0xd5, 0xaa, 0xb5 };
    public static final int[] DOS32_DATA_PROLOGUE = { 0xd5, 0xaa, 0xad };
    public static final int[] DOS33_ADDRESS_PROLOGUE = { 0xd5, 0xaa, 0x96 };
    public static final int[] DOS33_DATA_PROLOGUE = { 0xd5, 0xaa, 0xad };

    /**
     * Decode odd-even bytes as stored on disk.  The format will be
     * in two bytes.  They are stored as such:<pre>
     *     XX = 1d1d1d1d (odd data bits)
     *     YY = 1d1d1d1d (even data bits)
     * </pre>
     * XX is then shifted by a bit and ANDed with YY to get the data byte.
     * See page 3-12 in Beneath Apple DOS for more information.
     */
    public static int decodeOddEven(byte[] buffer, int offset) {
        int b1 = AppleUtil.getUnsignedByte(buffer[offset]);
        int b2 = AppleUtil.getUnsignedByte(buffer[offset+1]);
        return (b1 << 1 | 0x01) & b2;
    }

    /**
     * Encode odd-even bytes to be stored on disk.  See decodeOddEven
     * for the format.
     * @see #decodeOddEven
     */
    public static void encodeOddEven(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) ((value >> 1) | 0xaa);
        buffer[offset+1] = (byte) (value | 0xaa);
    }

    /**
     * Identify if this is a 13-sector or 16-sector disk. Note that it is "guaranteed"
     * to return 13, 16, or an exception.
     */
    public static int identifySectorsPerTrack(byte[] trackData) {
        int sectorsPerTrack = 0;
        try {
            byte[] data = readSectorFromTrack62(trackData, 0, 0, 16);
            if (data != null) sectorsPerTrack = 16;
        } catch (IllegalArgumentException ex1) {
            // ignored
        }
        if (sectorsPerTrack == 0) {
            try {
                byte[] data = readSectorFromTrack53(trackData, 0, 0, 13);
                if (data != null) sectorsPerTrack = 13;
            } catch (IllegalArgumentException ex2) {
                // ignored
            }
        }
        if (sectorsPerTrack == 0) {
            throw new RuntimeException("unable to locate T0,S0 on disk");
        }
        return sectorsPerTrack;
    }

    /**
     * Locate physical sector in the given track data. Decodes 6+2 encoding.
     */
    public static byte[] readSectorFromTrack62(byte[] trackData, int track, int physicalSector, int sectorsPerTrack) {
        // 2. locate address field for this track and sector
        int offset = 0;
        byte[] addressField = new byte[14];
        boolean found = false;
        int attempts = sectorsPerTrack;
        while (!found && attempts >= 0) {
            int nextOffset = locateField(DOS33_ADDRESS_PROLOGUE, trackData, addressField, offset);
            attempts--;
            offset = nextOffset;
            int t = decodeOddEven(addressField, 5);
            int s = decodeOddEven(addressField, 7);
            found = (t == track && s == physicalSector);
        }
        if (!found) {
            throw new IllegalArgumentException(textBundle
                    .format("NibbleOrder.InvalidPhysicalSectorError", physicalSector, track, 1)); //$NON-NLS-1$
        }
        // 3. read data field that immediately follows the address field
        byte[] dataField = new byte[349];
        locateField(DOS33_DATA_PROLOGUE, trackData, dataField, offset);
        // 4. translate data field
        byte[] buffer = new byte[342];
        int checksum = 0;
        for (int i=0; i<buffer.length; i++) {
            int b = AppleUtil.getUnsignedByte(dataField[i+3]);
            checksum ^= readTranslateTable62[b];		// XOR
            if (i < 86) {
                buffer[buffer.length - i - 1] = (byte) checksum;
            } else {
                buffer[i - 86] = (byte) checksum;
            }
        }
        checksum ^= readTranslateTable62[AppleUtil.getUnsignedByte(dataField[345])];
        if (checksum != 0) return null;	// BAD DATA
        // 5. decode data field
        byte[] sectorData = new byte[256];
        for (int i=0; i<sectorData.length; i++) {
            int b1 = AppleUtil.getUnsignedByte(buffer[i]);
            int lowerBits = buffer.length - (i % 86) - 1;
            int b2 = AppleUtil.getUnsignedByte(buffer[lowerBits]);
            int shiftPairs = (i / 86) * 2;
            // shift b1 up by 2 bytes (contains bits 7-2)
            // align 2 bits in b2 appropriately, mask off anything but
            // bits 0 and 1 and then REVERSE THEM...
            int[] reverseValues = { 0x0, 0x2, 0x1, 0x3 };
            int b = (b1 << 2) | reverseValues[(b2 >> shiftPairs) & 0x03];
            sectorData[i] = (byte) b;
        }
        return sectorData;
    }

    /**
     * Locate physical sector in the given track data. Decodes 5+3 encoding.
     */
    public static byte[] readSectorFromTrack53(byte[] trackData, int track, int physicalSector, int sectorsPerTrack) {
        // 2. locate address field for this track and sector
        int offset = 0;
        byte[] addressField = new byte[14];
        boolean found = false;
        int attempts = sectorsPerTrack;
        while (!found && attempts >= 0) {
            int nextOffset = locateField(DOS32_ADDRESS_PROLOGUE, trackData, addressField, offset);
            attempts--;
            offset = nextOffset;
            int t = decodeOddEven(addressField, 5);
            int s = decodeOddEven(addressField, 7);
            found = (t == track && s == physicalSector);
        }
        if (!found) {
            throw new IllegalArgumentException(textBundle
                    .format("NibbleOrder.InvalidPhysicalSectorError", physicalSector, track, 1)); //$NON-NLS-1$
        }
        // 3. read data field that immediately follows the address field (D5 AA AD ...410 data bytes... CKSUM DE AA EB
        byte[] dataField = new byte[417];
        locateField(DOS32_DATA_PROLOGUE, trackData, dataField, offset);
        // 4. translate data field
        byte[] buffer = new byte[410];
        int checksum = 0;
        for (int i=0; i<buffer.length; i++) {
            int b = AppleUtil.getUnsignedByte(dataField[i+3]);
            checksum ^= readTranslateTable53[b];		// XOR
            if (i < 154) {
                buffer[buffer.length - i - 1] = (byte) checksum;
            } else {
                buffer[i - 154] = (byte) checksum;
            }
        }
        checksum ^= readTranslateTable53[AppleUtil.getUnsignedByte(dataField[413])];
        if (checksum != 0) return null;	// BAD DATA
        // 5. decode data field
        // Note: This reads from these ranges and leaves two bytes that we use at end:
        // 0x00-0x33, 0x33-0x65, 0x66-0x98, 0x99-0xcb, 0xcc-0xfe, 0xff - unused
        // 0x100-0x132, 0x133-0x165, 0x166-0x198, 0x199 - unused
        byte[] sectorData = new byte[256];
        offset = 0;
        for (int i=0x33-1; i>= 0; i--) {
            int a76543 = Byte.toUnsignedInt(buffer[i]);
            int b76543 = Byte.toUnsignedInt(buffer[0x33 + i]);
            int c76543 = Byte.toUnsignedInt(buffer[0x66 + i]);
            int d76543 = Byte.toUnsignedInt(buffer[0x99 + i]);
            int e76543 = Byte.toUnsignedInt(buffer[0xcc + i]);
            int a210d2e2 = Byte.toUnsignedInt(buffer[0x100 + i]);
            int b210d1e1 = Byte.toUnsignedInt(buffer[0x133 + i]);
            int c210d0e0 = Byte.toUnsignedInt(buffer[0x166 + i]);

            sectorData[offset++] = (byte) (a76543 << 3 | (a210d2e2 >> 2) & 0x7);
            sectorData[offset++] = (byte) (b76543 << 3 | (b210d1e1 >> 2) & 0x7);
            sectorData[offset++] = (byte) (c76543 << 3 | (c210d0e0 >> 2) & 0x7);
            sectorData[offset++] = (byte) (d76543 << 3 | (a210d2e2 << 1) & 0x4 | b210d1e1 & 0x2 | (c210d0e0 >> 1) & 0x1);
            sectorData[offset++] = (byte) (e76543 << 3 | (a210d2e2 << 2) & 0x4 | (b210d1e1 << 1) & 0x2 | c210d0e0 & 0x1);
        }
        sectorData[255] = (byte)(buffer[0xff] << 3 | buffer[0x199] & 0x07);
        return sectorData;
    }


    /**
     * Locate a field on the track.  These are identified by a 3 byte unique
     * signature.  Because of the way in which disk bytes are captured, we need
     * to wrap around the track to ensure all sequences of bytes are accounted for.
     * <p>
     * This method fills fieldData as well as returning the last position referenced
     * in the track buffer.
     */
    public static int locateField(int[] prologue, byte[] trackData, byte[] fieldData, int startingOffset) {
        int i = startingOffset;	// logical position in track buffer (can wrap)
        int position = 0;			// physical position in field buffer
        while (i < trackData.length + fieldData.length) {
            int offset = i % trackData.length;	// physical position in track buffer
            int b = AppleUtil.getUnsignedByte(trackData[offset]);
            if (position == 0 && b == prologue[0]) {
                fieldData[position++] = (byte) b;
            } else if (position == 1 && b == prologue[1]) {
                fieldData[position++] = (byte) b;
            } else if (position == 2 && b == prologue[2]) {
                fieldData[position++] = (byte) b;
            } else if (position >= 3 && position <= fieldData.length) {
                if (position < fieldData.length) fieldData[position++] = (byte) b;
                if (position == fieldData.length) break;	// done!
            } else {
                position = 0;
            }
            i++;
        }
        return i % trackData.length;
    }

    public static void writeSectorToTrack(byte[] trackData, byte[] sectorData, int track, int physicalSector, int sectorsPerTrack) {
        // 2. locate address field for this track and sector
        int offset = 0;
        byte[] addressField = new byte[14];
        boolean found = false;
        while (!found && offset < trackData.length) {
            int nextOffset = locateField(DOS33_ADDRESS_PROLOGUE, trackData, addressField, offset);
            if (nextOffset < offset) {	// we wrapped!
                throw new IllegalArgumentException(textBundle
                        .format("NibbleOrder.InvalidPhysicalSectorError", physicalSector, track, 2)); //$NON-NLS-1$
            }
            offset = nextOffset;
            int t = decodeOddEven(addressField, 5);
            int s = decodeOddEven(addressField, 7);
            found = (t == track && s == physicalSector);
        }
        if (!found) {
            throw new IllegalArgumentException(textBundle
                    .format("NibbleOrder.InvalidPhysicalSectorError", physicalSector, track, 2)); //$NON-NLS-1$
        }

        // 3. PRENIBBLE: This is Java translated from assembly @ $B800
        //               The Java routine was not working...  :o(
        int[] bb00 = new int[0x100];
        int[] bc00 = new int[0x56];
        int x = 0;
        int y = 2;
        while (true) {
            y--;
            if (y < 0) {
                y+= 256;
            }
            int a = AppleUtil.getUnsignedByte(sectorData[y]);
            bc00[x]<<= 1;
            bc00[x]|= a & 1;
            a>>= 1;
            bc00[x]<<= 1;
            bc00[x]|= a & 1;
            a>>= 1;
            bb00[y] = a;
            x++;
            if (x >= 0x56) {
                x = 0;
                if (y == 0) break;	// done
            }
        }
        for (x=0; x<0x56; x++) {
            bc00[x]&= 0x3f;
        }

        // 4. Translated from portions of WRITE at $B82A:
        byte[] diskData = new byte[343];
        int pos = 0;
        for (y=0x56; y>0; y--) {
            if (y == 0x56) {
                diskData[pos++] = (byte) writeTranslateTable62[bc00[y-1]];
            } else {
                diskData[pos++] = (byte) writeTranslateTable62[bc00[y] ^ bc00[y-1]];
            }
        }
        diskData[pos++] = (byte) writeTranslateTable62[bc00[0] ^ bb00[y]];
        for (y=1; y<256; y++) {
            diskData[pos++] = (byte) writeTranslateTable62[bb00[y] ^ bb00[y-1]];
        }
        diskData[pos++] = (byte) writeTranslateTable62[bb00[255]];

        // 5. write to disk (data may wrap - hence the manual copy)
        byte[] dataFieldPrologue = new byte[3];
        offset= locateField(DOS33_DATA_PROLOGUE, trackData, dataFieldPrologue, offset);
        for (int i=0; i<diskData.length; i++) {
            pos = (offset + i) % trackData.length;
            trackData[pos] = diskData[i];
        }
    }
}
