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

import java.io.File;

import com.webcodepro.applecommander.storage.Disk;

import io.github.applecommander.acx.base.ReusableCommandOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "convert", description = 
            "Uncompress a ShrinkIt or Binary II file; "
            + "or convert a DiskCopy 4.2 image into a ProDOS disk image.")
public class ConvertCommand extends ReusableCommandOptions {
    @Option(names = { "-d", "--disk" }, description = "Image to create [$ACX_DISK_NAME].", required = true,
            defaultValue = "${ACX_DISK_NAME}")
    private String diskName;
    
    @Option(names = { "-f", "--force" }, description = "Allow existing disk image to be replaced.")
    private boolean overwriteFlag;

    @Parameters(description = "Archive to convert.", arity = "1")
    private String archiveName;

    @Override
    public int handleCommand() throws Exception {
        File targetFile = new File(diskName);
        if (targetFile.exists() && !overwriteFlag) {
            throw new RuntimeException("File exists and overwriting not enabled.");
        }
        
        Disk disk = new Disk(archiveName);
        disk.setFilename(diskName);
        saveDisk(disk);
                    
        return 0;
    }
}
