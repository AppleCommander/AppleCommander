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

/// ContentType are the types of files that AppleCommander recognizes.
/// The ultimate intent is that there will be filters or viewers for them.
/// Part of the management can be deferred to the `FileMagic` class.
///
/// Each `ContentType` also has a (much more generic) `Subtype` that declares
/// what type of content this is. This is intended to allow a simpler choice
/// for viewers. (Instead of 10+ checks for graphics types, every graphics
/// type will indicate by teh subtype that it is graphics, for instance).
///
/// @see org.applecommander.util.FileMagic
/// @see Subtype
public enum ContentType {
    /// The content type is unknown.
    UNKNOWN(Subtype.NONE),
    /// Disk images types.
    DISK_IMAGE(Subtype.FILE_STORE),
    /// Archive image types.
    ARCHIVE_IMAGE(Subtype.FILE_STORE),
    /// High ASCII text file.
    HIGH_ASCII(Subtype.TEXT),
    /// Low (normal) ASCII. Aka, modern text files.
    LOW_ASCII(Subtype.TEXT),
    /// Pascal TEXT file.
    PASCAL_TEXT(Subtype.TEXT);

    public final Subtype subtype;

    private ContentType(Subtype subtype) {
        this.subtype = subtype;
    }

    /// Subtype indicates a broad type of this file with the intent of
    /// identifying the viewer and/or possible interfaces that can be
    /// discovered to translate to a modern format or view.
    public enum Subtype {
        /// We have no details on this particular file.
        NONE,
        /// This file should be viewed as a `FileStore`. Useful for the images that contain other
        /// images (lots of SHK or ZIP files).
        FILE_STORE,
        /// Represents a text format, irrespective of the actual formatting of the text file.
        TEXT
    }
}
