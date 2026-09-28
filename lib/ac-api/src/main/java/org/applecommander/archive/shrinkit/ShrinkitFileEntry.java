/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.applecommander.archive.shrinkit;

import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.shrinkit.HeaderBlock;
import org.applecommander.shrinkit.ThreadFormat;
import org.applecommander.shrinkit.ThreadKind;
import org.applecommander.shrinkit.ThreadRecord;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.FileMagic;

import java.io.IOException;
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
    public long getSize() {
        return headerBlock.getUncompressedSize();
    }

    @Override
    public String getFiletype() {
        if (headerBlock.getFileSysId() == 1) {
            return FileMagic.getProdosFileTypeText((int) headerBlock.getFileType(), (int) headerBlock.getExtraType());
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
        return headerBlock.getDataForkThreadRecord()
            .or(headerBlock::getResourceForkThreadRecord)
            .map(ThreadRecord::getThreadFormat)
            .map(ThreadFormat::getName)
            .orElse("?");
    }
    public long getCompressedSize() {
        return headerBlock.getCompressedSize();
    }
    public int getDataForkCrc() {
        return headerBlock.getDataForkThreadRecord()
            .map(ThreadRecord::getThreadCrc)
            .orElse(0);
    }
    public int getResourceForkCrc() {
        return headerBlock.getResourceForkThreadRecord()
            .map(ThreadRecord::getThreadCrc)
            .orElse(0);
    }
    public int getFileSysId() {
        return headerBlock.getFileSysId();
    }
    public String getFileSysIdText() {
        return headerBlock.getFileSysIdText();
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
        try {
            return record.readThreadData();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public DataBuffer getDataFork() {
        return headerBlock.getDataForkThreadRecord()
            .map(this::decompress)
            .map(DataBuffer::wrap)
            .orElseGet(() -> DataBuffer.create(0));
    }

    @Override
    public Optional<DataBuffer> getResourceFork() {
        return headerBlock.getResourceForkThreadRecord()
            .map(this::decompress)
            .map(DataBuffer::wrap);
    }

    @Override
    public ContentType getContentType() {
        Optional<ThreadRecord> record = headerBlock.getDataForkThreadRecord();
        if (record.isPresent() && record.get().getThreadKind() == ThreadKind.DISK_IMAGE) {
            return ContentType.DISK_IMAGE;
        }
        return ContentType.UNKNOWN;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, headerBlock);
    }
}
