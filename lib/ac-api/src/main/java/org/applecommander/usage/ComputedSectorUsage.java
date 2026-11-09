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
package org.applecommander.usage;

import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.FileStore;

import java.util.Objects;
import java.util.function.Consumer;

/// Provides sector level usage information where the usage details must be computed.
public class ComputedSectorUsage extends SectorUsage {
    private final Consumer<UsagePopulator> usageFn;
    private byte[] usage;

    public ComputedSectorUsage(TrackSectorDevice.Geometry geometry,
                               Consumer<UsagePopulator> usageFn) {
        super(geometry);
        Objects.requireNonNull(usageFn);
        this.usageFn = usageFn;
    }

    public void populateUsage() {
        usage = new byte[getGeometry().sectorsPerDisk()];
        usageFn.accept(this::setUsage);
    }

    @Override
    public UsageType getUsage(int track, int sector) {
        if (usage == null) {
            populateUsage();
        }
        return UsageType.values()[usage[getGeometry().calculateSectorOffset(track,sector)]];
    }

    public void setUsage(int track, int sector, UsageType usageType) {
        assert track < getGeometry().tracksOnDisk();
        assert sector < getGeometry().sectorsPerTrack();
        int pos = getGeometry().calculateSectorOffset(track, sector);
        usage[pos] = (byte) usageType.ordinal();
    }

    @Override
    public void stateChanged(FileStore.FileStoreChangeEvent event) {
        if (IMPACTFUL_CHANGES.contains(event.getChangeType())) {
            usage = null;
        }
    }

    public interface UsagePopulator {
        void setUsage(int track, int sector, UsageType usageType);
    }
}
