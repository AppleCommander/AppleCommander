/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003-2022 by Robert Greene
 * robgreene at users.sourceforge.net
 * Copyright (C) 2003-2022 by John B. Matthews
 * matthewsj at users.sourceforge.net
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
package com.webcodepro.applecommander.util;

import java.util.List;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.ui.UiBundle;

public class Name {
    private static final TextBundle textBundle = UiBundle.getInstance();

    private String fullName;
    private final String name;
    private final String[] path;
    
    public Name(String s) {
        this.fullName = s;
        if (s.startsWith("/")) {
            fullName = s.substring(1, s.length());
        }
        this.path = s.split("/");
        this.name = path[path.length - 1];
    }
    
    public FileEntry getEntry(DirectoryEntry directoryEntry) throws DiskException {
        List<FileEntry> files = directoryEntry.getFiles();
        FileEntry entry = null;
        for (int i = 0; i < path.length - 1; i++) {
            String dirName = path[i];
            for (int j = 0; j < files.size(); j++) {
                entry = (FileEntry) files.get(j);
                String entryName = entry.getFilename();
                if (entry.isDirectory() && dirName.equalsIgnoreCase(entryName)) {
                    files = ((DirectoryEntry) entry).getFiles();
                }
            }
        }
        for (int i = 0; i < files.size(); i++) {
            entry = (FileEntry) files.get(i);
            String entryName = entry.getFilename();
            if (!entry.isDeleted() && name.equalsIgnoreCase(entryName)) {
                return entry;
            }
        }
        return null;
    }
    
    public FileEntry createEntry(DirectoryEntry directoryEntry) throws DiskException {
        if (path.length == 1) {
            return directoryEntry.createFile();
        }
        List<FileEntry> files = directoryEntry.getFiles();
        DirectoryEntry dir = null, parentDir = null;
        for (int i = 0; i < path.length - 1; i++) {
            String dirName = path[i];
            dir = null;
            for (int j = 0; j < files.size(); j++) {
                FileEntry entry = (FileEntry) files.get(j);
                String entryName = entry.getFilename();
                if (!entry.isDeleted() && entry.isDirectory() && dirName.equalsIgnoreCase(entryName)) {
                    dir = (DirectoryEntry) entry;
                    parentDir = dir;
                    files = dir.getFiles();
                }
            }
            if (dir == null) {
                if (parentDir != null) {
                    // If there's a parent directory in the mix, add
                    // the new child directory to that.
                    dir = parentDir.createDirectory(dirName);
                    parentDir = dir;
                } else {
                    // Add the directory to the root of the filesystem
                    dir = directoryEntry.createDirectory(dirName);
                    parentDir = dir;
                }
            }
        }
        if (dir != null) {
            return dir.createFile();
        } else {
            System.err.println(textBundle.format(
                "CommandLineNoMatchMessage", fullName)); //$NON-NLS-1$
            return null;
        }
    }
}
