/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package org.applecommander.capability;

/// An enumeration of capabilities that may be available in various components.
public enum Capability {

    /// Indicates if a nibble device can encode data.
    ENCODE,

    /// Indicates if a nibble device can write a track.
    WRITE_TRACK,

    /// Indicates if a nibble device can format a track (write out markers, for example).
    FORMAT_TRACK,

    /// Indicates if this device can write a sector.
    WRITE_SECTOR,

    /// Indicates if this device can write a block.
    WRITE_BLOCK,

    /// Indicates if a `Source` can be saved. This can be impacted by how it was created or loaded.
    SAVE_SOURCE,

    /// Indicates if a `FileStore` can create files.
    CREATE_FILES,

    /// Indicates if a `FileStore` can delete files.
    DELETE_FILES,

    /// Indicates if a `FileStore` can write files.
    WRITE_FILES,

    /// Indicates if a `FileStore` supports (sub-)directories.
    SUPPORTS_DIRECTORIES,

    /// Indicates if a `FileStore` can create (sub-)directories.
    CREATE_DIRECTORIES,

    /// Indicates if this `FileStore` supports resource forks.
    SUPPORTS_RESOURCE_FORKS
}
