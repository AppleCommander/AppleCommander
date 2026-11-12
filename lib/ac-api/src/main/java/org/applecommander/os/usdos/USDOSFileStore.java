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

import org.applecommander.capability.Capability;
import org.applecommander.change.ChangeRegistry;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.FileStore;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.usage.SectorAllocator;
import org.applecommander.usage.SectorBitmapUsage;
import org.applecommander.usage.UsageType;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.InformationGroup;

import java.util.List;
import java.util.Optional;

public class USDOSFileStore extends ChangeRegistry<FileStore.FileStoreChangeEvent> implements FileStore, USDOSConstants {
    private final TrackSectorDevice device;
    private final USDOSDirectory rootDirectory;
    private final SectorBitmapUsage usage;
    private final SectorAllocator allocator;
    private DataBuffer bitmap;

    USDOSFileStore(TrackSectorDevice device) {
        this.device = device;
        this.rootDirectory = new USDOSDirectory(this, 0, "<ROOT>");
        this.usage = new SectorBitmapUsage(device.getGeometry(), this::getUsage);
        this.allocator = new SectorAllocator(this::getUsage, this::setUsed, device.getGeometry());
        readBitmap();
    }

    /// Read in a fresh copy of the sector bitmap.
    public void readBitmap() {
        bitmap = device.readSector(BITMAP_TRACK, BITMAP_SECTOR);
    }
    /// Write out the current copy of the sector bitmap.
    public void writeBitmap() {
        device.writeSector(BITMAP_TRACK, BITMAP_SECTOR, bitmap);
    }
    /// Get the usage type for the given sector.
    public UsageType getUsage(int track, int sector) {
        if (track == 0) {
            if (sector < 2) {
                return UsageType.BOOT;
            }
            else if (sector <= DIRECTORY_FIRST_SECTOR) {
                return UsageType.DIRECTORY;
            }
            else {
                return UsageType.BITMAP;
            }
        }
        int offset = BITMAP_OFFSET + (track * 2) + (sector < 8 ? 1 : 0);
        int bit = 7 - (sector & 0x7);
        boolean used = bitmap.isBitSet(offset, bit);
        return used ? UsageType.FILE : UsageType.FREE;
    }
    /// Set the used status for a sector in the sector usage bitmap.
    public void setUsed(int track, int sector, boolean used) {
        int offset = BITMAP_OFFSET + (track * 2) + (sector < 8 ? 1 : 0);
        int bit = 7 - (sector & 0x7);
        bitmap.putBit(offset, bit, used);
    }

    void notifyChangeListeners(ChangeType changeType) {
        switch (changeType) {
            case DATA_UPDATED, FILE_CREATED, FILE_DELETED -> readBitmap();
        }
        notifyChangeListeners(new FileStoreChangeEvent(this, changeType));
    }

    /// Read the entirety of the directory.
    public DataBuffer readDirectory() {
        DataBuffer directory = DataBuffer.create(DIRECTORY_TOTAL_SECTORS * TrackSectorDevice.SECTOR_SIZE);
        int count = DIRECTORY_TOTAL_SECTORS;
        int sector = DIRECTORY_FIRST_SECTOR;
        int offset = 0;
        while (count > 0) {
            device.readSector(DIRECTORY_TRACK, sector, offset, directory);
            count--;
            sector--;
            offset += TrackSectorDevice.SECTOR_SIZE;
        }
        return directory;
    }

    /// Write the entirety of the directory.
    public void writeDirectory(DataBuffer directory) {
        int count = DIRECTORY_TOTAL_SECTORS;
        int sector = DIRECTORY_FIRST_SECTOR;
        int offset = 0;
        while (count > 0) {
            device.writeSector(DIRECTORY_TRACK, sector, directory.slice(offset, TrackSectorDevice.SECTOR_SIZE));
            count--;
            sector--;
            offset += TrackSectorDevice.SECTOR_SIZE;
        }
    }

    /// Get the type of file store. This is a generic name, such as "RDOS 2.1" or "ShrinkIt".
    @Override
    public String getType() {
        return "US-DOS";
    }

    /// The label is a mechanism to distinguish multiple file stores in a single file.
    /// For instance, UniDOS has two file stores in the file, so it would have two
    /// different labels such as "Disk 1" and "Disk 2".
    @Override
    public String getLabel() {
        return "US-DOS";
    }

    /// All FileStores support a "root" directory that references all files.
    @Override
    public USDOSDirectory getRootDirectory() {
        return rootDirectory;
    }

    /// This is the path separator character.
    @Override
    public String getPathSeparator() {
        return "\\";
    }

    /// Yields a list of columns to be used in this display mode.
    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(USDOSFileEntry.class)
            .left("Name").string(USDOSFileEntry::getName).done()
            .center("Ext").string(USDOSFileEntry::getExtension).done()
            .right("Size").integer(USDOSFileEntry::getSize, "%,d").done()
            .center("Date").dateDDMONYYYY(USDOSFileEntry::getDate).done()
            .right("Address").integer(USDOSFileEntry::getAddress, "$%04X").mode(DisplayMode.DETAIL).done()
            .center("Index").object(USDOSFileEntry::getTrackSectorIndex).mode(DisplayMode.DETAIL).done()
            .toList();
    }

    /// Helps identify capabilities of the existing component.
    @Override
    public boolean can(Capability capability) {
        return CAPABILITIES.contains(capability);
    }

    /// Retrieve non-standard components.
    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device, usage, allocator);
    }

    /// Get information about this component.
    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("US-DOS")
            .item("Total Files").value(rootDirectory.getTotalFileCount())
            .merge(usage)
            .get(device);
    }
}
