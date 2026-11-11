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
package org.applecommander.os.usdos;

import org.applecommander.device.Coordinate;
import org.applecommander.device.Coordinate.TrackAndSectorCoordinate;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.WritableFileEntry;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Consumer;

public class USDOSFileEntry implements WritableFileEntry, USDOSConstants {
    private final USDOSDirectory parent;
    private final TrackSectorDevice device;
    private final int entryOffset;
    private final USDOSDirectory directory;

    USDOSFileEntry(USDOSDirectory parent, int entryOffset) {
        this.parent = parent;
        this.entryOffset = entryOffset;
        this.device = parent.getFileStore().get(TrackSectorDevice.class).orElseThrow();

        DataBuffer entry = readEntry();
        if (entry.getUnsignedByte(ENTRY_EXT) == ENTRY_DIR_VALUE) {
            int directoryNumber = entry.getUnsignedByte(ENTRY_EXT+1);
            this.directory = new USDOSDirectory(parent.getFileStore(), directoryNumber, getName(), parent);
        }
        else {
            this.directory = null;
        }
    }

    public DataBuffer readEntry() {
        return getFileStore().readDirectory().slice(entryOffset, ENTRY_LENGTH);
    }
    public void modifyEntry(Consumer<DataBuffer> consumer) {
        DataBuffer directory = getFileStore().readDirectory();
        DataBuffer entryData = directory.slice(entryOffset, ENTRY_LENGTH);
        consumer.accept(entryData);
        getFileStore().writeDirectory(directory);
        getFileStore().notifyChangeListeners(FileStore.ChangeType.ATTRIBUTES_UPDATED);
    }

    /// The `Directory` that holds this file.
    @Override
    public USDOSDirectory getParent() {
        return parent;
    }

    /// Indicates if this `FileEntry` is deleted.
    @Override
    public boolean isDeleted() {
        return readEntry().getUnsignedByte(ENTRY_NAME) == 0;
    }

    /// Returns the `FileStore` that this `Entry` originates from.
    @Override
    public USDOSFileStore getFileStore() {
        return parent.getFileStore();
    }

    /// Returns the name of this entry: either a directory name or a file name.
    /// This does not contain any directory components -- strictly the name of this entry.
    /// Note that depending on context, this may be a computed field.
    @Override
    public String getName() {
        return readEntry().getFixedLengthString(ENTRY_NAME, ENTRY_NAME_LENGTH);
    }
    public void setName(String name) {
        modifyEntry(entry -> entry.putFixedLengthString(ENTRY_NAME, ENTRY_NAME_LENGTH, name, false));
    }

    public String getExtension() {
        DataBuffer entry = readEntry();
        if (entry.getUnsignedByte(ENTRY_EXT) == ENTRY_DIR_VALUE) {
            return "<DIR>";
        }
        return entry.getFixedLengthString(ENTRY_EXT, ENTRY_EXT_LENGTH);
    }
    public void setExtension(String extension) {
        modifyEntry(entry -> entry.putFixedLengthString(ENTRY_EXT, ENTRY_EXT_LENGTH, extension, false));
    }
    public void setExtensionAsDirectory(int directoryNumber) {
        modifyEntry(entry -> {
            entry.putBytes(ENTRY_EXT, ENTRY_DIR_VALUE, directoryNumber, 0);
        });
    }

    /// Returns the size, in bytes, of this item.
    /// It may be approximate (based off a sector count, for instance).
    @Override
    public long getSize() {
        return readEntry().getUnsignedShort(ENTRY_SIZE);
    }
    public void setSize(int size) {
        modifyEntry(entry -> entry.putUnsignedShort(ENTRY_SIZE, size));
    }

    /// Convert custom date format.
    /// Bytes are (essentially) big-endian format `dddddmmm myyyyyyy`.
    /// Year = 1977 + `yyyyyyy`.
    /// Month is zero based.
    /// Day is zero based.
    public LocalDate getDate() {
        int value = readEntry().getUnsignedShortBE(ENTRY_DATE);
        int year = 1977 + (value & 0x7f);
        int month = (value >> 7) & 0x0f;
        int dayOfMonth = (value >> 11) & 0x1f;
        return LocalDate.of(year, month+1, dayOfMonth+1);
    }
    public void setDate(LocalDate date) {
        int dayOfMonth = date.getDayOfMonth() - 1;
        int month = date.getMonthValue() - 1;
        int year = date.getYear() - 1977;
        int dateValue = (dayOfMonth << 11) | (month << 7) | year;
        modifyEntry(entry -> entry.putUnsignedShortBE(ENTRY_DATE, dateValue));
    }

    public int getAddress() {
        return readEntry().getUnsignedShort(ENTRY_ADDRESS);
    }
    public void setAddress(int address) {
        modifyEntry(entry -> entry.putUnsignedShort(ENTRY_ADDRESS, address));
    }

    public TrackAndSectorCoordinate getTrackSectorIndex() {
        DataBuffer entry = readEntry();
        return Coordinate.trackAndSector(entry.getUnsignedByte(ENTRY_MAP_TRACK), entry.getUnsignedByte(ENTRY_MAP_SECTOR));
    }
    public void setTrackSectorIndex(int track, int sector) {
        modifyEntry(entry -> {
            entry.putByte(ENTRY_MAP_TRACK, track);
            entry.putByte(ENTRY_MAP_SECTOR, sector);
        });
    }

    public int getDirectoryNumber() {
        return readEntry().getUnsignedByte(ENTRY_DIR_NUM);
    }
    public void setDirectoryNumber(int directoryNumber) {
        modifyEntry(entry -> entry.putByte(ENTRY_DIR_NUM, directoryNumber));
    }

    /// Return the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public DataBuffer getDataFork() {
        TrackAndSectorCoordinate coordinate = getTrackSectorIndex();
        int size = (int)getSize();
        int totalSectors = device.calculateRequiredSectors(size);
        DataBuffer index = device.readSector(coordinate.track(), coordinate.sector());
        DataBuffer data = DataBuffer.create(totalSectors * TrackSectorDevice.SECTOR_SIZE);
        int offset = 0;
        for (int i=0; i<totalSectors; i++) {
            int track = index.getUnsignedByte(INDEX_TRACKS+i);
            if (track == 0xff) break;
            int sectorOffset = i >> 1;
            int sectorByte = index.getUnsignedByte(INDEX_SECTORS+sectorOffset);
            int sector = ((i % 2 == 0) ? sectorByte : (sectorByte >> 4)) & 0xf;
            device.readSector(track, sector, offset, data);
            offset += TrackSectorDevice.SECTOR_SIZE;
        }
        return data.slice(0, size);
    }

    /// Store the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public void setDataFork(DataBuffer data) {
        getFileStore().notifyChangeListeners(FileStore.ChangeType.DATA_UPDATED);
        throw new UnsupportedOperationException("TODO");
    }

    /// Interpret the `FileEntry` as if it were on a ProDOS volume.
    /// The intention is that ProDOS ended up being the common denominator in many tools and formats,
    /// so this allows each file store to manage the interpretation itself.
    @Override
    public ProdosAttributes getProdosAttributes() {
        ProdosAttributes.Builder builder = ProdosAttributes.builder()
            .unlocked()
            .auxType(getAddress())
            .creation(getDate())
            .name(String.format("%s.%s", getName(), getExtension()));
        switch (getExtension()) {
            case "BAS" -> builder.BAS();
            case "TXT", "BAT" -> builder.TXT();
            default -> builder.BIN(getAddress());
        }
        return builder.get();
    }

    /// Return the type of content, if known. It should never return null; use UNKNOWN if not known.
    ///
    /// @see ContentType
    @Override
    public ContentType getContentType() {
        return switch (getExtension()) {
            case "TXT", "BAT" -> ContentType.LOW_ASCII;
            default -> ContentType.UNKNOWN;
        };
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, directory);
    }
}
