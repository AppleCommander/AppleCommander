/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002 by Robert Greene
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
 * Represents a file entry on disk - not the data.
 * <p>
 * Date created: Oct 4, 2002 4:46:42 PM
 * @author Rob Greene
 */
public interface FileEntry {
	/**
	 * Return the name of this file.
	 */
	public String getFilename();
	
	/**
	 * Set the name of this file.
	 */
	public void setFilename(String filename);
	
	/**
	 * Return the filetype of this file.
	 * This will be OS specific.
	 */
	public String getFiletype();
	
	/**
	 * Set the filetype.
	 */
	public void setFiletype(String filetype);
	
	/**
	 * Identify if this file is locked.
	 */
	public boolean isLocked();
	
	/**
	 * Set the lock indicator.
	 */
	public void setLocked(boolean lock);
	
	/**
	 * Compute the size of this file (in bytes).
	 */
	public int getSize();
	
	/**
	 * Identify if this is a directory file.
	 */
	public boolean isDirectory();
	
	/**
	 * Identify if this file has been deleted.
	 */
	public boolean isDeleted();
	
	/**
	 * Delete the file.
	 */
	public void delete();

	/**
	 * Get the standard file column header information.
	 * This default implementation is intended only for standard mode.
	 * displayMode is specified in FormattedDisk.
	 */
	public List getFileColumnData(int displayMode);
	
	/**
	 * Get file data.  This handles any operating-system specific issues.
	 * Specifically, DOS 3.3 places address and length into binary files
	 * and length into Applesoft files.
	 */
	public byte[] getFileData();
	
	/**
	 * Set file data.  This, essentially, is saving data to disk using this
	 * file entry.
	 */
	public void setFileData(byte[] data) throws DiskFullException;
	
	/**
	 * Get the suggested FileFilter.  This appears to be operating system
	 * specific, so each operating system needs to implement some manner
	 * of guessing the appropriate filter.
	 */
	public FileFilter getSuggestedFilter();
	
	/**
	 * Get the FormattedDisk associated with this FileEntry.
	 * This is useful to interfaces that need to retrieve the associated
	 * disk.
	 */
	public FormattedDisk getFormattedDisk();
	
	/**
	 * Return the maximum filename length.
	 */
	public int getMaximumFilenameLength();
	
	/**
	 * Indicates if this filetype requires an address component.
	 * Note that the FormattedDisk also has this method - normally,
	 * this will defer to the method on FormattedDisk, as it will be
	 * more generic.
	 */
	public abstract boolean needsAddress();
	
	/**
	 * Set the address that this file loads at.
	 */
	public abstract void setAddress(int address);
	
	/**
	 * Indicates that this filetype can be compiled.
	 * Compile is a somewhat arbitrary term, as the code may be
	 * assembled.
	 * NOTE:  The current assumption is that the filetype is
	 * AppleSoft.  This should be updated to include Integer BASIC,
	 * assembly, and potentially other languages.
	 */
	public abstract boolean canCompile();
}
