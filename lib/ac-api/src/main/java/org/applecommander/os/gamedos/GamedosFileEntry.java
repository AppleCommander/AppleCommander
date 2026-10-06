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
package org.applecommander.os.gamedos;

import org.applecommander.device.Coordinate.TrackAndSectorCoordinate;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.exception.DiskFullException;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.ProdosAttributes;
import org.applecommander.filestore.WritableFileEntry;
import org.applecommander.os.SequentialAllocator;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;
import java.util.function.Consumer;

public class GamedosFileEntry implements WritableFileEntry, GamedosConstants {
    private final GamedosDirectory directory;
    private final TrackSectorDevice device;
    private final int sector;
    private final int offset;

    public GamedosFileEntry(GamedosDirectory directory, int sector, int offset) {
        this.directory = directory;
        this.sector = sector;
        this.offset = offset;
        this.device = directory.getFileStore().get(TrackSectorDevice.class).orElseThrow();
    }

    public DataBuffer readEntry() {
        return device.readSector(DIRECTORY_TRACK, sector).slice(offset, ENTRY_SIZE);
    }
    public void modifyEntry(Consumer<DataBuffer> consumer) {
        DataBuffer data = device.readSector(GamedosDirectory.DIRECTORY_TRACK, sector);
        consumer.accept(data);
        device.writeSector(DIRECTORY_TRACK, sector, data);
    }

    @Override
    public Directory getParent() {
        return directory;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public FileStore getFileStore() {
        return directory.getFileStore();
    }

    @Override
    public String getName() {
        return readEntry().getFixedLengthString(ENTRY_NAME_OFFSET, ENTRY_NAME_LENGTH).trim();
    }
    @Override
    public void setName(String name) {
        modifyEntry(data -> data.putFixedLengthString(ENTRY_NAME_OFFSET, ENTRY_NAME_LENGTH, name));
    }

    @Override
    public long getSize() {
        return (long) getSectorCount() * TrackSectorDevice.SECTOR_SIZE;
    }

    public String getFiletype() {
        return readEntry().getFixedLengthString(ENTRY_TYPE_OFFSET,1);
    }
    public void setFiletype(String filetype) {
        modifyEntry(data -> data.putFixedLengthString(ENTRY_TYPE_OFFSET, 1, filetype));
    }

    public int getFirstTrack() {
        return readEntry().getUnsignedByte(ENTRY_TRACK_OFFSET);
    }
    public void setFirstTrack(int track) {
        modifyEntry(data -> data.putByte(ENTRY_TRACK_OFFSET, track));
    }

    public int getFirstSector() {
        return readEntry().getUnsignedByte(ENTRY_SECTOR_OFFSET);
    }
    public void setFirstSector(int sector) {
        modifyEntry(data -> data.putByte(ENTRY_SECTOR_OFFSET, sector));
    }

    public int getMeta() {
        return readEntry().getUnsignedShort(ENTRY_META_OFFSET);
    }
    public void setMeta(int meta) {
        modifyEntry(data -> data.putShort(ENTRY_META_OFFSET, (short)meta));
    }

    public int getSectorCount() {
        return readEntry().getUnsignedByte(ENTRY_SECTORS_OFFSET);
    }
    public void setSectorCount(int sectorCount) {
        modifyEntry(data -> data.putByte(ENTRY_SECTORS_OFFSET, sectorCount));
    }

    @Override
    public DataBuffer getDataFork() {
        return device.readRange(getFirstTrack(), getFirstSector(), getSectorCount());
    }

    @Override
    public void setDataFork(DataBuffer data) {
        final int requiredSectors = device.calculateRequiredSectors(data.limit());
        TrackSectorDevice.Geometry geometry = device.getGeometry();
        SequentialAllocator allocator = new SequentialAllocator(geometry.sectorsPerDisk());
        allocator.addUsedByLength(0, geometry.sectorsPerTrack());   // Track 0
        getParent().getFiles().forEach(file -> {
            GamedosFileEntry entry = (GamedosFileEntry) file;
            if (entry.sector != this.sector || entry.offset != this.offset) {
                int sectorOffset = geometry.calculateSectorOffset(entry.getFirstTrack(), entry.getFirstSector());
                allocator.addUsedByLength(sectorOffset, entry.getSectorCount());
            }
        });
        int sectorOffset = allocator.find(requiredSectors);
        if (sectorOffset != -1) {
            TrackAndSectorCoordinate coordinate = geometry.sectorOffsetToCoordinate(sectorOffset);
            device.writeRange(coordinate.track(), coordinate.sector(), data);
            setFirstTrack(coordinate.track());
            setFirstSector(coordinate.sector());
            setSectorCount(requiredSectors);
        }
        else {
            throw new DiskFullException("Unable to allocate %d sectors on disk", requiredSectors);
        }
    }

    @Override
    public ProdosAttributes getProdosAttributes() {
        ProdosAttributes.Builder builder = ProdosAttributes.builder()
                .name(getName())
                .unlocked()
                .size(getSize());
        switch (getFiletype()) {
            case "A" -> builder.BAS();
            case "B" -> builder.BIN(getMeta());
            case "T" -> builder.TXT();
            case "S" -> builder.BIN(0x0600);
            case "P" -> builder.BIN(0x2000);
            case "I" -> builder.INT();
        }
        return builder.build();
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device);
    }
}
