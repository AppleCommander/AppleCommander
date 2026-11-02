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

import com.webcodepro.applecommander.storage.DiskConstants;
import org.applecommander.device.*;
import org.applecommander.image.NibbleImage;
import org.applecommander.image.UniversalDiskImage;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;

/// Indicator for the organization of the file store.
/// If the file store is an archive, just use the archive organization.
/// Otherwise, pick the most appropriate image type.
public enum FileStoreOrganization {
    ARCHIVE("Archive", null, null, 0),
    DISK_13SECTOR_DO("13-sector disk", "d13", SectorOrder.DOS, DiskConstants.APPLE_13SECTOR_DISK),
    DISK_16SECTOR_DO("16-sector disk", "do", SectorOrder.DOS, DiskConstants.APPLE_140KB_DISK),
    DISK_16SECTOR_PO("16-sector disk", "po", SectorOrder.PRODOS, DiskConstants.APPLE_140KB_DISK),
    DISK_16SECTOR_NIB("16-sector disk", "nib", SectorOrder.NIBBLE, DiskConstants.APPLE_140KB_NIBBLE_DISK),
    DISK_800K_PO("800K disk", "po", null, DiskConstants.APPLE_800KB_DISK),
    DISK_800K_2IMG("800K disk", "2img", null, DiskConstants.APPLE_800KB_2IMG_DISK),
    DISK_5MB_PO("5MB disk", "hdv", null, DiskConstants.APPLE_5MB_HARDDISK),
    DISK_10MB_PO("10MB disk", "hdv", null, DiskConstants.APPLE_10MB_HARDDISK),
    DISK_20MB_PO("20MB disk", "hdv", null, DiskConstants.APPLE_20MB_HARDDISK),
    DISK_32MB_PO("32MB disk", "hdv", null, DiskConstants.APPLE_32MB_HARDDISK);

    private final String title;
    private final String extension;
    private final SectorOrder sectorOrder;
    private final int size;

    FileStoreOrganization(String title, String extension, SectorOrder sectorOrder, int size) {
        this.title = title;
        this.extension = extension;
        this.sectorOrder = sectorOrder;
        this.size = size;
    }

    public String getDescription() {
        if (extension != null) {
            return String.format("%s ('*.%s')", title, extension);
        }
        return title;
    }

    public String getExtension() {
        return extension;
    }
    public SectorOrder getSectorOrder() {
        return sectorOrder;
    }
    public int getSize() {
        return size;
    }

    /// Create the source object for this FileStore organization. The extension is required
    /// due to the fact that _all_ archives are under `ARCHIVE`.
    /// Expected to be used directly by all archive and indirectly by all disk images.
    public Source createSource(String fileExtension) {
        String fileName = String.format("%s.%s", this == ARCHIVE ? "new-archive" : "new-disk", fileExtension);
        if (this == DISK_800K_2IMG) {
            return UniversalDiskImage.create(size, fileName);
        }
        return DataBufferSource.create(size, fileName).get();
    }

    /// Create the corresponding `TrackSectorDevice`. The device will be formatted, which will
    /// handle the nibble images.
    public TrackSectorDevice createTrackSectorDevice() {
        if (size == 0 || sectorOrder == null) {
            throw new UnsupportedOperationException();
        }
        Source source = createSource(extension);
        TrackSectorDevice device = switch (sectorOrder) {
            case DOS, PRODOS, CPM -> new OrderedTrackSectorDevice(source, sectorOrder);
            case NIBBLE -> TrackSectorNibbleDevice.create(new NibbleImage(source), 16);
        };
        device.format();
        return device;
    }

    /// Create the corresponding `BlockDevice`.
    public BlockDevice createBlockDevice() {
        if (size == 0 || this == DISK_13SECTOR_DO) {
            throw new UnsupportedOperationException();
        }
        if (sectorOrder != null) {
            TrackSectorDevice trackSectorDevice = createTrackSectorDevice();
            return new TrackSectorToBlockAdapter(trackSectorDevice.request(SectorOrder.PRODOS),
                    TrackSectorToBlockAdapter.BlockStyle.PRODOS);
        }
        Source source = createSource(extension);
        return new ProdosOrderedBlockDevice(source, TrackSectorToBlockAdapter.BlockStyle.PRODOS.blockSize);
    }
}
