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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;

/**
 * SwtImage is a specific implementation of AppleImage that handles all
 * SWT related coding surrounding image manipulation.
 * <p>
 * Date Created: Mar 25, 2003
 * @author Rob Greene
 */
public class SwtImage extends AppleImage {
	private PaletteData paletteData;
	private ImageData imageData;
	/**
	 * Create SwtImage.  Verifies all (known) required classes are available
	 * as well as sets up the class.
	 */
	public SwtImage(int width, int height) throws ClassNotFoundException {
		// FIXME: Only able to get BMP functioning.  JPEG images come out
		// black; PNG throw a "not implemented"; GIF throws "unsupported
		// color depth".
		super(new String[] { "BMP" });
		Class.forName("org.eclipse.swt.graphics.ImageLoader");
		Class.forName("org.eclipse.swt.graphics.ImageData");
		Class.forName("org.eclipse.swt.graphics.PaletteData");
		paletteData = new PaletteData(0, 0, 0);
		imageData = new ImageData(width, height, 24, paletteData);
	}
	/**
	 * Set a color point.
	 */
	public void setPoint(int x, int y, int color) {
		imageData.setPixel(x, y, color);
	}
	/**
	 * Save the image.
	 */
	public void save(OutputStream outputStream) throws IOException {
		ImageLoader imageLoader = new ImageLoader();
		imageLoader.data = new ImageData[] { imageData };
		int format = SWT.IMAGE_PNG;
		if ("BMP".equals(getFileExtension())) {
			format = SWT.IMAGE_BMP;
		} else if ("GIF".equals(getFileExtension())) {
			format = SWT.IMAGE_GIF;
		} else if ("JPEG".equals(getFileExtension())) {
			format = SWT.IMAGE_JPEG;
		}
		imageLoader.save(outputStream, format);
	}
}
