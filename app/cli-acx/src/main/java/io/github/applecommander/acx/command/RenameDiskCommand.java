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
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;

import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "rename-disk", description = "Rename volume of a disk image.")
public class RenameDiskCommand extends ReadWriteDiskCommandOptions {
    private static Logger LOG = Logger.getLogger(RenameDiskCommand.class.getName());
    
    @Parameters(description = "Disk name.")
    private String diskName;

    @Override
    public int handleCommand() throws Exception {
		FormattedDisk[] formattedDisks = disk.getFormattedDisks();
		FormattedDisk formattedDisk = formattedDisks[0];
    	if (formattedDisk instanceof ProdosFormatDisk || formattedDisk instanceof PascalFormatDisk) {
			formattedDisk.setDiskName(diskName);
	    	return 0;
    	} else {
    		LOG.warning("Disk must be ProDOS or Pascal.");
    		return 1;
    	}
    }
}
