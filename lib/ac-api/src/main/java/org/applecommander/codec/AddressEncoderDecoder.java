package org.applecommander.codec;

import org.applecommander.capability.Capability;
import org.applecommander.util.DataBuffer;

public class AddressEncoderDecoder implements NibbleEncoderDecoder {
    @Override
    public boolean can(Capability capability) {
        if (capability == Capability.ENCODE) {
            return true;
        }
        return false;
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
    @Override
    public DataBuffer decode(DataBuffer rawData) {
        assert(rawData.limit() == 2);
        int b1 = rawData.getUnsignedByte(0);
        int b2 = rawData.getUnsignedByte(1);
        return DataBuffer.wrap((b1 << 1 | 0x01) & b2);
    }

    /**
     * Encode odd-even bytes to be stored on disk.  See decodeOddEven
     * for the format.
     * @see #decode
     */
    @Override
    public DataBuffer encode(DataBuffer data) {
        assert(data.limit() == 1);
        int value = data.getUnsignedByte(0);
        int b1 = (value >> 1) | 0xaa;
        int b2 = value | 0xaa;
        return DataBuffer.wrap(b1, b2);
    }
}
