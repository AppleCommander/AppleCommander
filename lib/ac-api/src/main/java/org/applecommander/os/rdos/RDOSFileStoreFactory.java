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
package org.applecommander.os.rdos;

import com.webcodepro.applecommander.storage.DiskConstants;
import org.applecommander.device.*;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.util.DataBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class RDOSFileStoreFactory implements FileStoreFactory, RDOSConstants {
    /// Simple directory marker to determine sector order
    public static final byte[] CAT_MARKER = "  LEN         -<NAME>-       LENGTH BLK".getBytes();
    public final static Set<Integer> VALID_FILE_TYPES = Set.of(0xc1 /*A*/, 0xc2 /*B*/, 0xd4 /*T*/, 0xd3 /*S*/);

    @Override
    public void inspect(Context ctx) {
        ctx.trackSectorDevice()
                .check13Sector(device -> {
                    // These are nibble/physical ordered
                    test(device.request(SectorOrder.NIBBLE), RDOSVersionAdapter.RDOS21).ifPresent(ctx.fileStores::add);
                })
                .check16Sector(device -> {
                    test(device.request(SectorOrder.PRODOS), RDOSVersionAdapter.RDOS33).ifPresent(ctx.fileStores::add);
                    test(SkewedTrackSectorDevice.truncate16sectorTo13(device.request(SectorOrder.NIBBLE)),
                            RDOSVersionAdapter.RDOS21).ifPresent(ctx.fileStores::add);
                });
    }

    public Optional<RDOSFileStore> test(TrackSectorDevice device, RDOSVersionAdapter adapter) {
        BlockDevice blockDevice = new TrackSectorToBlockAdapter(device, TrackSectorToBlockAdapter.BlockStyle.RDOS);
        if (blockDevice.readBlock(adapter.catalogCodeBlock).scan(CAT_MARKER, 0x7f, 0).isEmpty()) {
            return Optional.empty();
        }
        for (int block = adapter.firstDirectoryBlock; block < adapter.lastDirectoryBlock; block++) {
            DataBuffer blockData = blockDevice.readBlock(block);
            if (!testCatalogSector(blockData, blockDevice.getGeometry().blocksOnDevice())) {
                return Optional.empty();
            }
        }
        return Optional.of(new RDOSFileStore(blockDevice, adapter));
    }

    /// Test a single catalog sector.
    ///
    /// | Offset      | Description |
    /// | :---        | :--- |
    /// | `$00`-`$17` | File name; space-filled.  If the first byte is $00, that is the end of the directory. If the first byte is $80, the file is deleted. |
    /// | `$18`       | File type. Valid types are 'A', 'B', 'T', 'S'. |
    /// | `$19`       | File length in blocks (block = sector = 256 bytes). |
    /// | `$1A`-`$1B` | Address of application.  For Applesoft and binary; others may vary. |
    /// | `$1C`-`$1D` | Length in bytes of file. (Text files are multiple of 256.) |
    /// | `$1E`-`$1F` | Starting block of application. |
    public boolean testCatalogSector(DataBuffer data, final int maxBlocks) {
        boolean good = true;
        boolean atEnd = false;
        data.position(0);
        while (data.hasRemaining()) {
            DataBuffer entry = data.readBuffer(ENTRY_LENGTH);
            int check = entry.getUnsignedByte(NAME_OFFSET);
            // Once we reach the last file, ensure the rest of the directory is all 0's
            if (atEnd && check != 0) return false;
            atEnd = check == 0;             // at end of directory
            if (atEnd) continue;
            if (check == 0x80) continue;    // deleted
            // Check this is all valid high-bit ASCII
            for (int j=0; j<NAME_LENGTH; j++) {
                good = entry.getUnsignedByte(j) >= 0xa0;
                if (!good) return good;
            }
            int fileType = entry.getUnsignedByte(TYPE_OFFSET);
            int lengthInBlocks = entry.getUnsignedByte(BLOCKS_OFFSET);
            int lengthInBytes = entry.getUnsignedShort(LENGTH_OFFSET);
            int firstBlock = entry.getUnsignedShort(FIRST_BLOCK_OFFSET);
            good = VALID_FILE_TYPES.contains(fileType)
                    && lengthInBytes <= lengthInBlocks * DiskConstants.SECTOR_SIZE
                    && firstBlock < maxBlocks;
            if (!good) return good;
        }
        return good;
    }

    public static RDOSFileStore create(TrackSectorDevice device) {
        Objects.requireNonNull(device);
        if (device.getGeometry().sectorsPerDisk() != 560) {
            throw new RuntimeException("RDOS requires a 560 sector disk.");
        }
        device.format();
        BlockDevice blockDevice = new TrackSectorToBlockAdapter(device.request(SectorOrder.PRODOS),
                                                                TrackSectorToBlockAdapter.BlockStyle.RDOS);
        // The BIN file is in block order, so we can copy the blocks into the device!
        try (InputStream inputStream = RDOSFileStore.class.getResourceAsStream("/files/rdos33-template.bin")) {
            Objects.requireNonNull(inputStream);
            byte[] disk = inputStream.readAllBytes();
            blockDevice.writeRange(0, DataBuffer.wrap(disk));
            RDOSFileStore fileStore = new RDOSFileStore(blockDevice, RDOSVersionAdapter.RDOS33);
            RDOSFileEntry systemboot = fileStore.getRootDirectory().createFile("SYSTEMBOOT");
            systemboot.setType("T");
            systemboot.setAddress(0xb100);
            // FIXME: Text file has to have high bit set.
            byte[] systembootContent = """
                    PR#0:IN#0:TEXT:HOME:&CAT:PRINT:PRINT "WELCOME TO RDOS!"
                    """.getBytes();
            for (int i = 0; i < systembootContent.length; i++) {
                if (systembootContent[i] == '\n') {
                    systembootContent[i] = '\r';
                }
                systembootContent[i] = (byte)(systembootContent[i] | 0x80);
            }
            systemboot.setDataFork(DataBuffer.wrap(systembootContent));
            return fileStore;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
