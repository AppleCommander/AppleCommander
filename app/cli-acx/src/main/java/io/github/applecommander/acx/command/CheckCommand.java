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
package io.github.applecommander.acx.command;

import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosDirectoryEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import io.github.applecommander.acx.base.ReadWriteDiskCommandOptions;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

@Command(name = "check", description = "Check image for issues.", hidden = true)
public class CheckCommand extends ReadWriteDiskCommandOptions {
    @Option(names = { "--fix" }, description = "Fix defects (modifies image in place).")
    private boolean fix;

    @Override
    public int handleCommand() throws Exception {
        for (FormattedDisk disk : selectedDisks()) {
            switch (disk) {
                case ProdosFormatDisk p -> handleProdosFormatDisk(p);
                default -> {
                    System.err.println("Currently only support ProDOS disks.\n");
                    return 1;
                }
            }
        }
        return 0;
    }

    private void handleProdosFormatDisk(ProdosFormatDisk disk) throws DiskException {
        for (var dir : disk.getFiles()) {
            if ( ! (dir instanceof ProdosDirectoryEntry pdosDir)) {
                // Skip anything but directories
                continue;
            }
            if (pdosDir.getHeaderPointer() != 2) {
                System.out.printf("Subdirectory %s is not pointing to the key block of disk %s.\n", pdosDir.getDirname(), disk.getDirname());
            }
            if (fix) {
                applyHeaderPointerFix(pdosDir, 2);
            }
            handleDirectory(pdosDir);
        }
    }

    private void handleDirectory(ProdosDirectoryEntry mainDir) throws DiskException {
        for (var dir : mainDir.getFiles()) {
            if ( ! (dir instanceof ProdosDirectoryEntry pdosDir)) {
                // Skip anything but directories
                continue;
            }
            if (pdosDir.getHeaderPointer() != mainDir.getKeyPointer()) {
                System.out.printf("Subdirectory %s is not pointing to the key block of directory %s.\n", pdosDir.getDirname(), mainDir.getDirname());
            }
            if (fix) {
                applyHeaderPointerFix(pdosDir, mainDir.getKeyPointer());
            }
            handleDirectory(pdosDir);
        }
    }

    private void applyHeaderPointerFix(ProdosDirectoryEntry pdosDir, int newHeaderPointerBlock) {
        pdosDir.setHeaderPointer(newHeaderPointerBlock);
        System.out.printf("Patch applied to subdirectory %s.\n", pdosDir.getDirname());
    }
}
