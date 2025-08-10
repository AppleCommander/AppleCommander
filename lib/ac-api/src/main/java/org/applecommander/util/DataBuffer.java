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
    private static final int[] BIT_MASKS = { 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80 };

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
    public void limit(int newLimit) {
        this.buffer.limit(newLimit);
    }

    // GET/PUT RELATED FUNCTIONS

    public int getUnsignedByte(int index) {
        return Byte.toUnsignedInt(this.buffer.get(index));
    }
    public int getUnsignedShort(int index) {
        return Short.toUnsignedInt(this.buffer.getShort(index));
    }
    public int getUnsignedShortBE(int index) {
        this.buffer.order(ByteOrder.BIG_ENDIAN);
        int value = getUnsignedShort(index);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        return value;
    }
    public int getInt(int index) {
        return this.buffer.getInt(index);
    }
    public int getIntBE(int index) {
        this.buffer.order(ByteOrder.BIG_ENDIAN);
        int value = getInt(index);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        return value;
    }
    public void get(int position, byte[] data) {
        // Hopefully this is a bridge method and can be removed over time
        this.buffer.get(position, data);
    }
    public String getFixedLengthString(int index, int length) {
        byte[] s = new byte[length];
        this.buffer.get(index, s);
        return new String(s);
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
    /**
     * Determine if a specific bit is set.
     */
    public boolean isBitSet(int index, int bit) {
        return (getUnsignedByte(index) & BIT_MASKS[bit]) != 0;
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
    public int readUnsignedShort() {
        return Short.toUnsignedInt(this.buffer.getShort());
    }
    /** Read an short but in big endian format. Used for marker bytes so they "look" as expected in program code. */
    public int readUnsignedShortBE() {
        this.buffer.order(ByteOrder.BIG_ENDIAN);
        int value = readUnsignedShort();
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        return value;
    }
    public void read(byte[] data) {
        this.buffer.get(data);
    }
    public int readInt() {
        return this.buffer.getInt();
    }
    /** Read an int but in big endian format. Used for marker bytes so they "look" as expected in program code. */
    public int readIntBE() {
        this.buffer.order(ByteOrder.BIG_ENDIAN);
        int value = readInt();
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        return value;
    }
    public DataBuffer readBuffer(int length) {
        DataBuffer buf = slice(position(), length);
        this.buffer.position(position()+length);
        return buf;
    }
    public String readFixedLengthString(int length) {
        byte[] s = new byte[length];
        this.buffer.get(s);
        return new String(s);
    }
    public String readPascalString(int maxLength) {
        int startingPosition = position();
        int length = readUnsignedByte();
        assert(length < maxLength);
        byte[] s = new byte[length];
        this.buffer.get(s);
        this.position(startingPosition+maxLength);
        return new String(s);
    }
    public void writeByte(int value) {
        this.buffer.put((byte)value);
    }
}
