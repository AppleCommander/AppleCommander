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
package com.webcodepro.applecommander.storage.filters.imagehandlers;

import java.io.IOException;
import java.io.OutputStream;

/**
 * AppleImage is an abstract class that represents a generic interface
 * for handing graphic images.  This abstraction is needed because not
 * all graphic routines use BufferedImage.  AppleImage itself acts as
 * a factory for creating "the best" AppleImage subclass.
 * <p>
 * Date Created: Mar 25, 2003
 * @author Rob Greene
 */
public abstract class AppleImage {
	/**
	 * The specific file extension to use.
	 */
	private String fileExtension;
	/**
	 * All available image formats.  This is instance specific since
	 * (in theory) there could be different AppleImages being used.
	 */
	private String[] availableExtensions;
	/**
	 * Create a specific instance of AppleImage.
	 */
	public static AppleImage create(int width, int height) {
		try {
			return new ImageIoImage(width, height);
		} catch (ClassNotFoundException ignored) {
		}
		try {
			return new SunJpegImage(width, height);
		} catch (ClassNotFoundException ignored) {
		}
		try {
			return new SwtImage(width, height);
		} catch (ClassNotFoundException ignored) {
		}
		return null;
	}
	/**
	 * Construct AppleImage.
	 */
	public AppleImage(String[] availableExtensions) {
		setAvailableExtensions(availableExtensions);
		setFileExtension(getDefaultExtension());
	}
	/**
	 * Get the default file extension.  Used to "prime" the graphics
	 * FileFilter.
	 */
	public String getDefaultExtension() {
		return getAvailableExtensions()[0];
	}
	/**
	 * Return a list of file extensions that this specific implementation
	 * can handle.
	 */
	public String[] getAvailableExtensions() {
		return this.availableExtensions;
	}
	/**
	 * Set the list of file extensions that can be handled.
	 */
	protected void setAvailableExtensions(String[] availableExtensions) {
		this.availableExtensions = availableExtensions;
	}
	/**
	 * Returns the file extension.
	 */
	public String getFileExtension() {
		return fileExtension;
	}
	/**
	 * Sets the file extension.
	 */
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}
	/**
	 * Set a color point.
	 */
	public abstract void setPoint(int x, int y, int color);
	/**
	 * Save the image.
	 */
	public abstract void save(OutputStream outputStream) throws IOException;
}
