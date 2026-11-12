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

import org.applecommander.applesingle.AppleSingle;
import org.applecommander.converter.ConverterRepository;
import org.applecommander.converter.TextConverter;
import org.applecommander.device.SectorOrder;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.ConfigurationOption;
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.DataBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.applecommander.filestore.ConfigurationOption.getFlagValue;

public class USDOSFileStoreFactory implements FileStoreFactory, USDOSConstants {
    public static final String COMMAND_COM_FLAG = "include_command_com";
    public static final String HELP_BAT_FLAG = "include_help_bat";

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
                if (entryData.getUnsignedByte(ENTRY_BASE_NAME +1) == 0) {
                    break;  // end of directory
                }
                for (int i = 0; i< ENTRY_BASE_NAME_LENGTH; i++) {
                    int ch = entryData.getUnsignedByte(ENTRY_BASE_NAME +i);
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
        return ConfigurationOption.builder()
                .flagValue(COMMAND_COM_FLAG).prompt("Include 'COMMAND.COM'?").defaultValue(true).add()
                .flagValue(HELP_BAT_FLAG).prompt("Include 'HELP.BAT'? (Engish translation)").defaultValue(false).add()
                .done();
    }

    /// Create a `USDOSFileStore` using the given configuration settings.
    @Override
    public USDOSFileStore create(FileStoreOrganization fileStoreOrganization, Map<String, ConfigurationOption> options) {
        Objects.requireNonNull(fileStoreOrganization);
        TrackSectorDevice device = fileStoreOrganization.createTrackSectorDevice().request(SectorOrder.DOS);
        try (
            InputStream inputStream = USDOSFileStoreFactory.class.getResourceAsStream("/files/usdos-1.11-boot.bin");
            InputStream commandCom = USDOSFileStoreFactory.class.getResourceAsStream("/files/COMMAND.COM.as");
            InputStream helpBat = USDOSFileStoreFactory.class.getResourceAsStream("/files/HELP.BAT");
        ) {
            Objects.requireNonNull(inputStream);
            Objects.requireNonNull(commandCom);
            Objects.requireNonNull(helpBat);
            byte[] boot = inputStream.readAllBytes();
            device.writeRange(0, 0, DataBuffer.wrap(boot));
            DataBuffer bitmap = device.readSector(BITMAP_TRACK, BITMAP_SECTOR);
            // There isn't any other geometry, so US-DOS assumes 40 tracks. This is a 35 track disk.
            for (int track : List.of(0, 35, 36, 37, 38, 39)) {
                bitmap.putBytes(BITMAP_OFFSET + track*2, 0xff, 0xff);
            }
            device.writeSector(BITMAP_TRACK, BITMAP_SECTOR, bitmap);
            USDOSFileStore fileStore = new USDOSFileStore(device);
            USDOSDirectory rootDirectory = fileStore.getRootDirectory();
            if (getFlagValue(options, COMMAND_COM_FLAG)) {
                AppleSingle appleSingle = AppleSingle.read(commandCom);
                ProdosAttributes prodosAttributes = ProdosAttributes.from(appleSingle);
                rootDirectory.createFrom(prodosAttributes);
            }
            if (getFlagValue(options, HELP_BAT_FLAG)) {
                USDOSFileEntry file = rootDirectory.createFile("HELP.BAT");
                TextConverter converter = ConverterRepository.getTextConverter(file.getContentType()).orElseThrow();
                converter.write(file, new String(helpBat.readAllBytes()));
            }
            return fileStore;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
