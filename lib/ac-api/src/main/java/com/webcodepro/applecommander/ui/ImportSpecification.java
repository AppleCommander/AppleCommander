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
package com.webcodepro.applecommander.ui;

/**
 * This class represents the Import specification for a single
 * file.  It is generic, and could/should be used as a common
 * user interface bean.
 * <br>
 * Created on Jan 25, 2003.
 * @author Rob Greene
 */
public class ImportSpecification {
	private String sourceFilename;
	private String targetFilename;
	private String filetype;
	private int address;
	private boolean rawFileImport;
	/**
	 * Create the ImportSpecification with default values.
	 */
	public ImportSpecification(String sourceFilename, String targetFilename) {
		this.sourceFilename = sourceFilename;
		this.targetFilename = targetFilename;
	}
	/**
	 * Create the ImportSpecification with default values.
	 */
	public ImportSpecification(String sourceFilename, String targetFilename,
			String filetype) {
		this.sourceFilename = sourceFilename;
		this.targetFilename = targetFilename;
		this.filetype = filetype;
	}
	/**
	 * Returns the address.
	 * @return int
	 */
	public int getAddress() {
		return address;
	}
	/**
	 * Indicates if a filetype has been set.
	 */
	public boolean hasFiletype() {
		return filetype != null && filetype.length() > 0;
	}
	/**
	 * Returns the filetype.
	 * @return String
	 */
	public String getFiletype() {
		return filetype;
	}
	/**
	 * Returns the sourceFilename.
	 * @return String
	 */
	public String getSourceFilename() {
		return sourceFilename;
	}
	/**
	 * Returns the targetFilename.
	 * @return String
	 */
	public String getTargetFilename() {
		return targetFilename;
	}
	/**
	 * Sets the address.
	 * @param address The address to set
	 */
	public void setAddress(int address) {
		this.address = address;
	}
	/**
	 * Sets the filetype.
	 * @param filetype The filetype to set
	 */
	public void setFiletype(String filetype) {
		this.filetype = filetype;
	}
	/**
	 * Sets the targetFilename.
	 * @param targetFilename The targetFilename to set
	 */
	public void setTargetFilename(String targetFilename) {
		this.targetFilename = targetFilename;
	}
	/**
	 * Answers true if the file should be imported as a raw file -
	 * that is, no doctoring of the file bytes at all.  Particularly
	 * important in operating systems like DOS 3.3.  
	 */
	public boolean isRawFileImport() {
		return rawFileImport;
	}
	/**
	 * Sets raw file import flag.
	 */
	public void setRawFileImport(boolean rawFileImport) {
		this.rawFileImport = rawFileImport;
	}
}
