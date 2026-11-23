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
package org.applecommander.os.gutenberg;

import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.DataBuffer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GutenbergFileEntry implements FileEntry, GutenbergConstants {
    private final GutenbergDirectory parent;
    private final TrackSectorDevice device;
    private final GutenbergFileHelper helper;
    private final int directoryTrack;
    private final int directorySector;
    private final int directoryOffset;

    GutenbergFileEntry(GutenbergDirectory parent, int directoryTrack, int directorySector, int directoryOffset) {
        this.parent = parent;
        this.device = parent.getFileStore().get(TrackSectorDevice.class).orElseThrow();
        this.helper = parent.getFileStore().get(GutenbergFileHelper.class).orElseThrow();
        this.directoryTrack = directoryTrack;
        this.directorySector = directorySector;
        this.directoryOffset = directoryOffset;
    }

    public DataBuffer readEntry() {
        DataBuffer sector = device.readSector(directoryTrack, directorySector);
        return sector.slice(directoryOffset, ENTRY_LENGTH);
    }

    /// The `Directory` that holds this file.
    @Override
    public Directory getParent() {
        return parent;
    }

    /// Indicates if this `FileEntry` is deleted.
    @Override
    public boolean isDeleted() {
        // Not supported. The entire entry gets wiped out on delete.
        return false;
    }

    /// Returns the `FileStore` that this `Entry` originates from.
    @Override
    public FileStore getFileStore() {
        return parent.getFileStore();
    }

    /// Returns the name of this entry: either a directory name or a file name.
    /// This does not contain any directory components -- strictly the name of this entry.
    /// Note that depending on context, this may be a computed field.
    @Override
    public String getName() {
        return readEntry().getFixedLengthString(ENTRY_NAME_OFFSET, ENTRY_NAME_LENGTH).trim();
    }

    /// Returns the size, in bytes, of this item.
    /// It may be approximate (based off a sector count, for instance).
    @Override
    public long getSize() {
        final int dataLength = TrackSectorDevice.SECTOR_SIZE - HEADER_BYTES;
        return (long) getSectors() * dataLength;
    }

    /// The first track containing file data.
    public int getFirstTrack() {
        int track = readEntry().getUnsignedByte(ENTRY_FIRST_TRACK);
        return (track == 0x40) ? 0 : track;
    }
    /// The first sector containing file data.
    public int getFirstSector() {
        int sector = readEntry().getUnsignedByte(ENTRY_FIRST_SECTOR) & 0x7f;
        return (sector == 0x40) ? 0 : sector;
    }
    /// To number of sectors in this file. (Note that Gutenberg stores track/sector links in the first
    /// 6 bytes of every sector.)
    public int getSectors() {
        // This is a little bit of a cheat. We can't tally with the inline function, but we can add to a list.
        List<Integer> count = new ArrayList<>();
        helper.follow(getFirstTrack(), getFirstSector(), (_, _, _) -> count.add(1));
        return count.size();
    }
    /// The Gutenberg file type.
    public String getType() {
        return readEntry().getFixedLengthString(ENTRY_TYPE, 1);
    }

    /// This is the directory track that the file entry is on.
    public int getDirectoryTrack() {
        return directoryTrack;
    }
    /// This is the directory sector that the file entry is on.
    public int getDirectorySector() {
        return directorySector;
    }
    /// This is the offset into the directory sector.
    public int getDirectoryOffset() {
        return directoryOffset;
    }

    /// Return the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public DataBuffer getDataFork() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final int dataLength = TrackSectorDevice.SECTOR_SIZE - HEADER_BYTES;
        helper.follow(getFirstTrack(), getFirstSector(), (_, _, sectorData) -> {
            out.writeBytes(sectorData.slice(HEADER_BYTES, dataLength).asBytes());
        });
        return DataBuffer.wrap(out.toByteArray());
    }

    /// Interpret the `FileEntry` as if it were on a ProDOS volume.
    /// The intention is that ProDOS ended up being the common denominator in many tools and formats,
    /// so this allows each file store to manage the interpretation itself.
    @Override
    public ProdosAttributes getProdosAttributes() {
        ProdosAttributes.Builder builder = ProdosAttributes.builder().unlocked();
        if (Objects.requireNonNull(getContentType()) == ContentType.HIGH_ASCII) {
            builder.TXT();
        } else {
            // TODO what is the address? _Where_ is the address? $900 is a guess.
            builder.BIN(0x900);
        }
        return builder.size(getSize()).get();
    }

    /// Return the type of content, if known. It should never return null; use UNKNOWN if not known.
    @Override
    public ContentType getContentType() {
        return switch (getType()) {
            case "P" -> ContentType.BINARY_65C02;
            case "M" -> ContentType.BINARY;
            case "L", " " -> ContentType.HIGH_ASCII;
            default -> ContentType.UNKNOWN;
        };
    }

    /// Retrieve non-standard components.
    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Optional.empty();
    }
}
