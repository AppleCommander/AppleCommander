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
package org.applecommander.os.pascal;

import com.webcodepro.applecommander.storage.DiskConstants;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.SectorOrder;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.util.DataBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

public class PascalFileStoreFactory implements FileStoreFactory, PascalConstants {
    public static final List<FileStoreOrganization> VALID_ORGANIZATIONS = List.of(
            FileStoreOrganization.DISK_16SECTOR_DO,
            FileStoreOrganization.DISK_16SECTOR_PO,
            FileStoreOrganization.DISK_16SECTOR_NIB,
            FileStoreOrganization.DISK_800K_PO,
            FileStoreOrganization.DISK_800K_2IMG
    );

    @Override
    public String getName() {
        return "Apple Pascal";
    }

    @Override
    public void inspect(Context ctx) {
        ctx.blockDevice()
                .check16Sector(SectorOrder.PRODOS, device -> {
                    if (check(device)) {
                        ctx.fileStores.add(new PascalFileStore(device));
                    }
                })
                .check800k(device -> {
                    if (check(device)) {
                        ctx.fileStores.add(new PascalFileStore(device));
                    }
                });
    }

    @Override
    public List<FileStoreOrganization> getFileStoreOrganizations() {
        return VALID_ORGANIZATIONS;
    }

    @Override
    public PascalFileStore create(FileStoreOrganization fileStoreOrganization) {
        Objects.requireNonNull(fileStoreOrganization);
        BlockDevice blockDevice = fileStoreOrganization.createBlockDevice();
        try (InputStream inputStream = PascalFileStoreFactory.class.getResourceAsStream("/files/pascal-1.3-boot.bin")) {
            Objects.requireNonNull(inputStream);
            byte[] bootBlocks = inputStream.readAllBytes();
            blockDevice.writeRange(0, DataBuffer.wrap(bootBlocks));
            PascalFileStore fileStore = new PascalFileStore(blockDevice);
            fileStore.getRootDirectory().initVolumeHeader("NEW.DISK");
            return fileStore;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Check for a likely directory structure. Note that we scan all sizes, even though that is overkill. */
    public boolean check(BlockDevice device) {
        boolean good = false;
        if (device.getGeometry().blockSize() >= DiskConstants.PRODOS_BLOCKS_ON_140KB_DISK) {
            // Read entire directory for analysis
            DataBuffer dir = device.readRange(DIRECTORY_FIRST_BLOCK, DIRECTORY_BLOCKS);
            // Check volume entry
            int dFirstBlock = dir.getUnsignedShort(ENTRY_FIRST_BLOCK);
            int dLastBlock = dir.getUnsignedShort(ENTRY_LAST_BLOCK);
            int dEntryType = dir.getUnsignedShort(ENTRY_TYPE);
            int dNameLength = dir.getUnsignedByte(ENTRY_NAME);
            int dBlocksOnDisk = dir.getUnsignedShort(VOLUME_BLOCKS_ON_DISK);
            int dFilesOnDisk = dir.getUnsignedShort(VOLUME_FILES_ON_DISK);
            if (dBlocksOnDisk == 0) dBlocksOnDisk = 280;    // patch for some Pascal disks found
            good = dFirstBlock == 0 && dLastBlock == 6 && dEntryType == 0
                    && dNameLength < 8
                    && dFilesOnDisk < 78
                    && dBlocksOnDisk >= 280;
            // Check (any) existing file entries
            int offset = ENTRY_LENGTH;
            while (good && dFilesOnDisk > 0 && offset < dir.limit()) {
                int fFirstBlock = dir.getUnsignedShort(offset+ENTRY_FIRST_BLOCK);
                int fLastBlock = dir.getUnsignedShort(offset+ENTRY_LAST_BLOCK);
                int fEntryType = dir.getUnsignedShort(offset+ENTRY_TYPE);
                int fNameLength = dir.getUnsignedByte(offset+ENTRY_NAME);
                int fBytesLastBlock = dir.getUnsignedShort(offset+FILE_BYTES_USED_IN_LAST_BLOCK);
                if (fNameLength == 0) break;    // last entry?
                // There appear to be some zero block entries that have the same first and last block
                good = fFirstBlock <= fLastBlock
                        && fLastBlock <= dBlocksOnDisk
                        && (fEntryType & 0x7fff) < 9
                        && fNameLength < 16
                        && fBytesLastBlock <= 512;
                offset += ENTRY_LENGTH;
                dFilesOnDisk--;
            }
        }
        return good;
    }
}
