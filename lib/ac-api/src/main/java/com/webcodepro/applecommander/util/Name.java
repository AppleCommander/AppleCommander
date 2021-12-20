package com.webcodepro.applecommander.util;

import java.util.List;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.ui.UiBundle;

public class Name {
    private static TextBundle textBundle = UiBundle.getInstance();

    private String fullName;
    private String name;
    private String[] path;
    
    public Name(String s) {
        this.fullName = s;
        if (s.startsWith("/")) {
            fullName = s.substring(1, s.length());
        }
        this.path = s.split("/");
        this.name = path[path.length - 1];
    }
    
    public FileEntry getEntry(FormattedDisk formattedDisk) throws DiskException {
        List<FileEntry> files = formattedDisk.getFiles();
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
    
    public FileEntry createEntry(FormattedDisk formattedDisk) throws DiskException {
        if (path.length == 1) {
            return formattedDisk.createFile();
        }
        List<FileEntry> files = formattedDisk.getFiles();
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
                    dir = formattedDisk.createDirectory(dirName);
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
