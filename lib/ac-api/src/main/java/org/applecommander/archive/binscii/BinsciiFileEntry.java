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
package org.applecommander.archive.binscii;

import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.shrinkit.CRC16;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.ui.ProdosUtils;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.FileMagic;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public class BinsciiFileEntry implements FileEntry, BinsciiConstants {
    private final BinsciiFileStore fileStore;
    private final String name;
    private final DataBuffer header;
    private final DataBuffer filedata;
    private final boolean checksumMatches;

    public BinsciiFileEntry(BinsciiFileStore fileStore, String name, DataBuffer header, DataBuffer filedata) {
        Objects.requireNonNull(fileStore);
        Objects.requireNonNull(name);
        Objects.requireNonNull(header);
        Objects.requireNonNull(filedata);
        this.fileStore = fileStore;
        this.name = name;
        this.header = header;
        this.filedata = filedata;

        CRC16 crc = new CRC16();
        crc.update(header.slice(0, OFFSET_CRC).asBytes());
        checksumMatches = crc.getValue() == header.getUnsignedShort(OFFSET_CRC);
    }

    /// The `Directory` that holds this file.
    @Override
    public Directory getParent() {
        return fileStore.getRootDirectory();
    }

    /// Indicates if this `FileEntry` is deleted.
    @Override
    public boolean isDeleted() {
        return false;
    }

    /// Returns the `FileStore` that this `Entry` originates from.
    @Override
    public FileStore getFileStore() {
        return fileStore;
    }

    /// Returns the name of this entry: either a directory name or a file name.
    /// This does not contain any directory components -- strictly the name of this entry.
    /// Note that depending on context, this may be a computed field.
    @Override
    public String getName() {
        return name;
    }

    /// Returns the size, in bytes, of this item.
    /// It may be approximate (based off a sector count, for instance).
    @Override
    public long getSize() {
        return filedata.limit();
    }

    /// Return the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public DataBuffer getDataFork() {
        return filedata;
    }

    public int getAccess() {
        return header.getUnsignedByte(OFFSET_ACCESS);
    }
    public String getAccessText() {
        return ProdosUtils.getAccessText(getAccess());
    }
    public int getFileType() {
        return header.getUnsignedByte(OFFSET_FILETYPE);
    }
    public String getFileTypeText() {
        return ProdosUtils.getFileTypeText(getFileType(), getAuxType());
    }
    public int getAuxType() {
        return header.getUnsignedShort(OFFSET_AUXTYPE);
    }
    public int getStorageType() {
        return header.getUnsignedByte(OFFSET_STORETYPE);
    }
    public String getStorageTypeText() {
        return ProdosUtils.getStorageTypeText(getStorageType());
    }
    public int getTotalBlocks() {
        return header.getUnsignedShort(OFFSET_BLKSIZE);
    }
    public Date getCreated() {
        return header.getProdosDate(OFFSET_CREDATE);
    }
    public Date getModified() {
        return header.getProdosDate(OFFSET_MODDATE);
    }
    public int getChecksum() {
        return header.getUnsignedByte(OFFSET_CRC);
    }
    public boolean checksumMatches() {
        return checksumMatches;
    }

    @Override
    public ContentType getContentType() {
        return FileMagic.getProdosContentType(getFileType(), getAuxType());
    }

    /// Interpret the `FileEntry` as if it were on a ProDOS volume.
    /// The intention is that ProDOS ended up being the common denominator in many tools and formats,
    /// so this allows each file store to manage the interpretation itself.
    @Override
    public ProdosAttributes getProdosAttributes() {
        return ProdosAttributes.builder()
                .name(name)
                .dataFork(filedata)
                .access(getAccess())
                .auxType(getAuxType())
                .fileType(getFileType())
                .creation(getCreated())
                .modification(getModified())
                .size(getSize())
                .get();
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Optional.empty();
    }
}
