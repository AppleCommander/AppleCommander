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
package org.applecommander.codec;

import org.applecommander.capability.Capability;
import org.applecommander.util.DataBuffer;

public class Nibble62Disk525Codec implements NibbleDiskCodec {
    private static final int RAW_BUFFER_SIZE = 342;
    private static final int SECTOR_SIZE = 256;
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

    @Override
    public boolean can(Capability capability) {
        return capability == Capability.ENCODE;
    }

    @Override
    public int encodedSize() {
        return RAW_BUFFER_SIZE;
    }

    @Override
    public int decodedSize() {
        return SECTOR_SIZE;
    }

    @Override
    public DataBuffer decode(DataBuffer rawData) {
        assert(rawData.limit() == RAW_BUFFER_SIZE+1);   // includes checksum
        // Translate data field and check the checksum
        DataBuffer buffer = DataBuffer.create(RAW_BUFFER_SIZE);
        int checksum = 0;
        for (int i=0; i<RAW_BUFFER_SIZE; i++) {
            int b = rawData.getUnsignedByte(i);
            checksum ^= readTranslateTable62[b];		// XOR
            if (i < 86) {
                buffer.putByte(RAW_BUFFER_SIZE - i - 1, checksum);
            } else {
                buffer.putByte(i - 86, checksum);
            }
        }
        checksum ^= readTranslateTable62[rawData.getUnsignedByte(RAW_BUFFER_SIZE)];
        if (checksum != 0) return null;	// BAD DATA -- FIXME when logging is enabled
        // Decode data field
        DataBuffer sectorData = DataBuffer.create(SECTOR_SIZE);
        final int[] reverseValues = { 0x0, 0x2, 0x1, 0x3 };
        for (int i=0; i<sectorData.limit(); i++) {
            int b1 = buffer.getUnsignedByte(i);
            int lowerBits = RAW_BUFFER_SIZE - (i % 86) - 1;
            int b2 = buffer.getUnsignedByte(lowerBits);
            int shiftPairs = (i / 86) * 2;
            // shift b1 up by 2 bytes (contains bits 7-2)
            // align 2 bits in b2 appropriately, mask off anything but
            // bits 0 and 1 and then REVERSE THEM...
            sectorData.writeByte((b1 << 2) | reverseValues[(b2 >> shiftPairs) & 0x03]);
        }
        assert(sectorData.position() == SECTOR_SIZE);
        return sectorData;
    }

    @Override
    public DataBuffer encode(DataBuffer data) {
        assert(data.limit() == SECTOR_SIZE);
        // PRENIBBLE: This is Java translated from assembly @ $B800
        //            The Java routine was not working...  :o(
        int[] bb00 = new int[0x100];
        int[] bc00 = new int[0x56];
        int x = 0;
        int y = 2;
        while (true) {
            y--;
            if (y < 0) {
                y+= 256;
            }
            int a = data.getUnsignedByte(y);
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
        DataBuffer diskData = DataBuffer.create(RAW_BUFFER_SIZE+1);
        for (y=0x56; y>0; y--) {
            if (y == 0x56) {
                diskData.writeByte(writeTranslateTable62[bc00[y-1]]);
            } else {
                diskData.writeByte(writeTranslateTable62[bc00[y] ^ bc00[y-1]]);
            }
        }
        diskData.writeByte(writeTranslateTable62[bc00[0] ^ bb00[y]]);
        for (y=1; y<256; y++) {
            diskData.writeByte(writeTranslateTable62[bb00[y] ^ bb00[y-1]]);
        }
        diskData.writeByte(writeTranslateTable62[bb00[255]]);
        assert(diskData.position() == RAW_BUFFER_SIZE+1);
        return diskData;
    }
}
