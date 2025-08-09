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

import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;

import io.github.applecommander.acx.base.ReadOnlyDiskImageCommandOptions;
import org.applecommander.source.Source;
import org.applecommander.util.Information;
import picocli.CommandLine.Command;

@Command(name = "info", description = "Show information on a disk image(s).",
        aliases = "i")
public class InfoCommand extends ReadOnlyDiskImageCommandOptions {
    private static Logger LOG = Logger.getLogger(InfoCommand.class.getName());
    
    @Override
    public int handleCommand() throws Exception {
        LOG.info(() -> "Path: " + disk.getFilename());
        FormattedDisk[] formattedDisks = disk.getFormattedDisks();
        for (int i = 0; i < formattedDisks.length; i++) {
            FormattedDisk formattedDisk = formattedDisks[i];
            LOG.info(() -> String.format("Disk: %s (%s)", formattedDisk.getDiskName(), formattedDisk.getFormat()));
            for (DiskInformation diskinfo : formattedDisk.getDiskInformation()) {
                System.out.printf("%s: %s\n", diskinfo.getLabel(), diskinfo.getValue());
            }
            formattedDisk.getDiskImageManager().get(Source.class).ifPresent(source -> {
                for (Information info : source.information()) {
                    System.out.printf("%s: %s\n", info.label(), info.value());
                }
            });
            System.out.println();
        }
        return 0;
    }
}
