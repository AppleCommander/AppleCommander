/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2025 by Robert Greene
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
package com.webcodepro.applecommander.storage;

import com.webcodepro.applecommander.util.TextBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifies a filter to be used in determining filetypes which are supported.
 * This works from a file extension, so it may or may not apply to the Macintosh.
 */
public class FilenameFilter {
    private String names;
    private String[] extensions;

    public FilenameFilter(String names, String... extensions) {
        this.names = names;
        this.extensions = extensions;
    }

    public String getExtensions() {
        return String.join(";", extensions);
    }

    public String[] getExtensionList() {
        return extensions;
    }

    public String getNames() {
        return names;
    }

    private static final TextBundle textBundle = StorageBundle.getInstance();
    private static final FilenameFilter[] filenameFilters;
    private static final List<String> allFileExtensions;
    static {
        // Build everything dynamically
        List<String> templates = List.of(
                "16SectorDosImages:do,d16,dsk",
                "13SectorDosImages:d13",
                "140kProdosImages:po",
                "140kNibbleImages:nib",
                "800kProdosImages:2mg,2img",
                "ApplePcImages:hdv",
                "WozImages:woz",
                "DiskCopyImages:dc");
        List<FilenameFilter> filters = new ArrayList<>();
        List<String> allImages = new ArrayList<>(List.of("*.shk", "*.sdk"));
        List<String> compressedImages = new ArrayList<>(allImages);
        for (String template : templates) {
            String[] parts = template.split(":");
            String bundleName = String.format("Disk.%s", parts[0]);
            List<String> extensions = new ArrayList<>();
            for (String extension : parts[1].split(",")) {
                String ext1 = String.format("*.%s", extension);
                String ext2 = String.format("*.%s.gz", extension);
                extensions.add(ext1);
                extensions.add(ext2);
                compressedImages.add(ext2);
            }
            allImages.addAll(extensions);
            String text = textBundle.get(bundleName);
            filters.add(new FilenameFilter(text, extensions.toArray(new String[0])));
        }
        filters.addFirst(new FilenameFilter(textBundle.get("Disk.AllImages"), allImages.toArray(new String[0])));
        filters.add(new FilenameFilter(textBundle.get("Disk.CompressedImages"), compressedImages.toArray(new String[0])));
        filters.add(new FilenameFilter(textBundle.get("Disk.AllFiles"), "*.*"));
        filenameFilters = filters.toArray(new FilenameFilter[0]);
        // allFileExtensions is of the format ".dsk", ".dsk.gz", so we just strip the first character off...
        allFileExtensions = allImages.stream().map(s -> s.substring(1)).toList();
    }

    /**
     * Get the supported file filters supported by the Disk interface.
     * This is due to the fact that FilenameFilter is an inner class of Disk -
     * without an instance of the class, the filters cannot be created.
     */
    public static FilenameFilter[] getFilenameFilters() {
        return filenameFilters;
    }

    /**
     * Get the supported file extensions supported by the Disk interface.
     * This is used by the Swing UI to populate the open file dialog box.
     */
    public static List<String> getAllExtensions() {
        return allFileExtensions;
    }
}
