/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx.command;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;
import io.github.applecommander.acx.base.ReadOnlyDiskContextCommandOptions;
import org.applecommander.device.Device;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.hint.Hint;
import org.applecommander.source.Source;
import org.applecommander.util.InformationGroup;
import org.applecommander.util.InformationItem;
import org.applecommander.util.InformationProvider;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.logging.Logger;

@Command(name = "info", description = "Show information on a disk image(s).",
        aliases = "i")
public class InfoCommand extends ReadOnlyDiskContextCommandOptions {
    private static final Logger LOG = Logger.getLogger(InfoCommand.class.getName());
    
    @Override
    public int handleCommand() {
        LOG.info(() -> "Path: " + context().source.getName());
        if (selectedDisks().isEmpty()) {
            showInformationGroups(context().source);
            List<TrackSectorDevice> devices = context().trackSectorDevice()
                    .include13Sector()
                    .include16Sector(Hint.DOS_SECTOR_ORDER)
                    .get();
            for (TrackSectorDevice device : devices) {
                showInformationGroups(device);
            }
        }
        else {
            for (FormattedDisk formattedDisk : selectedDisks()) {
                LOG.info(() -> String.format("Disk: %s (%s)", formattedDisk.getDiskName(), formattedDisk.getFormat()));
                for (DiskInformation diskinfo : formattedDisk.getDiskInformation()) {
                    System.out.printf("%s: %s\n", diskinfo.getLabel(), diskinfo.getValue());
                }
                formattedDisk.get(Device.class).ifPresent(this::showInformationGroups);
                formattedDisk.getSource().get(Source.class).ifPresent(this::showInformationGroups);
                System.out.println();
            }
        }
        return 0;
    }

    private void showInformationGroups(InformationProvider provider) {
        for (InformationGroup group : provider.information()) {
            System.out.printf("--- %s ---\n", group.title());
            for (InformationItem info : group.items()) {
                System.out.printf("%s: %s\n", info.label(), info.value());
            }
        }

    }
}
