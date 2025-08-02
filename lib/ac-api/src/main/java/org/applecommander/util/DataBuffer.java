package org.applecommander.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DataBuffer {
    private ByteBuffer buffer;

    public static DataBuffer wrap(byte[] data) {
        DataBuffer dbuf = new DataBuffer();
        dbuf.buffer = ByteBuffer.wrap(data);
        dbuf.buffer.order(ByteOrder.LITTLE_ENDIAN);
        return dbuf;
    }
    public static DataBuffer wrap(int... bytes) {
        byte[] data = new byte[bytes.length];
        for (int i=0; i<bytes.length; i++) {
            data[i] = (byte) bytes[i];
        }
        return wrap(data);
    }

    private DataBuffer() {
        // prevent construction
    }
    public DataBuffer slice(int offset, int length) {
        DataBuffer dbuf = new DataBuffer();
        dbuf.buffer = buffer.slice(offset, length);
        dbuf.buffer.order(ByteOrder.LITTLE_ENDIAN);
        return dbuf;
    }
    public void put(int offset, DataBuffer data) {
        this.buffer.put(offset, data.buffer, 0, data.buffer.limit());
    }
    public int limit() {
        return this.buffer.limit();
    }
    public int getUnsignedByte(int index) {
        return Byte.toUnsignedInt(this.buffer.get(index));
    }
}
