package org.applecommander.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A DataBuffer is a bunch of bytes with Apple II knowledge.
 * This shares the technical details as well as the actual image data
 * across all components in the application. There are two sets of
 * accessors get/put the index the buffer and read/write that
 * navigate via position. Note that read and write can overrun
 * the buffer and generate an error.
 */
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
    public static DataBuffer create(int size) {
        byte[] data = new byte[size];
        return wrap(data);
    }

    private DataBuffer() {
        // prevent construction
    }

    // UTILITY METHODS

    public DataBuffer slice(int offset, int length) {
        DataBuffer dbuf = new DataBuffer();
        dbuf.buffer = buffer.slice(offset, length);
        dbuf.buffer.order(ByteOrder.LITTLE_ENDIAN);
        return dbuf;
    }
    public int limit() {
        return this.buffer.limit();
    }

    // GET/PUT RELATED FUNCTIONS

    public int getUnsignedByte(int index) {
        return Byte.toUnsignedInt(this.buffer.get(index));
    }
    public void put(int offset, DataBuffer data) {
        this.buffer.put(offset, data.buffer, 0, data.buffer.limit());
    }
    public void putByte(int offset, int value) {
        this.buffer.put(offset, (byte)value);
    }
    public void putBytes(int offset, int... values) {
        for (int value : values) {
            putByte(offset++, value);
        }
    }

    // READ/WRITE RELATED METHODS

    public boolean hasRemaining() {
        return this.buffer.hasRemaining();
    }
    public int position() {
        return this.buffer.position();
    }
    public void position(int n) {
        this.buffer.position(n);
    }
    public int readUnsignedByte() {
        return Byte.toUnsignedInt(this.buffer.get());
    }
    public void writeByte(int value) {
        this.buffer.put((byte)value);
    }
}
