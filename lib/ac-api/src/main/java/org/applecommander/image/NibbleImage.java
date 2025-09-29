/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package org.applecommander.image;

import org.applecommander.capability.Capability;
import org.applecommander.device.nibble.NibbleTrackReaderWriter;
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

public class NibbleImage implements NibbleTrackReaderWriter {
    public static final int DISK_SIZE = 232960;
    public static final int TRACK_SIZE = 6656;
    public static final int TRACKS_ON_DEVICE = DISK_SIZE / TRACK_SIZE;

    private final Source source;

    public NibbleImage(Source source) {
        this.source = source;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, source);
    }

    @Override
    public boolean can(Capability capability) {
        return capability == Capability.WRITE_TRACK;
    }

    @Override
    public int getTracksOnDevice() {
        return TRACKS_ON_DEVICE;
    }

    @Override
    public DataBuffer readTrackData(int track) {
        return source.readBytes(track * TRACK_SIZE, TRACK_SIZE);
    }

    @Override
    public void writeTrackData(int track, DataBuffer data) {
        assert(data.limit() == TRACK_SIZE);
        source.writeBytes(track * TRACK_SIZE, data);
    }
}
