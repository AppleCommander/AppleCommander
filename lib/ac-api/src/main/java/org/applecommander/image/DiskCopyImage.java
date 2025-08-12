package org.applecommander.image;

import org.applecommander.capability.Capability;
import org.applecommander.hint.Hint;
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

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
        int calculatedDataChecksum = checksum(HEADER_SIZE, dataSize);
        int calculatedTagChecksum = checksum(HEADER_SIZE+dataSize, tagSize);
        this.info = new Info(diskName, dataSize, tagSize, dataChecksum, tagChecksum, diskFormat, formatByte,
                calculatedDataChecksum, calculatedTagChecksum);
    }

    /**
     * Calculate the expected checksum. Note that we try to be careful about length as there
     * are DiskCopy images that appear to have been truncated over time. Since the checksums should NOT
     * match, presumably the user may notice that detail!
     */
    public int checksum(int offset, int requestedLength) {
        int checksum = 0;
        if (offset < source.getSize() && requestedLength > 0) {
            int length = Math.min(source.getSize()-offset, requestedLength);
            DataBuffer data = source.readBytes(offset, length);
            while (length > 0) {
                checksum += data.readUnsignedShortBE();
                int carry = checksum & 1;
                checksum >>>= 1;
                checksum |= carry << 31;
                length -= 2;
            }
        }
        return checksum;
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

    @Override
    public boolean is(Hint hint) {
        return hint == Hint.PRODOS_BLOCK_ORDER;
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

    @Override
    public List<Information> information() {
        BiFunction<Integer,Integer,String> cksumFn = (a, e) -> {
            if (Objects.equals(a, e)) {
                return String.format("$%08x (verified)", a);
            }
            return String.format("$%08x (expected $%08x)", a, e);
        };
        List<Information> list = source.information();
        Info info = getInfo();
        list.add(Information.builder("Image Type").value("Disk Copy"));
        list.add(Information.builder("Disk Name").value(info.diskName()));
        list.add(Information.builder("Data Size").value(info.dataSize()));
        list.add(Information.builder("Tag Size").value(info.tagSize()));
        list.add(Information.builder("Data Checksum")
                .value(cksumFn.apply(info.dataChecksum(), info.calculatedDataChecksum())));
        list.add(Information.builder("Tag Checksum")
                .value(cksumFn.apply(info.tagChecksum(), getInfo().calculatedTagChecksum())));
        list.add(Information.builder("Disk Format").value("%d (%s)", info.diskFormat(),
                switch(info.diskFormat()) {
                    case 0 -> "400K - GCR CLV ssdd";
                    case 1 -> "800K - GCR CLV dsdd";
                    case 2 -> "720K - MFM CAV dsdd";
                    case 3 -> "1440K - MFM CAV dshd";
                    default -> "Reserved";
                }));
        list.add(Information.builder("Format Byte").value("%02x (%s)", info.formatByte(),
                switch(info.formatByte()) {
                    case 0x02 -> "400K Macintosh";
                    case 0x12 -> "400K";
                    case 0x22 -> "800K Macintosh";
                    case 0x24 -> "800K Apple II";
                    default -> "Other";
                }));
        return list;
    }

    public record Info(String diskName, int dataSize, int tagSize, int dataChecksum, int tagChecksum,
                       int diskFormat, int formatByte, int calculatedDataChecksum, int calculatedTagChecksum) {
    }

    public static class Factory implements Source.Factory {
        @Override
        public Optional<Source> fromObject(Object object) {
            return Optional.empty();
        }

        @Override
        public Optional<Source> fromSource(Source source) {
            if (source.getSize() > HEADER_SIZE) {
                DataBuffer header = source.readBytes(0, HEADER_SIZE);
                if (header.getUnsignedShortBE(HEADER_SIZE-2) != 0x100) {
                    // Not Disk Copy
                }
                else if (header.getUnsignedByte(0) > 63) {
                    // Name length must fit in first 64 bytes
                }
//                else if (HEADER_SIZE + header.getIntBE(64) + header.getIntBE(68) > source.getSize()) {
//                    // Header + Data + Tag length is too big
//                }
                else {
                    return Optional.of(new DiskCopyImage(source));
                }
            }
            return Optional.empty();
        }
    }
}
