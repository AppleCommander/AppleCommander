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
package org.applecommander.hint;

/**
 * Helpful hints that indicate something "interesting" (to code at least) about this <em>thing</em>.
 * @see HintProvider
 */
public enum Hint {
    /** "Nibble" sector order is really physical sector order. */
    NIBBLE_SECTOR_ORDER,
    /** Sectors are in DOS 3.3 sector order. */
    DOS_SECTOR_ORDER,
    /**
     * Sectors are organized in ProDOS sector order. Typically, this is initially read as a ProDOS block
     * and (if needed), mapped back to DOS tracks and sectors.
     */
    PRODOS_BLOCK_ORDER,
    /** Temporary flag to indicate this image was automagically extracted from a SHK or SDK image. */
    ORIGIN_SHRINKIT,
    /** Origin for this image was a 2IMG image. */
    UNIVERSAL_DISK_IMAGE,
    /** Origin for this image was a DiskCopy image. */
    DISK_COPY_IMAGE,
    /**
     * This flag is used to indicate a protected disk. Meaning factories may need to search a bit more
     * for disk markers.
     */
    NONSTANDARD_NIBBLE_IMAGE
}
