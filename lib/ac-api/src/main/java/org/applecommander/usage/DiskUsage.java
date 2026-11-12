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

import org.applecommander.filestore.FileStore;
import org.applecommander.util.InformationGroup;
import org.applecommander.util.InformationProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/// Provides a generic DiskUsage interface. Note that the assumption of free and used applies
/// only to disks. Each disk image will supply a more specific usage subclass. Additionally,
/// the assumption is that supplying a `used` and `free` count is sufficient. That is, `total`
/// is the addition of the two and the byte related numbers are just multiplied by the unit size.
/// Finally, `used` and `free` are supplied as functions to allow the disk usage to be a long-term
/// construct.
/// @see ComputedBlockUsage
/// @see SectorUsage
public abstract class DiskUsage implements InformationProvider {
    static final Set<FileStore.ChangeType> IMPACTFUL_CHANGES = Set.of(
            FileStore.ChangeType.DATA_UPDATED,
            FileStore.ChangeType.FILE_CREATED,
            FileStore.ChangeType.FILE_DELETED
    );

    private final int unitSize;

    protected DiskUsage(int unitSize) {
        this.unitSize = unitSize;
    }

    /// Compute a breakdown of the usage types.
    public abstract Map<UsageType,Integer> getUsageCounts();
    /// The total number of "units" (either sectors or blocks) supported.
    public abstract int getTotal();
    /// The number of used "units" (sectors or blocks).
    public int getUsed() {
        return getTotal() - getFree();
    }
    /// The number of free "units" (sectors or blocks).
    public int getFree() {
        return getUsageCounts().getOrDefault(UsageType.FREE, 0);
    }
    /// The total number of bytes on this device.
    public int getTotalBytes() {
        return getTotal() * unitSize;
    }
    /// The total number of bytes used.
    public int getBytesUsed() {
        return getUsed() * unitSize;
    }
    /// The total number of bytes free.
    public int getBytesFree() {
        return getFree() * unitSize;
    }

    /// Get information about this component.
    @Override
    public List<InformationGroup> information() {
        String unitType = "Sectors";
        if (this instanceof ComputedBlockUsage) {
            unitType = "Blocks";
        }
        return InformationGroup.builder("Usage")
            .item("Free (Bytes)").value("%,d", getBytesFree())
            .item("Used (Bytes)").value("%,d", getBytesUsed())
            .item("Total (Bytes)").value("%,d", getTotalBytes())
            .item("Free (%s)", unitType).value(getFree())
            .item("Used (%s)", unitType).value(getUsed())
            .item("Total (%s)", unitType).value(getTotal())
            .get();
    }
}
