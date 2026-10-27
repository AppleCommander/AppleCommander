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

import com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk;
import org.applecommander.capability.Capability;
import org.applecommander.source.Source;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.DiskUsage.UsageType;
import org.applecommander.usage.SectorUsage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class FileStoreTestHelper {
    public static List<FileStore> showDirectory(Source source) {
        FileStoreFactory.Context ctx = FileStores.inspect(source);

        // HACK - Let's TOSS OUT the older interfaces as they get replaced.
        ctx.fileStores.removeIf(fileStore -> fileStore.get(RdosFormatDisk.class).isPresent());

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
                .filter(entry -> entry.supports(DisplayMode.DETAIL))
                .toList();
        if (parent.getParent().isEmpty()) {
            // Print columns only at the top
            System.out.print(indent);
            displayColumns.forEach(column -> {
                System.out.print(column.getTitle());
                System.out.print(" ");
            });
            System.out.println();
        }
        for (FileEntry file : parent.getFiles()) {
            if (!file.isDeleted()) {
                System.out.print(indent);
                displayColumns.forEach(column -> {
                    System.out.print(column.getDisplayText(file));
                    System.out.print(" ");
                });
                System.out.println();

                Optional<Directory> subdirectory = file.get(Directory.class);
                subdirectory.ifPresent(directoryEntry -> showFiles(directoryEntry, indent + "  "));
            }
        }
    }

    public static void showDiskUsage(DiskUsage usage) {
        Map<UsageType,String> codes = Map.of(
                UsageType.FREE, ".",
                UsageType.USED, "U",
                UsageType.BOOT, "0",
                UsageType.BITMAP, "B",
                UsageType.SYSTEM, "S",
                UsageType.DIRECTORY, "D",
                UsageType.FILE, "F"
        );
        switch (usage) {
            case BlockUsage blockUsage -> {
                System.out.printf("--- BLOCK USAGE 0 TO %d ---\n", blockUsage.getTotal()-1);
                for (int block=0; block<blockUsage.getTotal(); block++) {
                    if (block > 0 && block % 80 == 0) System.out.println();
                    System.out.print(codes.getOrDefault(blockUsage.getUsage(block), "?"));
                }
                System.out.println();
            }
            case SectorUsage sectorUsage -> {
                for (int i = 0 ; i < 3; i++) {
                    System.out.print(switch (i) {
                        case 0 ->  "TRK | ";
                        case 1 ->  "    | ";
                        default -> "----+-";
                    });
                    for (int t = 0; t< sectorUsage.getTracksOnDisk(); t++) {
                        String trk = String.format("%02X", t);
                        switch (i) {
                            case 0 -> System.out.printf("%c", trk.charAt(0));
                            case 1 -> System.out.printf("%c", trk.charAt(1));
                            default -> System.out.print("-");
                        }
                    }
                    System.out.println();
                }
                final String sectorTitle = "   SECTORS";
                for (int s = 0; s<sectorUsage.getSectorsPerTrack(); s++) {
                    char ch = s < sectorTitle.length() ? sectorTitle.charAt(s) : ' ';
                    System.out.printf("%c %02X| ", ch, s);
                    for (int t = 0; t<sectorUsage.getTracksOnDisk(); t++) {
                        System.out.print(codes.getOrDefault(sectorUsage.getUsage(t,s), "?"));
                    }
                    System.out.println();
                }
            }
            default -> throw new RuntimeException("Unknown disk usage: " + usage);
        }
        System.out.println(codes);
    }
}
