package com.webcodepro.applecommander.storage.physical;

import com.webcodepro.applecommander.storage.StorageBundle;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.TextBundle;

public class NibbleCodec {
    private static TextBundle textBundle = StorageBundle.getInstance();

    /**
     * This is the 6 and 2 write translate table, as given in Beneath
     * Apple DOS, pg 3-21.
     */
    private static int[] writeTranslateTable = {
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
     * The read translation table.  Constructed from the write
     * translate table.  Used to decode a disk byte into a value
     * from 0x00 to 0x3f which is further decoded...
     */
    private static int[] readTranslateTable;
    static {
        // Construct the read translation table:
        readTranslateTable = new int[256];
        for (int i=0; i<writeTranslateTable.length; i++) {
            readTranslateTable[writeTranslateTable[i]] = i;
        }
    }

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
     * Locate physical sector in the given track data. Decodes 4+4 as well as 6+2.
     */
    public static byte[] readSectorFromTrack(byte[] trackData, int track, int physicalSector, int sectorsPerTrack) {
        // 2. locate address field for this track and sector
        int offset = 0;
        byte[] addressField = new byte[14];
        boolean found = false;
        int attempts = sectorsPerTrack;
        while (!found && attempts >= 0) {
            int nextOffset = locateField(0xd5, 0xaa, 0x96, trackData, addressField, offset);
            attempts--;
            offset = nextOffset;
            int t = decodeOddEven(addressField, 5);
            int s = decodeOddEven(addressField, 7);
            found = (t == track && s == physicalSector);
        }
        if (!found) {
            System.out.println("Looking for header:");
            byte[] header = new byte[] { (byte)0xd5, (byte)0xaa, (byte)0x96, 0, 0, 0, 0, 0, 0 };
            encodeOddEven(header, 3, 0xff);
            encodeOddEven(header, 5, track);
            encodeOddEven(header, 7, physicalSector);
            System.out.println(AppleUtil.getHexDump(header));
            System.out.printf("Track #%02d:\n", track);
            System.out.println(AppleUtil.getHexDump(trackData));
            throw new IllegalArgumentException(textBundle
                    .format("NibbleOrder.InvalidPhysicalSectorError", physicalSector, track, 1)); //$NON-NLS-1$
        }
        // 3. read data field that immediately follows the address field
        byte[] dataField = new byte[349];
        locateField(0xd5, 0xaa, 0xad, trackData, dataField, offset);
        // 4. translate data field
        byte[] buffer = new byte[342];
        int checksum = 0;
        for (int i=0; i<buffer.length; i++) {
            int b = AppleUtil.getUnsignedByte(dataField[i+3]);
            checksum ^= readTranslateTable[b];		// XOR
            if (i < 86) {
                buffer[buffer.length - i - 1] = (byte) checksum;
            } else {
                buffer[i - 86] = (byte) checksum;
            }
        }
        checksum ^= readTranslateTable[AppleUtil.getUnsignedByte(dataField[345])];
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
     * Locate a field on the track.  These are identified by a 3 byte unique
     * signature.  Because of the way in which disk bytes are captured, we need
     * to wrap around the track to ensure all sequences of bytes are accounted for.
     * <p>
     * This method fills fieldData as well as returning the last position referenced
     * in the track buffer.
     */
    public static int locateField(int byte1, int byte2, int byte3, byte[] trackData, byte[] fieldData, int startingOffset) {
        int i = startingOffset;	// logical position in track buffer (can wrap)
        int position = 0;			// physical position in field buffer
        while (i < trackData.length + fieldData.length) {
            int offset = i % trackData.length;	// physical position in track buffer
            int b = AppleUtil.getUnsignedByte(trackData[offset]);
            if (position == 0 && b == byte1) {
                fieldData[position++] = (byte) b;
            } else if (position == 1 && b == byte2) {
                fieldData[position++] = (byte) b;
            } else if (position == 2 && b == byte3) {
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
            int nextOffset = locateField(0xd5, 0xaa, 0x96, trackData, addressField, offset);
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
                diskData[pos++] = (byte) writeTranslateTable[bc00[y-1]];
            } else {
                diskData[pos++] = (byte) writeTranslateTable[bc00[y] ^ bc00[y-1]];
            }
        }
        diskData[pos++] = (byte) writeTranslateTable[bc00[0] ^ bb00[y]];
        for (y=1; y<256; y++) {
            diskData[pos++] = (byte) writeTranslateTable[bb00[y] ^ bb00[y-1]];
        }
        diskData[pos++] = (byte) writeTranslateTable[bb00[255]];

        // 5. write to disk (data may wrap - hence the manual copy)
        byte[] dataFieldPrologue = new byte[3];
        offset= locateField(0xd5, 0xaa, 0xad, trackData, dataFieldPrologue, offset);
        for (int i=0; i<diskData.length; i++) {
            pos = (offset + i) % trackData.length;
            trackData[pos] = diskData[i];
        }
    }
}
