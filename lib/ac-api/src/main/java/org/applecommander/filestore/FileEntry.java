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

import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

/// A `FileEntry` represents a single file in a file store.
/// The generic interface is intentionally spare to keep useless methods to a minimum.
public interface FileEntry extends Container {

    /// The parent `Directory`, if applicable. Can return `null` if this is the root directory.
    Directory getParent();

    /// Indicates if this `FileEntry` is deleted.
    boolean isDeleted();

    /// Returns the `FileStore` that this `Entry` originates from.
    FileStore getFileStore();

    /// Returns the name of this entry: either a directory name or a file name.
    /// This does not contain any directory components -- strictly the name of this entry.
    /// Note that depending on context, this may be a computed field.
    String getName();

    /// Returns the size, in bytes, of this item.
    /// It may be approximate (based off a sector count, for instance).
    long getSize();

    /// Return the file's data.
    /// This does not include any metadata that may be embedded with the file.
    DataBuffer getDataFork();

    /// Return the resource fork data. If there is no resource fork (or a resource fork is not supported),
    /// then this returns an empty `Optional`.
    default Optional<DataBuffer> getResourceFork() {
        return Optional.empty();
    }

    /// Return the type of content, if known. It should never return null; use UNKNOWN if not known.
    /// @see ContentType
    default ContentType getContentType() {
        return ContentType.UNKNOWN;
    }
}
