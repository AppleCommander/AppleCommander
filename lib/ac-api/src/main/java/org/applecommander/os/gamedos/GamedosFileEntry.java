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

import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.ProdosAttributes;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

public class GamedosFileEntry implements FileEntry, GamedosConstants {
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
        return device.readSector(GamedosDirectory.DIRECTORY_TRACK, sector).slice(offset, ENTRY_SIZE);
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
    public long getSize() {
        return (long) getSectorCount() * TrackSectorDevice.SECTOR_SIZE;
    }

    public String getFiletype() {
        return readEntry().getFixedLengthString(ENTRY_TYPE_OFFSET,1);
    }
    public int getFirstTrack() {
        return readEntry().getUnsignedByte(ENTRY_TRACK_OFFSET);
    }
    public int getFirstSector() {
        return readEntry().getUnsignedByte(ENTRY_SECTOR_OFFSET);
    }
    public int getMeta() {
        return readEntry().getUnsignedShort(ENTRY_META_OFFSET);
    }
    public int getSectorCount() {
        return readEntry().getUnsignedByte(ENTRY_SECTORS_OFFSET);
    }

    @Override
    public DataBuffer getDataFork() {
        return device.readRange(getFirstTrack(), getFirstSector(), getSectorCount());
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
