package org.applecommander.archive.shrinkit;

import com.webcodepro.shrinkit.HeaderBlock;
import com.webcodepro.shrinkit.ThreadRecord;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.util.Container;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Optional;

public class ShrinkitFileEntry implements FileEntry {
    private final ShrinkitFileStore fileStore;
    private final HeaderBlock headerBlock;

    public ShrinkitFileEntry(ShrinkitFileStore fileStore, HeaderBlock headerBlock) {
        this.fileStore = fileStore;
        this.headerBlock = headerBlock;
    }

    @Override
    public ShrinkitDirectory getParent() {
        return null;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public FileStore getFileStore() {
        return fileStore;
    }

    @Override
    public String getName() {
        return headerBlock.getFilename();
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() {
        return headerBlock.getUncompressedSize();
    }

    @Override
    public String getFiletype() {
        if (headerBlock.getFileSysId() == 1) {
            // FIXME PRODOS filetypes will likely be managed elsewhere
            return switch ((int) headerBlock.getFileType()) {
                case 0x04 -> "TXT";
                case 0x06 -> "BIN";
                case 0x0f -> "DIR";
                case 0x19 -> "ADB";
                case 0x1a -> "AWP";
                case 0x1b -> "ASP";
                case 0xfa -> "INT";
                case 0xfc -> "BAS";
                case 0xfd -> "VAR";
                case 0xff -> "SYS";
                default -> String.format("$%02X", headerBlock.getFileType());
            };
        }
        // FIXME
        return "?";
    }

    public int getStorageType() {
        return headerBlock.getStorageType();
    }
    public String getStorageTypeString() {
        return switch (getStorageType()) {
            case 0x1 -> "Seedling";
            case 0x2 -> "Sapling";
            case 0x3 -> "Tree";
            case 0x4 -> "Pascal";
            case 0x5 -> "Extended";
            case 0xd -> "Directory";
            case 0xe -> "Subdirectory";
            case 0xf -> "Volume Directory";
            default -> "?";
        };
    }
    public long getExtraType() {
        return headerBlock.getExtraType();
    }
    public Date getArchiveWhen() {
        return headerBlock.getArchiveWhen();
    }
    public String getThreadFormat() {
        ThreadRecord record = headerBlock.getDataForkThreadRecord();
        if (record == null) {
            record = headerBlock.getResourceForkThreadRecord();
        }
        if (record == null) {
            return "?";
        }
        return record.getThreadFormat().getName();
    }
    public long getCompressedSize() {
        return headerBlock.getCompressedSize();
    }
    public int getDataForkCrc() {
        ThreadRecord record = headerBlock.getDataForkThreadRecord();
        if (record == null) {
            return 0;
        }
        return record.getThreadCrc();
    }
    public int getResourceForkCrc() {
        ThreadRecord record = headerBlock.getResourceForkThreadRecord();
        if (record == null) {
            return 0;
        }
        return record.getThreadCrc();
    }
    public int getFileSysId() {
        return headerBlock.getFileSysId();
    }
    public String getFileSysIdString() {
        return switch (getFileSysId()) {
            case 0x0001 -> "ProDOS/SOS";
            case 0x0002 -> "DOS 3.3";
            case 0x0003 -> "DOS 3.2";
            case 0x0004 -> "Apple II Pascal";
            case 0x0005 -> "Macintosh HFS";
            case 0x0006 -> "Macintosh MFS";
            case 0x0007 -> "Lisa File System";
            case 0x0008 -> "Apple CP/M";
            case 0x000A -> "MS-DOS";
            case 0x000B -> "High Sierra";
            case 0x000C -> "ISO 9660";
            case 0x000D -> "AppleShare";
            default -> String.format("Reserved ($%04X)", getFileSysId());
        };
    }
    public int getFileSysInfo() {
        return headerBlock.getFileSysInfo();
    }
    public long getAccess() {
        return headerBlock.getAccess();
    }
    public String getAccessString() {
        StringBuilder sb = new StringBuilder();
        String flags = "RWI--BRD";  // string index is bit value
        for (int i=0; i<flags.length(); i++) {
            int bit = i^2;
            if ((getAccess() & bit) != 0) {
                sb.append(flags, i, i+1);
            }
            else {
                sb.append("-");
            }
        }
        return sb.toString();
    }
    public Date getCreateWhen() {
        return headerBlock.getCreateWhen();
    }
    public Date getModWhen() {
        return headerBlock.getModWhen();
    }

    private byte[] decompress(ThreadRecord record) {
        if (record == null) {
            return new byte[0];
        }
        try (InputStream is = record.getInputStream()) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] getDataFork() {
        return decompress(headerBlock.getDataForkThreadRecord());
    }

    @Override
    public byte[] getResourceFork() {
        return decompress(headerBlock.getResourceForkThreadRecord());
    }

    @Override
    public void setDataFork(byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setResourceFork(byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, headerBlock);
    }
}
