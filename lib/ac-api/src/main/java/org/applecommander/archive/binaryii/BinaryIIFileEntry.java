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
package org.applecommander.archive.binaryii;

import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.ui.ProdosUtils;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.FileMagic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class BinaryIIFileEntry implements FileEntry, BinaryIIConstants {
    private final BinaryIIFileStore fileStore;
    private final DataBuffer header;
    private final DataBuffer data;

    BinaryIIFileEntry(BinaryIIFileStore fileStore, DataBuffer header, DataBuffer data) {
        this.fileStore = fileStore;
        this.header = header;
        this.data = data;
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
        String fileName = header.getPascalString(FILE_NAME);
        if (fileName.length() <= 15 && header.getUnsignedByte(NATIVE_NAME) > 0) {
            fileName = header.getPascalString(NATIVE_NAME);
        }
        return fileName;
    }

    /// Returns the size, in bytes, of this item.
    /// It may be approximate (based off a sector count, for instance).
    @Override
    public long getSize() {
        return (long) header.getUnsignedByte(GSOS_EOF) << 24 | header.getUnsignedShort3(EOF);
    }

    public int getAccess() {
        return header.getUnsignedByte(ACCESS_CODE);
    }
    public String getAccessText() {
        return ProdosUtils.getAccessText(getAccess());
    }

    public int getFileType() {
        return header.getUnsignedByte(FILE_TYPE);
    }
    public String getFileTypeText() {
        // TODO do we need to pay attention to OS Type or is this usually just ProDOS files?
        return ProdosUtils.getFileTypeText(getFileType(), getAuxType());
    }

    public int getAuxType() {
        return header.getUnsignedByte(AUX_TYPE);
    }

    public int getStorageType() {
        return header.getUnsignedByte(STORAGE_TYPE);
    }
    public String getStorageTypeText() {
        return ProdosUtils.getStorageTypeText(getStorageType());
    }

    public int getBlocksUsed() {
        return header.getUnsignedShort(GSOS_FILE_SIZE) << 16 | header.getUnsignedShort(FILE_SIZE);
    }

    public Date getCreateDate() {
        return header.getProdosDate(CREATION_DATE);
    }
    public Date getModificationDate() {
        return header.getProdosDate(MODIFICATION_DATE);
    }

    public int getOSType() {
        return header.getUnsignedByte(OS_TYPE);
    }
    public String getOSTypeText() {
        return switch (getOSType()) {
            case 0x00 -> "ProDOS or SOS";
            case 0x01 -> "DOS 3.3";
            case 0x03 -> "DOS 3.2 or DOS 3.1";
            case 0x04 -> "Apple II Pascal";
            case 0x05 -> "Macintosh MFS";
            case 0x06 -> "Macintosh HFS";
            case 0x07 -> "Lisa Filing System";
            case 0x08 -> "Apple CP/M";
            case 0x0a -> "MS-DOS";
            case 0x0b -> "High Sierra (CD-ROM)";
            case 0x0c -> "ISO 9660 (CD-ROM)";
            case 0x0d -> "AppleShare";
            default -> String.format("Reserved ($%02X)", getOSType());
        };
    }

    public int getDataFlags() {
        return header.getUnsignedByte(DATA_FLAGS);
    }
    public String getDataFlagsText() {
        List<String> flags = new ArrayList<>();
        int dataFlags = getDataFlags();
        if ((dataFlags & 0b10000000) != 0) {
            flags.add("Compressed");
        }
        if ((dataFlags & 0b01000000) != 0) {
            flags.add("Encrypted");
        }
        if ((dataFlags & 0b00000001) != 0) {
            flags.add("Sparse");
        }
        if (flags.isEmpty()) {
            flags.add("- None -");
        }
        return String.join(",", flags);
    }

    public int getVersion() {
        return header.getUnsignedByte(VERSION);
    }

    /// Return the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public DataBuffer getDataFork() {
        return data;
    }

    /// Interpret the `FileEntry` as if it were on a ProDOS volume.
    /// The intention is that ProDOS ended up being the common denominator in many tools and formats,
    /// so this allows each file store to manage the interpretation itself.
    @Override
    public ProdosAttributes getProdosAttributes() {
        return ProdosAttributes.builder()
                .name(getName())
                .fileType(getFileType())
                .auxType(getAuxType())
                .access(getAccess())
                .modification(getModificationDate())
                .creation(getCreateDate())
                .size(getSize())
                .dataFork(data)
                .get();
    }

    @Override
    public ContentType getContentType() {
        ContentType contentType = FileMagic.getProdosContentType(getFileType(), getAuxType());
        if (contentType == ContentType.UNKNOWN) {
            contentType = FileMagic.identifyContentType(getName());
        }
        return contentType;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Optional.empty();
    }
}
