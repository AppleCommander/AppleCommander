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
import org.applecommander.converter.ConverterRepository;
import org.applecommander.converter.TextConverter;
import org.applecommander.device.*;
import org.applecommander.filestore.ConfigurationOption;
import org.applecommander.filestore.ContentType;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.util.DataBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

import static org.applecommander.filestore.ConfigurationOption.getValidatedValue;

public class RDOSFileStoreFactory implements FileStoreFactory, RDOSConstants {
    /// Simple directory marker to determine sector order
    public static final byte[] CAT_MARKER = "  LEN         -<NAME>-       LENGTH BLK".getBytes();
    public static final Set<Integer> VALID_FILE_TYPES = Set.of(0xc1 /*A*/, 0xc2 /*B*/, 0xd4 /*T*/, 0xd3 /*S*/);
    public static final List<FileStoreOrganization> VALID_ORGANIZATIONS = List.of(
            FileStoreOrganization.DISK_16SECTOR_DO,
            FileStoreOrganization.DISK_16SECTOR_PO,
            FileStoreOrganization.DISK_16SECTOR_NIB
    );
    public static final String STARTUP_FILE_NAME_FIELD = "startup_file_name";
    public static final String STARTUP_FILE_NAME = "SYSTEMBOOT";
    public static final String STARTUP_CONTENT_FIELD = "startup_content";
    public static final String STARTUP_CONTENT_DEFAULT = """
                    PR#0
                    IN#0
                    TEXT
                    HOME
                    &CAT
                    PRINT
                    PRINT "WELCOME TO RDOS!"
                    """;

    @Override
    public String getName() {
        return "RDOS v3.3";
    }

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

    @Override
    public List<FileStoreOrganization> getFileStoreOrganizations() {
        return VALID_ORGANIZATIONS;
    }

    @Override
    public Map<String,ConfigurationOption> getConfigurationOptions() {
        return ConfigurationOption.builder()
                .next(STARTUP_FILE_NAME_FIELD)
                .restrictions(FILE_NAME_MAPPER)
                .enforceLength(1, NAME_LENGTH)
                .prompt("Startup File Name")
                .notes("""
                        RDOS allows customization of the startup file. It doesn't appear that
                        the type of file can be customized (always a text file) but the name
                        doesn't have to be 'SYSTEMBOOT'.
                        """)
                .defaultValue(STARTUP_FILE_NAME)
                .add()
                .next(STARTUP_CONTENT_FIELD)
                .enforceLength(1, 200)
                .prompt("Startup Contents")
                .notes("""
                        Please enter the contents of the startup file. It appears that
                        RDOS must have PR#0 and IN#0 at the beginning. Multiple lines
                        are joined by a colon so the startup is a single line long.
                        """)
                .defaultValue(STARTUP_CONTENT_DEFAULT)
                .add()
                .done();
    }

    @Override
    public RDOSFileStore create(FileStoreOrganization fileStoreOrganization, Map<String,ConfigurationOption> options) {
        Objects.requireNonNull(fileStoreOrganization);
        TrackSectorDevice device = fileStoreOrganization.createTrackSectorDevice();
        BlockDevice blockDevice = new TrackSectorToBlockAdapter(device.request(SectorOrder.PRODOS),
                TrackSectorToBlockAdapter.BlockStyle.RDOS);
        // The BIN file is in RDOS block order, so we can copy the blocks directly into the device!
        try (InputStream inputStream = RDOSFileStore.class.getResourceAsStream("/files/rdos33-template.bin")) {
            Objects.requireNonNull(inputStream);
            byte[] disk = inputStream.readAllBytes();
            blockDevice.writeRange(0, DataBuffer.wrap(disk));
            RDOSFileStore fileStore = new RDOSFileStore(blockDevice, RDOSVersionAdapter.RDOS33);
            // Create the "SYSTEMBOOT" file (or whatever name was chosen).
            String startupName = getValidatedValue(options, STARTUP_FILE_NAME_FIELD, STARTUP_FILE_NAME);
            RDOSFileEntry startup = fileStore.getRootDirectory().createFile(startupName);
            startup.setType("T");
            startup.setAddress(0xb100);
            String content = getValidatedValue(options, STARTUP_CONTENT_FIELD, STARTUP_CONTENT_DEFAULT);
            content = String.join(":", content.split("[\r\n]")) + "\r";
            TextConverter textConverter = ConverterRepository.getTextConverter(ContentType.HIGH_ASCII).orElseThrow();
            textConverter.write(startup, content);
            // Need to copy the entry to 0xF60 (track 0, sector F, offset 0x60)
            DataBuffer block = blockDevice.readBlock(15);
            block.put(0x60, startup.readEntry());
            blockDevice.writeBlock(15, block);
            return fileStore;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
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
                // Originally was looking for high-bit space, but some games really put in funky characters. (shrug)
                good = entry.getUnsignedByte(j) > 0;
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
