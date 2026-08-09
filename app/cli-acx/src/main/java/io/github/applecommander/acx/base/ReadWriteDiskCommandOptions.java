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

import org.applecommander.util.BackupStrategy;
import picocli.CommandLine.Option;

public abstract class ReadWriteDiskCommandOptions extends ReadOnlyDiskImageCommandOptions {
    @Option(names = "--backup", description = {
            "Automated backup when an image changes; use 'bak' to append '.bak' to file name;",
            "any other value is assumed to be a directory. [$ACX_BACKUP]"
        }, defaultValue = "${ACX_BACKUP}")
    private String backupCode = "";

    @Override
    public Integer call() throws Exception {
        int returnCode = handleCommand();
        
        if (returnCode == 0) {
            BackupStrategy backupStrategy = BackupStrategy.create(backupCode);
            saveDisk(selectedDisks().getFirst(), backupStrategy);
        }
        
        return returnCode;
    }
}
