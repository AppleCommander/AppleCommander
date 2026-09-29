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

import org.applecommander.capability.Capability;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.DisplayColumn;
import org.applecommander.filestore.FileStore;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.InformationGroup;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.applecommander.filestore.DisplayColumn.Mode;

public class GamedosFileStore implements FileStore {
    private final TrackSectorDevice device;
    private final GamedosDirectory rootDirectory;

    public GamedosFileStore(TrackSectorDevice device) {
        this.device = device;
        this.rootDirectory = new GamedosDirectory(this);
    }

    @Override
    public String getLabel() {
        DataBuffer track0 = device.readRange(0, 0, 14);
        Optional<Integer> opt = track0.scan("GAMEDOS".getBytes(StandardCharsets.UTF_8),0);
        if (opt.isPresent()) {
            int pos = opt.get() + 8;    // Go past "GAMEDOS "
            int ch;
            // Search for the "V9.9"
            do {
                ch = track0.getUnsignedByte(pos);
                pos++;
            } while (ch == 'V' || (ch >= '0' && ch <= '9') || ch == '.');
            return track0.getFixedLengthString(opt.get(), pos-opt.get()-1);
        }
        return "GameDOS";
    }

    @Override
    public GamedosDirectory getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public String getPathSeparator() {
        return "";
    }

    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(GamedosFileEntry.class)
            .addStringField("Type", GamedosFileEntry::getFiletype)
            .addStringField("Name", GamedosFileEntry::getName)
            .addIntField("Meta", GamedosFileEntry::getMeta, "$%04X")
            .addIntField("First Track", GamedosFileEntry::getFirstTrack, Mode.DETAIL)
            .addIntField("First Sector", GamedosFileEntry::getFirstSector, Mode.DETAIL)
            .addIntField("Sector Count", GamedosFileEntry::getSectorCount, Mode.DETAIL)
            .addLongField("Size", GamedosFileEntry::getSize)
            .toList();
    }

    @Override
    public boolean can(Capability capability) {
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device);
    }

    @Override
    public List<InformationGroup> information() {
        return List.of();
    }
}
