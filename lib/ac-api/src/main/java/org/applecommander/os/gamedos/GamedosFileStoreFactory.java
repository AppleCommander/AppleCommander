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
import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.hint.Hint;
import org.applecommander.util.DataBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;

public class GamedosFileStoreFactory implements FileStoreFactory,GamedosConstants {
    @Override
    public void inspect(Context ctx) {
        ctx.trackSectorDevice()
                .include16Sector(Hint.DOS_SECTOR_ORDER)
                .get()
                .forEach(device -> {
                    if (check(device)) {
                        ctx.fileStores.add(new GamedosFileStore(device));
                    }
                });
    }

    public boolean check(TrackSectorDevice device) {
        if (device.getGeometry().sectorsPerDisk() == 560) {
            DataBuffer track0 = device.readRange(DIRECTORY_TRACK, 0, DIRECTORY_SECTOR);
            Optional<Integer> opt = track0.scan(MARKER, 0);
            if (opt.isPresent()) {
                DataBuffer directory = device.readRange(DIRECTORY_TRACK, DIRECTORY_SECTOR, DIRECTORY_SIZE);
                for (int i=0; i<directory.limit(); i+=ENTRY_SIZE) {
                    // Test each entry for validity. Note that the entry TYPE has every defined type even though not implemented.
                    boolean validType = directory.testUnsignedByte(ENTRY_TYPE_OFFSET, 0, 'A', 'B', 'T', 'S', 'P', 'I');
                    boolean validTrack = directory.getUnsignedByte(ENTRY_TRACK_OFFSET) < device.getGeometry().tracksOnDisk();
                    boolean validSector = directory.getUnsignedByte(ENTRY_SECTOR_OFFSET) < device.getGeometry().sectorsPerTrack();
                    boolean validName = directory.testForFixedLengthString(ENTRY_NAME_OFFSET, ENTRY_NAME_LENGTH);
                    if (!validType || !validTrack || !validSector || !validName) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static GamedosFileStore create(TrackSectorDevice device) {
        Objects.requireNonNull(device);
        if (device.getGeometry().sectorsPerDisk() != 560) {
            throw new RuntimeException("GameDOS requires a 560 sector disk.");
        }
        device.format();
        try (InputStream inputStream = GamedosFileStoreFactory.class.getResourceAsStream("/files/gamedos-track0.bin")) {
            Objects.requireNonNull(inputStream);
            byte[] track0 = inputStream.readAllBytes();
            device.writeRange(0, 0, 16, DataBuffer.wrap(track0));
            return new GamedosFileStore(device);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
