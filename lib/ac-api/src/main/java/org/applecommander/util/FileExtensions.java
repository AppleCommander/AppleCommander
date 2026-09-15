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
package org.applecommander.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileExtensions {
    public static final List<FileExtension> FILE_EXTENSIONS;

    static {
        List<FileExtension> fileExtensions = new ArrayList<>();
        List<String> allExtensions = new ArrayList<>();
        List<String> diskExtensions = new ArrayList<>();
        List<String> archiveExtensions = new ArrayList<>();
        // Disk Image types seem to all come with possible GZip compression
        List.of("16-sector DOS Ordered Images:do,d16,dsk",
                "13-sector DOS Ordered Images:d13",
                "Nibble Images:nib",
                "140K ProDOS Ordered Images:po",
                "800K ProDOS Ordered Images:2mg,2img",
                "WOZ 1.x/2.x Images:woz",
                "Disk Copy Images:dc",
                "ApplePC Hard Disk Images:hdv")
        .forEach(extension -> {
            String[] parts = extension.split(":");
            List<String> plainExtensions = new ArrayList<>();
            List<String> gzipExtensions = new ArrayList<>();
            for (String ext : parts[1].split(",")) {
                plainExtensions.add(String.format("*.%s", ext));
                gzipExtensions.add(String.format("*.%s.gz", ext));
            }
            String description = String.format("%s (%s)", parts[0], String.join(", ", plainExtensions));
            List<String> combinedExtensions = new ArrayList<>();
            combinedExtensions.addAll(plainExtensions);
            combinedExtensions.addAll(gzipExtensions);
            FileExtension fileExtension = new FileExtension(description, Collections.unmodifiableList(combinedExtensions));
            fileExtensions.add(fileExtension);
            allExtensions.addAll(combinedExtensions);
            diskExtensions.addAll(combinedExtensions);
        });
        // Archive formats generally do not have compression
        List.of("Zip Images:zip")
        .forEach(extension -> {
            String[] parts = extension.split(":");
            List<String> extensions = new ArrayList<>();
            for (String ext : parts[1].split(",")) {
                extensions.add(String.format("*.%s", ext));
            }
            String description = String.format("%s (%s)", parts[0], String.join(",", extensions));
            FileExtension fileExtension = new FileExtension(description, Collections.unmodifiableList(extensions));
            fileExtensions.add(fileExtension);
            allExtensions.addAll(extensions);
            archiveExtensions.addAll(extensions);
        });

        fileExtensions.addFirst(new FileExtension("All Images", allExtensions));
        fileExtensions.add(new FileExtension("All Disk Images", diskExtensions));
        fileExtensions.add(new FileExtension("All Archive Images", archiveExtensions));
        fileExtensions.add(new FileExtension("All Files", List.of("*.*")));
        FILE_EXTENSIONS = Collections.unmodifiableList(fileExtensions);
    }

    public record FileExtension(String description, List<String> extensions) {}
}
