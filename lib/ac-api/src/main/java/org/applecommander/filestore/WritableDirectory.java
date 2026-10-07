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

/// A WritableDirectory is a Directory that allows general directory modification.
/// Since not all directories can perform all tasks, check the capability first,
/// otherwise an exception will be generated.
public interface WritableDirectory extends Directory {

    /// Create a file. By definition, any file created is a `WritableFileEntry`.
    /// @see Capability#CREATE_FILES
    default WritableFileEntry createFile(String fileName) {
        throw new UnsupportedOperationException("unable to create files");
    }

    /// Create a file given the supplied ProDOS attributes. This is expected to handle
    /// setting all appropriate `FileEntry` attributes that align. File type should be
    /// converted, dates applied, etc.
    /// @see Capability#CREATE_FILES
    default WritableFileEntry createFrom(ProdosAttributes prodosAttributes) {
        throw new UnsupportedOperationException("unable to create files from ProDOS attributes");
    }

    /// Create a directory. By definition, any directory create is a `WritableDirectory`.
    /// @see Capability#CREATE_DIRECTORIES
    default WritableDirectory createDirectory(String directoryName) {
        throw new UnsupportedOperationException("unable to create directories");
    }

    /// Delete a file.
    /// @see Capability#DELETE_FILES
    default void deleteFile(FileEntry fileEntry) {
        throw new UnsupportedOperationException("unable to delete files");
    }
}
