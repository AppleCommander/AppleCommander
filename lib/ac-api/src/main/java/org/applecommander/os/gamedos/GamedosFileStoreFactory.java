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

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class GamedosFileStoreFactory implements FileStoreFactory {
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
            DataBuffer track0 = device.readRange(GamedosDirectory.DIRECTORY_TRACK, 0, GamedosDirectory.DIRECTORY_SECTOR);
            Optional<Integer> opt = track0.scan("GAMEDOS".getBytes(StandardCharsets.UTF_8), 0);
            if (opt.isPresent()) {
                DataBuffer directory = device.readRange(GamedosDirectory.DIRECTORY_TRACK, GamedosDirectory.DIRECTORY_SECTOR,
                        GamedosDirectory.DIRECTORY_SIZE);
                for (int i=0; i<directory.limit(); i+=GamedosFileEntry.ENTRY_SIZE) {
                    // Test each entry for validity. Note that the entry TYPE has everny defined type even thought not implemented.
                    boolean validType = directory.testUnsignedByte(GamedosFileEntry.ENTRY_TYPE_OFFSET, 0, 'A', 'B', 'T', 'S', 'P', 'I');
                    boolean validTrack = directory.getUnsignedByte(GamedosFileEntry.ENTRY_TRACK_OFFSET) < device.getGeometry().tracksOnDisk();
                    boolean validSector = directory.getUnsignedByte(GamedosFileEntry.ENTRY_SECTOR_OFFSET) < device.getGeometry().sectorsPerTrack();
                    boolean validName = directory.testForFixedLengthString(GamedosFileEntry.ENTRY_NAME_OFFSET,  GamedosFileEntry.ENTRY_NAME_LENGTH);
                    if (!validType || !validTrack || !validSector || !validName) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
