package org.applecommander.image;

import org.applecommander.capability.Capability;
import org.applecommander.hint.Hint;
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.util.List;
import java.util.Optional;

public class UniversalDiskImage implements Source {
    public static final int MAGIC = 0x32494d47;     // "2IMG" marker
    public static final int HEADER_SIZE = 0x40;     // documented header size
    public static final int HEADER_SIZE_ALT = 0x34; // found in KeyWin.2mg

    private final Source source;
    private final Info info;

    public UniversalDiskImage(Source source) {
        this.source = source;

        DataBuffer header = source.readBytes(0, HEADER_SIZE);
        if (header.readIntBE() != MAGIC) {
            throw new RuntimeException("magic bytes do not match for 2IMG");
        }
        String creator = header.readFixedLengthString(4);
        int headerSize = header.readUnsignedShort();
        assert(headerSize == HEADER_SIZE || headerSize == HEADER_SIZE_ALT);
        int version = header.readUnsignedShort();
        assert(version == 0 || version == 1);
        int imageFormat = header.readInt();
        int flags = header.readInt();
        int prodosBlocks = header.readInt();
        int dataOffset = header.readInt();
        int dataLength = header.readInt();
        int commentOffset = header.readInt();
        int commentLength = header.readInt();
        int creatorOffset = header.readInt();
        int creatorLength = header.readInt();

        String comment = "";
        if (commentOffset > 0 && commentLength > 0) {
            DataBuffer commentBuffer = source.readBytes(commentOffset, commentLength);
            comment = commentBuffer.readFixedLengthString(commentLength);
        }
        DataBuffer creatorData = DataBuffer.create(0);
        if (creatorOffset > 0 && creatorLength > 0) {
            creatorData = source.readBytes(creatorOffset, creatorLength);
        }
        this.info = new Info(creator, headerSize, version, imageFormat, flags, prodosBlocks,
                dataOffset, dataLength, comment, creatorData);
    }

    public Info getInfo() {
        return info;
    }

    @Override
    public boolean can(Capability capability) {
        // TODO we haven't defined any yet
        return false;
    }

    @Override
    public int getSize() {
        return source.getSize() - HEADER_SIZE;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, this, source, info);
    }

    @Override
    public boolean is(Hint hint) {
        return switch (hint) {
            case DOS_SECTOR_ORDER -> info.imageFormat() == 0;
            case PRODOS_BLOCK_ORDER -> info.imageFormat() == 1;
            case NIBBLE_SECTOR_ORDER -> info.imageFormat() == 2;
            default -> false;
        };
    }

    @Override
    public DataBuffer readAllBytes() {
        return source.readBytes(this.info.dataOffset, this.info.dataLength);
    }

    @Override
    public DataBuffer readBytes(int offset, int length) {
        return source.readBytes(this.info.dataOffset + offset, length);
    }

    @Override
    public void writeBytes(int offset, DataBuffer data) {
        source.writeBytes(this.info.dataOffset + offset, data);
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
        List<Information> list = source.information();
        Info info = getInfo();
        list.add(Information.builder("Image Type").value("Universal Disk Image (2IMG/2MG)"));
        list.add(Information.builder("Creator ID").value("%s (%s)", info.creator(),
                switch(info.creator()) {
                    case "!nfc" -> "ASIMOV2";
                    case "B2TR" -> "Bernie [] the Rescue";
                    case "CTKG" -> "Catakig";
                    case "ShIm" -> "Shelly's ImageMaker";
                    case "WOOF" -> "Sweet 16";
                    default -> "Unknown";
                }));
        list.add(Information.builder("Version").value(info.version()));
        list.add(Information.builder("Image Format").value("%d (%s)", info.imageFormat(),
                switch(info.imageFormat()) {
                    case 0 -> "DOS 3.3 sector order";
                    case 1 -> "ProDOS sector order";
                    case 2 -> "Nibble data";
                    default -> "Unknown";
                }));
        list.add(Information.builder("Flags").value("$%02x (%s locked; volume %d)",
                info.flags(), info.isLocked() ? "is" : "is not", info.getDosVolumeNumber()));
        list.add(Information.builder("ProDOS Blocks").value(info.prodosBlocks()));
        list.add(Information.builder("Data Offset & Length").value("$%08x & %08x",
                info.dataOffset(), info.dataLength()));
        list.add(Information.builder("Comment").value(info.comment()));
        return list;
    }

    public record Info(String creator, int headerSize, int version, int imageFormat, int flags,
                       int prodosBlocks, int dataOffset, int dataLength, String comment, DataBuffer creatorData) {
        public boolean isDOSOrdered() {
            return imageFormat == 0;
        }
        public boolean isProdosOrdered() {
            return imageFormat == 1;
        }
        public boolean isNibbleOrder() {
            return imageFormat == 2;
        }
        public boolean isLocked() {
            return flags < 0;   // bit 31 is sign bit for Java int
        }
        public int getDosVolumeNumber() {
            if (isDOSOrdered() && (flags & 0x10) != 0) {
                int volume = flags & 0xff;
                if (volume == 0) volume = 0xff;
                return volume;
            }
            return 0;
        }
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
                if (header.getIntBE(0) != MAGIC) {
                    // Need the magic bytes!
                }
                else if (header.getUnsignedShort(8) != HEADER_SIZE
                         && header.getUnsignedShort(8) != HEADER_SIZE_ALT) {
                    // Expecting header size to match
                }
                else if (header.getUnsignedShort(10) > 1) {
                    // We only have seen versions 0 and 1
                }
                else if (header.getInt(0x18) + header.getInt(0x1c) > source.getSize()) {
                    // We at least expect data to be a valid size within the file
                }
                else {
                    return Optional.of(new UniversalDiskImage(source));
                }
            }
            return Optional.empty();
        }
    }
}
