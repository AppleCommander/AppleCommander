/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2021-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.util.filestreamer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

public class FileTuple {
    public static final String SEPARATOR = "/";
    private static final Logger LOG = Logger.getLogger(FileTuple.class.getName());
    public final FormattedDisk formattedDisk;
    public final List<String> paths;
    public final DirectoryEntry directoryEntry;
    public final FileEntry fileEntry;
    
    private FileTuple(FormattedDisk formattedDisk, 
                      List<String> paths, 
                      DirectoryEntry directoryEntry, 
                      FileEntry fileEntry) {
        this.formattedDisk = formattedDisk;
        this.paths = Collections.unmodifiableList(paths);
        this.directoryEntry = directoryEntry;
        this.fileEntry = fileEntry;
    }
    
    public FileTuple pushd(FileEntry directoryEntry) {
        LOG.fine("Adding directory " + directoryEntry.getFilename());
        List<String> newPaths = new ArrayList<>(paths);
        newPaths.add(directoryEntry.getFilename());
        return new FileTuple(formattedDisk, newPaths, (DirectoryEntry)directoryEntry, null);
    }
    public boolean isDisk() {
        // Just in case directoryEntry is unset or is a disk - looks like either cna occur!
        return fileEntry == null && (directoryEntry == null || directoryEntry == formattedDisk);
    }
    public boolean isDirectory() {
        return !isDisk() && (fileEntry == null || fileEntry.isDirectory());
    }
    public boolean isFile() {
        return fileEntry != null && !fileEntry.isDirectory();
    }
    public FileTuple of(FileEntry fileEntry) {
        return new FileTuple(formattedDisk, paths, directoryEntry, fileEntry);
    }
    public String fullPath() {
        return String.join(SEPARATOR, String.join(SEPARATOR, paths), fileEntry.getFilename());
    }
    
    public static FileTuple of(FormattedDisk disk) {
        return new FileTuple(disk, new ArrayList<String>(), (DirectoryEntry)disk, null);
    }
}
