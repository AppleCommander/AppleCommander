package org.applecommander.codec;

import org.applecommander.capability.Capability;
import org.applecommander.util.DataBuffer;

public class Nibble53Disk525Codec implements NibbleDiskCodec {
    private static final int RAW_BUFFER_SIZE = 410;
    private static final int SECTOR_SIZE = 256;
    /**
     * This is the 5 and 3 write translate table, as given in Beneath
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

    @Override
    public boolean can(Capability capability) {
        // Encoding is not implemented.
        return false;
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
            checksum ^= readTranslateTable53[b];		// XOR
            if (i < 154) {
                buffer.putByte(RAW_BUFFER_SIZE - i - 1, checksum);
            } else {
                buffer.putByte(i - 154, checksum);
            }
        }
        checksum ^= readTranslateTable53[rawData.getUnsignedByte(RAW_BUFFER_SIZE)];
        if (checksum != 0) return null;	// BAD DATA -- FIXME when logging is enabled
        // Decode the data field
        // Note: This reads from these ranges and leaves two bytes that we use at end:
        // 0x00-0x33, 0x33-0x65, 0x66-0x98, 0x99-0xcb, 0xcc-0xfe, 0xff - unused
        // 0x100-0x132, 0x133-0x165, 0x166-0x198, 0x199 - unused
        DataBuffer sectorData = DataBuffer.create(SECTOR_SIZE);
        for (int i=0x33-1; i>= 0; i--) {
            int a76543 = buffer.getUnsignedByte(i);
            int b76543 = buffer.getUnsignedByte(0x33 + i);
            int c76543 = buffer.getUnsignedByte(0x66 + i);
            int d76543 = buffer.getUnsignedByte(0x99 + i);
            int e76543 = buffer.getUnsignedByte(0xcc + i);
            int a210d2e2 = buffer.getUnsignedByte(0x100 + i);
            int b210d1e1 = buffer.getUnsignedByte(0x133 + i);
            int c210d0e0 = buffer.getUnsignedByte(0x166 + i);

            sectorData.writeByte(a76543 << 3 | (a210d2e2 >> 2) & 0x7);
            sectorData.writeByte(b76543 << 3 | (b210d1e1 >> 2) & 0x7);
            sectorData.writeByte(c76543 << 3 | (c210d0e0 >> 2) & 0x7);
            sectorData.writeByte(d76543 << 3 | (a210d2e2 << 1) & 0x4 | b210d1e1 & 0x2 | (c210d0e0 >> 1) & 0x1);
            sectorData.writeByte(e76543 << 3 | (a210d2e2 << 2) & 0x4 | (b210d1e1 << 1) & 0x2 | c210d0e0 & 0x1);
        }
        sectorData.writeByte(buffer.getUnsignedByte(0xff) << 3 | buffer.getUnsignedByte(0x199 & 0x07));
        assert(sectorData.position() == SECTOR_SIZE);
        return sectorData;
    }

    @Override
    public DataBuffer encode(DataBuffer data) {
        throw new RuntimeException("5&3 encoding not implemented");
    }
}
