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
package io.github.applecommander.acx.base;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.Disk;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n",
         parameterListHeading = "%nParameters:%n")
public abstract class ReusableCommandOptions implements Callable<Integer> {
    private static Logger LOG = Logger.getLogger(ReusableCommandOptions.class.getName());

    @Option(names = { "-h", "--help" }, description = "Show help for subcommand.", usageHelp = true)
    private boolean helpFlag;
    
    @Override
    public Integer call() throws Exception {
        return handleCommand();
    }
    
    public abstract int handleCommand() throws Exception;

    public void saveDisk(Disk disk) {
        try {
            // Only save if there are changes.
            if (disk.getSource().hasChanged()) {
                LOG.fine(() -> String.format("Saving disk '%s'", disk.getFilename()));
                disk.save();
            } else {
                LOG.fine(() -> String.format("Disk '%s' has not changed; not saving.", disk.getFilename()));
            }
        } catch (IOException e) {
            LOG.severe(e.getMessage());
        }
    }
}
