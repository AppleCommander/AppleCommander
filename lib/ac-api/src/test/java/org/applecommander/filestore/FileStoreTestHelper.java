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

import org.applecommander.capability.Capability;
import org.applecommander.source.Source;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.SectorUsage;

import java.util.List;
import java.util.Optional;

public abstract class FileStoreTestHelper {
    public static List<FileStore> showDirectory(Source source) {
        FileStoreFactory.Context ctx = FileStores.inspect(source);
        if (ctx.fileStores.isEmpty()) {
            throw new RuntimeException("no disks discovered for: " + source.getName());
        }
        for (FileStore fileStore : ctx.fileStores) {
            System.out.println();
            System.out.println(fileStore.getLabel());
            showFiles(fileStore.getRootDirectory(), "");
            Optional<DiskUsage> opt = fileStore.get(DiskUsage.class);
            opt.ifPresent(diskUsage -> {
                System.out.println(diskUsage.getBytesFree() + " bytes free.");
                System.out.println(diskUsage.getBytesUsed() + " bytes used.");
            });
            System.out.println("This disk " + (fileStore.can(Capability.SUPPORTS_DIRECTORIES) ? "does" : "does not") +
                    " support directories.");
            //System.out.println("This disk is formatted in the " + fileStore.getFormat() + " format.");
            System.out.println();

            opt.ifPresent(FileStoreTestHelper::showDiskUsage);
        }
        return ctx.fileStores;
    }

    public static void showFiles(Directory parent, String indent) {
        FileStore fileStore = parent.getFileStore();
        List<DisplayColumn> displayColumns = fileStore.getDisplayColumns().stream()
                .filter(entry -> entry.supports(DisplayColumn.Mode.DETAIL))
                .toList();
        if (parent.getParent().isEmpty()) {
            // Print columns only at the top
            System.out.print(indent);
            displayColumns.forEach(column -> {
                System.out.print(column.headerText());
                System.out.print(" ");
            });
            System.out.println();
        }
        for (FileEntry file : parent.getFiles()) {
            if (!file.isDeleted()) {
                System.out.print(indent);
                displayColumns.forEach(column -> {
                    System.out.print(column.formatAsText(file));
                    System.out.print(" ");
                });
                System.out.println();

                Optional<Directory> subdirectory = file.get(Directory.class);
                subdirectory.ifPresent(directoryEntry -> showFiles(directoryEntry, indent + "  "));
            }
        }
    }

    public static void showDiskUsage(DiskUsage usage) {
        switch (usage) {
            case BlockUsage blockUsage -> {
                System.out.printf("--- BLOCK USAGE 0 TO %d ---\n", blockUsage.getTotal()-1);
                for (int block=0; block<blockUsage.getTotal(); block++) {
                    if (block > 0 && block % 80 == 0) System.out.println();
                    System.out.print(blockUsage.isUsed(block) ? "U" : ".");
                }
                System.out.println();
            }
            case SectorUsage sectorUsage -> {
                System.out.printf("---> TRACK USAGE 0 TO %d --->\n", sectorUsage.getTotalTracks()-1);
                System.out.printf("v--- SECTOR USAGE 0 TO %d ---v\n", sectorUsage.getTotalSectors()-1);
                for (int s=sectorUsage.getTotalSectors()-1; s>=0; s--) {
                    for (int t=0; t<sectorUsage.getTotalTracks(); t++) {
                        System.out.print(sectorUsage.isUsed(t,s) ? "U" : ".");
                    }
                    System.out.println();
                }
            }
            default -> throw new RuntimeException("Unknown disk usage: " + usage);
        }
        System.out.println("U = used, . = free");
    }
}
