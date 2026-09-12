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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A DirectoryEntry represents a directory in a FileStore.  Note that even
 * FileStores that don't naturally support a directory do contain a root directory,
 * so there will be a directory associated to each FileStore.
 */
public interface Directory {
    /**
     * Get the parent directory. If this is the root directory, it returns empty.
     */
    Optional<Directory> getParent();
    /**
     * Returns the FileStore that this Directory belongs to.
     */
    FileStore getFileStore();
    /**
     * Return all entries that are stored in this directory.
     */
    List<FileEntry> getFiles();
    /**
     * Filter out the <code>DirectoryEntry</code> entries in this directory.
     */
    default List<Directory> getDirectories() {
        return getFiles().stream()
                .map(fileEntry -> fileEntry.get(Directory.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    /**
     * Locate a given file of the given name recursively on the disk.
     */
    default Optional<FileEntry> findFile(String name) {
        for (FileEntry fileEntry : getFiles()) {
            if (name.equalsIgnoreCase(fileEntry.getName())) {
                return Optional.of(fileEntry);
            }
            Optional<Directory> directory = fileEntry.get(Directory.class);
            if (directory.isPresent()) {
                Optional<FileEntry> finding = directory.get().findFile(name);
                if (finding.isPresent()) {
                    return finding;
                }
            }
        }
        return Optional.empty();
    }
}
