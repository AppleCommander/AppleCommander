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
package com.webcodepro.applecommander.storage.os.prodos;

import java.util.List;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.DiskFullException;

/**
 * Implement directory functionality.
 * <p>
 * Date Created: Mar 2, 2003
 * @author Rob Greene
 *
 * Changed at: Dec 1, 2017
 * @author Lisias Toledo
 */
public class ProdosDirectoryEntry extends ProdosFileEntry implements DirectoryEntry {
	private ProdosSubdirectoryHeader subdirectoryHeader;

	/**
	 * Constructor for ProdosDirectoryEntry.
	 */
	public ProdosDirectoryEntry(ProdosFormatDisk disk, int block, int offset,
			ProdosSubdirectoryHeader subdirectoryHeader) {
		super(disk, block, offset);
		this.subdirectoryHeader = subdirectoryHeader;
		subdirectoryHeader.setProdosDirectoryEntry(this);
	}

	/**
	 * Get the subdirectory header.
	 */
	public ProdosSubdirectoryHeader getSubdirectoryHeader() {
		return this.subdirectoryHeader;
	}

	/**
	 * Retrieve the list of files in this directory.
	 * Note that if this is not a directory, the return
	 * value should be null.  If this a directory, the
	 * return value should always be a list - a directory
	 * with 0 entries returns an empty list.
	 * @throws DiskException
	 */
	public List<ProdosFileEntry> getFiles() throws DiskException {
		return getDisk().getFiles(getSubdirectoryHeader().getFileEntryBlock());
	}

	/**
	 * Create a new FileEntry.
	 */
	public ProdosFileEntry createFile() throws DiskFullException {
		return getDisk().createFile(getSubdirectoryHeader());
	}

	/**
	 * Identify if additional directories can be created.  This
	 * may indicate that directories are not available to this
	 * operating system or simply that the disk image is "locked"
	 * to writing.
	 */
	public boolean canCreateDirectories() {
		return true;
	}
	
	/**
	 * Indicates if this disk image can create a file.
	 * If not, the reason may be as simple as it has not beem implemented
	 * to something specific about the disk.
	 */
	public boolean canCreateFile() {
		return true;
	}

	/**
	 * Create a new DirectoryEntry.
	 * @see com.webcodepro.applecommander.storage.DirectoryEntry#createDirectory(String)
	 */
	public DirectoryEntry createDirectory(String name) throws DiskFullException {
		return getDisk().createDirectory(getSubdirectoryHeader(), name);
	}
}
