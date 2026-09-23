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

import org.applecommander.capability.CapabilityProvider;
import org.applecommander.util.Container;
import org.applecommander.util.InformationProvider;

import java.util.List;

/**
 * A FileStore is a generic interface for all archive and filesystem objects.
 */
public interface FileStore extends CapabilityProvider, Container, InformationProvider {
    /**
     * The label is a mechanism to distinguish multiple file stores in a single file.
     * For instance, UniDOS has two file stores in the file, so it would have two
     * different labels such as "Disk 1" and "Disk 2".
     */
    String getLabel();
    /** All FileStores support a "root" directory that references all files. */
    Directory getRootDirectory();
    /** This is the path separator character. */
    // TODO there has to be a better / dynamic way to do this (applies to ProDOS, ShrinkIt only?)
    String getPathSeparator();
    /** Yields a list of columns to be used in this display mode. */
    List<DisplayColumn> getDisplayColumns();
}
