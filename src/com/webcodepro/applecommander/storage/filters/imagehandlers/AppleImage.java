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
import java.lang.reflect.Constructor;

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
	 * Create a specific instance of AppleImage.  This has been coded
	 * using Reflection to ease native compilation for the most part.
	 */
	public static AppleImage create(int width, int height) {
		String[] classes = {
			"ImageIoImage", "SunJpegImage", "SwtImage" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Class[] constructorArgClasses = new Class[] {
			int.class, int.class };
		Object[] constructorArgs = new Object[] {
			new Integer(width), new Integer(height) };
		for (int i=0; i<classes.length; i++) {
			try {
				Class appleImageClass = Class.forName(
					"com.webcodepro.applecommander.storage.filters.imagehandlers."  //$NON-NLS-1$
					+ classes[i]);
				Constructor constructor = 
					appleImageClass.getConstructor(constructorArgClasses);
				AppleImage appleImage = (AppleImage) 
					constructor.newInstance(constructorArgs);
				return appleImage;
			} catch (Exception ignored) {
				// There are multiple exceptions that can be thrown here.
				// For the most part, this is expected and simply means that
				// the image handler is not available on the platform.
			}
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
	 * Get a color point.
	 */
	public abstract int getPoint(int x, int y);
	/**
	 * Save the image.
	 */
	public abstract void save(OutputStream outputStream) throws IOException;
	/**
	 * Return the width of the image.
	 */
	public abstract int getWidth();
	/**
	 * Return the height of the image.
	 */
	public abstract int getHeight();
}
