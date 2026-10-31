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
package org.applecommander.filestore;

import com.webcodepro.applecommander.storage.DiskFileStoreAdapter;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.Device;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// This unit test runs through all the images, expecting them to be recognized.
/// Then it reads all sectors or blocks, expecting to be able to read them all.
/// Then it reads all file entries and reads both the data and resource fork,
/// expecting to be able to read them all.
public class GeneralFileStoreExerciseTest {
    public static Stream<Path> generatePaths() throws IOException {
        final Path root = Path.of("src/test/resources/");
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).toList().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("generatePaths")
    public void exerciseFileStore(Path path) {
        Source source = Sources.create(path).orElseThrow();
        FileStoreFactory.Context ctx = FileStores.inspect(source);
        assertNotEquals(0, ctx.fileStores.size());
        for (FileStore fileStore : ctx.fileStores) {
            if (fileStore instanceof DiskFileStoreAdapter) {
                System.err.println("Skipping legacy FormattedDisk wrapper.");
                continue;
            }
            // We expect all disks used in unit tests to be "good"
            fileStore.get(Device.class).ifPresent(device -> {
                switch (device) {
                    case TrackSectorDevice trackSectorDevice -> verify(trackSectorDevice);
                    case BlockDevice blockDevice -> verify(blockDevice);
                    default -> throw new RuntimeException();
                }
            });
            // We expect all images to have files that can be read
            for (FileEntry fileEntry : fileStore.getRootDirectory().getFiles()) {
                assertNotEquals(0, fileEntry.getName().length());
                // We need to skip subdirectories
                if (fileEntry.get(Directory.class).isPresent()) continue;
                // We need to skip deleted files
                if (fileEntry.isDeleted()) continue;
                assertNotNull(fileEntry.getDataFork());
                fileEntry.getResourceFork().ifPresent(Assertions::assertNotNull);
            }
        }
    }

    public void verify(TrackSectorDevice device) {
        for (int t=0; t<device.getGeometry().tracksOnDisk(); t++) {
            for (int s=0; s<device.getGeometry().sectorsPerTrack(); s++) {
                device.readSector(t, s);
            }
        }
    }

    public void verify(BlockDevice device) {
        for (int b=0; b<device.getGeometry().blocksOnDevice(); b++) {
            device.readBlock(b);
        }
    }
}
