package org.applecommander.os.usdos;

import org.applecommander.capability.Capability;
import org.applecommander.change.ChangeRegistry;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.FileStore;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.usage.BitmapSectorUsage;
import org.applecommander.usage.DiskUsage.UsageType;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.InformationGroup;

import java.util.List;
import java.util.Optional;

public class USDOSFileStore extends ChangeRegistry<FileStore.FileStoreChangeEvent> implements FileStore, USDOSConstants {
    private final TrackSectorDevice device;
    private final USDOSDirectory rootDirectory;
    private final BitmapSectorUsage usage;

    USDOSFileStore(TrackSectorDevice device) {
        this.device = device;
        this.rootDirectory = new USDOSDirectory(this, 0, "<ROOT>");
        this.usage = new BitmapSectorUsage(device.getGeometry(), this::getUsage);
    }

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
        DataBuffer bitmap = device.readSector(BITMAP_TRACK, BITMAP_SECTOR);
        int offset = BITMAP_OFFSET + (track * 2) + (sector < 8 ? 1 : 0);
        int bit = 7 - (sector & 0x7);
        boolean used = bitmap.isBitSet(offset, bit);
        return used ? UsageType.FILE : UsageType.FREE;
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


    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device, usage);
    }

    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("US-DOS")
            .item("Total Files").value(rootDirectory.getTotalFileCount())
            .merge(usage)
            .get(device);
    }
}
