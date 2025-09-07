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
package io.github.applecommander.acx.base;

import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.FormattedDisk;
import io.github.applecommander.acx.converter.DiskFactoryContextConverter;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.hint.Hint;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Optional;

public abstract class ReadOnlyDiskContextCommandOptions extends ReusableCommandOptions {
    @Option(names = { "-d", "--disk" }, description = "Image to process [$ACX_DISK_NAME].", required = true,
            converter = DiskFactoryContextConverter.class, defaultValue = "${ACX_DISK_NAME}")
    private DiskFactory.Context ctx;

    @Option(names = { "-k", "--number" }, description = "Select disk number to access [$ACX_DISK_NUMBER].",
            defaultValue = "${ACX_DISK_NUMBER}")
    private Integer diskNumber;

    protected List<FormattedDisk> selectedDisks() {
        if (diskNumber != null) {
            return List.of(ctx.disks.get(diskNumber));
        }
        return ctx.disks;
    }

    protected DiskFactory.Context context() {
        return ctx;
    }

    protected Optional<BlockDevice> blockDevice() {
        if (!selectedDisks().isEmpty()) {
            Optional<BlockDevice> deviceOpt = selectedDisks().getFirst().get(BlockDevice.class);
            if (deviceOpt.isPresent()) {
                return deviceOpt;
            }
        }
        List<BlockDevice> devices = context().blockDevice()
                .include16Sector(Hint.PRODOS_BLOCK_ORDER)
                .include800K()
                .includeHDV()
                .get();
        if (!devices.isEmpty()) {
            return Optional.of(devices.getFirst());
        }
        return Optional.empty();
    }

    protected Optional<TrackSectorDevice> trackSectorDevice() {
        if (!selectedDisks().isEmpty()) {
            Optional<TrackSectorDevice> deviceOpt = selectedDisks().getFirst().get(TrackSectorDevice.class);
            if (deviceOpt.isPresent()) {
                return deviceOpt;
            }
        }
        List<TrackSectorDevice> devices = context().trackSectorDevice()
                .include13Sector()
                .include16Sector(Hint.DOS_SECTOR_ORDER)
                .get();
        if (!devices.isEmpty()) {
            return Optional.of(devices.getFirst());
        }
        return Optional.empty();
    }
}
