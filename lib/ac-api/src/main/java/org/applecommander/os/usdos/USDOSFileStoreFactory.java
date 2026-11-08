package org.applecommander.os.usdos;

import org.applecommander.device.SectorOrder;
import org.applecommander.filestore.ConfigurationOption;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.util.DataBuffer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class USDOSFileStoreFactory implements FileStoreFactory, USDOSConstants {

    /// The name of this factory. Usually the name and file extensions.
    @Override
    public String getName() {
        return "US-DOS";
    }

    /// Inspect the given `Source`, storing any results or messages in the context.
    @Override
    public void inspect(Context ctx) {
        ctx.trackSectorDevice().check16Sector(device -> {
            device = device.request(SectorOrder.DOS);
            // Boot sector marker
            DataBuffer sector2 = device.readSector(0, 1);
            if (sector2.scan(BOOT_PROMPT, 0).isEmpty()) {
                return;
            }
            // Check the structure of the directory. If there are no entries, we end up relying on the boot sector.
            USDOSFileStore fileStore = new USDOSFileStore(device);
            DataBuffer directory = fileStore.readDirectory();
            directory.position(0);
            while (directory.hasRemaining()) {
                DataBuffer entryData = directory.readBuffer(ENTRY_LENGTH);
                // Offset +0 can be $00 if the file is deleted. Checking the 2nd byte instead.
                if (entryData.getUnsignedByte(ENTRY_NAME+1) == 0) {
                    break;  // end of directory
                }
                for (int i=0; i<ENTRY_NAME_LENGTH; i++) {
                    int ch = entryData.getUnsignedByte(ENTRY_NAME+i);
                    if (i ==0 && ch == 0) {
                        continue;   // Might be a deleted file
                    }
                    if (ch < 0x20 || ch >= 0x80) return;
                }
                for (int i=0; i<ENTRY_EXT_LENGTH; i++) {
                    int ch = entryData.getUnsignedByte(ENTRY_EXT+i);
                    if (ch == ENTRY_DIR_VALUE) break;   // this is a directory and a different format
                    if (ch < 0x20 || ch >= 0x80) return;
                }
                if (entryData.getUnsignedByte(ENTRY_MAP_TRACK) >= device.getGeometry().tracksOnDisk() ||
                        entryData.getUnsignedByte(ENTRY_MAP_SECTOR) >= device.getGeometry().sectorsPerTrack()) {
                    return;
                }
            }
            // Seems to be US-DOS!
            ctx.fileStores.add(fileStore);
        });
    }

    /// The supported organizations for `FileStore` creation. Note that this isn't particularly useful for archives,
    /// so there is a generic "archive" organization. For disk images, this will typically orient around the
    /// physical device including sector ordering, if applicable.
    @Override
    public List<FileStoreOrganization> getFileStoreOrganizations() {
        return List.of(FileStoreOrganization.DISK_16SECTOR_DO, FileStoreOrganization.DISK_16SECTOR_NIB,
                FileStoreOrganization.DISK_16SECTOR_PO);
    }

    /// The configuration options for `FileStore` creation. The key of the map is the id and is intended to be
    /// utilized in the CLI environment to locate and update a field.
    @Override
    public Map<String,ConfigurationOption> getConfigurationOptions() {
        return Collections.emptyMap();
    }

    /// Create a `FileStore` using the given configuration settings.
    @Override
    public USDOSFileStore create(FileStoreOrganization fileStoreOrganization, Map<String, ConfigurationOption> options) {
        // FIXME TODO
        throw new UnsupportedOperationException("TODO!");
    }
}
