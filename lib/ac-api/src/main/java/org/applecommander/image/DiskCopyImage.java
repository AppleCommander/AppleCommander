package org.applecommander.image;

import org.applecommander.capability.Capability;
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

public class DiskCopyImage implements Source {
    public static final int HEADER_SIZE = 84;

    private final Source source;
    private final Info info;

    public DiskCopyImage(Source source) {
        this.source = source;

        DataBuffer header = source.readBytes(0, HEADER_SIZE);
        String diskName = header.readPascalString(64);
        int dataSize = header.readIntBE();
        int tagSize = header.readIntBE();
        int dataChecksum = header.readIntBE();
        int tagChecksum = header.readIntBE();
        int diskFormat = header.readUnsignedByte();
        int formatByte = header.readUnsignedByte();
        int magic = header.readUnsignedShortBE();
        assert(magic == 0x100);
        assert(header.position() == HEADER_SIZE);
        this.info = new Info(diskName, dataSize, tagSize, dataChecksum, tagChecksum, diskFormat, formatByte);
    }

    @Override
    public boolean can(Capability capability) {
        // TODO - not definted yet
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, this, source, info);
    }

    public Info getInfo() {
        return info;
    }

    @Override
    public int getSize() {
        return source.getSize() - HEADER_SIZE;
    }

    @Override
    public DataBuffer readAllBytes() {
        return this.source.readBytes(HEADER_SIZE, getInfo().dataSize());
    }

    @Override
    public DataBuffer readBytes(int offset, int length) {
        return this. source.readBytes(HEADER_SIZE+offset, length);
    }

    @Override
    public void writeBytes(int offset, DataBuffer data) {
        this.source.writeBytes(HEADER_SIZE+offset, data);
    }

    @Override
    public boolean hasChanged() {
        return source.hasChanged();
    }

    @Override
    public void clearChanges() {
        source.clearChanges();
    }

    public record Info(String diskName, int dataSize, int tagSize, int dataChecksum, int tagChecksum,
                       int diskFormat, int formatByte) {
    }
}
