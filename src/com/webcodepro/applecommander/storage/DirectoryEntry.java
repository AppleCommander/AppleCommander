/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2003 by Robert Greene
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
package com.webcodepro.applecommander.storage;

import java.util.List;

/**
 * Represents a directory on disk.  This could be the main
 * catalog or an actual directory.  (FormattedDisk implements
 * DirectoryEntry.)
 * <p>
 * Date Created: Mar 2, 2003
 * @author Rob Greene
 */
public interface DirectoryEntry {
	/**
	 * Retrieve the list of files in this directory.
	 * Note that if this is not a directory, the return
	 * value should be null.  If this a directory, the
	 * return value should always be a list - a directory
	 * with 0 entries returns an empty list.
	 */
	public List getFiles();

	/**
	 * Create a new FileEntry.
	 */
	public FileEntry createFile() throws DiskFullException;
	
	/**
	 * Create a new DirectoryEntry.
	 */
	public DirectoryEntry createDirectory(String name) throws DiskFullException;

	/**
	 * Identify if additional directories can be created.  This
	 * may indicate that directories are not available to this
	 * operating system or simply that the disk image is "locked"
	 * to writing.
	 */
	public boolean canCreateDirectories();
	
	/**
	 * Indicates if this disk image can create a file.
	 * If not, the reason may be as simple as it has not beem implemented
	 * to something specific about the disk.
	 */
	public boolean canCreateFile();

	/**
	 * Get the FormattedDisk associated with this DirectoryEntry.
	 * This is useful to interfaces that need to retrieve the associated
	 * disk.
	 */
	public FormattedDisk getFormattedDisk();
}
