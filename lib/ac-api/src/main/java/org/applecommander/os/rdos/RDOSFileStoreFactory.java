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
import org.applecommander.device.BlockDevice;
import org.applecommander.device.SkewedTrackSectorDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.device.TrackSectorToBlockAdapter;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.hint.Hint;
import org.applecommander.util.DataBuffer;

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
                    // These are only DOS or nibble/physical ordered
                    if (!device.is(Hint.NIBBLE_SECTOR_ORDER)) {
                        device = SkewedTrackSectorDevice.dosToPhysicalSkew(device);
                    }
                    // Looking for the marker on block 25 (T1, S12)
                    DataBuffer sectorData = device.readSector(1, 12);
                    if (sectorData.scan(CAT_MARKER, 0).isPresent()) {
                        test(device, RDOSVersionAdapter.RDOS21).ifPresent(ctx.fileStores::add);
                    }
                })
                .check16Sector(device -> {
                    DataBuffer firstTwoTracks = device.readRange(0, 0, 32);
                    firstTwoTracks.scan(CAT_MARKER, 0x7f, 0).ifPresent(pos -> {
                        int sectorNumber = pos >> 8;
                        System.out.println(sectorNumber);
                        // We found the marker. Determine where it was located. That should help determine version + geometry.
                        Optional<RDOSFileStore> fileStore = switch (sectorNumber) {

                            // --- RDOS 3.3 (marker is located on block 1 which is track 0, sector 1) ---
                            // Pascal/ProDOS sector order
                            case 0x01 -> test(device, RDOSVersionAdapter.RDOS33);
                            // Nibble/physical sector order
                            case 0x02 -> test(SkewedTrackSectorDevice.physicalToPascalSkew(device), RDOSVersionAdapter.RDOS33);
                            // DOS sector order
                            case 0x0e -> test(SkewedTrackSectorDevice.dosToPascalSkew(device), RDOSVersionAdapter.RDOS33);

                            // --- RDOS 2.1 (marker is located on block 25 which is track 1 sector 12) ---
                            //    (note that we have a 13-sector device mapped into a 16-sector device)
                            // Pascal/ProDOS sector order
                            case 0x1c -> test(SkewedTrackSectorDevice.truncate16sectorTo13(
                                    SkewedTrackSectorDevice.pascalToPhysicalSkew(device)), RDOSVersionAdapter.RDOS21);
                            // Nibble/physical sector order
                            case 0x16 -> test(SkewedTrackSectorDevice.truncate16sectorTo13(device), RDOSVersionAdapter.RDOS21);
                            // DOS sector order
                            case 0x19 -> test(SkewedTrackSectorDevice.truncate16sectorTo13(
                                    SkewedTrackSectorDevice.dosToPhysicalSkew(device)), RDOSVersionAdapter.RDOS21);
                            default -> Optional.empty();
                        };
                        fileStore.ifPresent(ctx.fileStores::add);
                    });
                });
    }

    public Optional<RDOSFileStore> test(TrackSectorDevice device, RDOSVersionAdapter adapter) {
        BlockDevice blockDevice = new TrackSectorToBlockAdapter(device, TrackSectorToBlockAdapter.BlockStyle.RDOS);
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
}
