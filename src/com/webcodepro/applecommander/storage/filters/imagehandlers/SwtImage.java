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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

/**
 * SwtImage is a specific implementation of AppleImage that handles all
 * SWT related coding surrounding image manipulation.
 * <p>
 * Date Created: Mar 25, 2003
 * @author Rob Greene
 */
public class SwtImage extends AppleImage {
	private ImageData imageData;
	/**
	 * Create SwtImage.  Verifies all (known) required classes are available
	 * as well as sets up the class.
	 */
	public SwtImage(int width, int height) throws ClassNotFoundException {
		super(new String[] { "BMP", "RLE", "JPEG", "ICO" });  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
		Class.forName("org.eclipse.swt.graphics.ImageLoader"); //$NON-NLS-1$
		Class.forName("org.eclipse.swt.graphics.ImageData"); //$NON-NLS-1$
		Class.forName("org.eclipse.swt.graphics.Image"); //$NON-NLS-1$
		Class.forName("org.eclipse.swt.SWT"); //$NON-NLS-1$
		// Gives better results than manually building the ImageData
		// object.  However, explicitly requires DLL in the path.
		imageData = new Image(null, width, height).getImageData();
	}
	/**
	 * Set a color point.
	 */
	public void setPoint(int x, int y, int color) {
		imageData.setPixel(x, y, color);
	}
	/**
	 * Get a color point.
	 */
	public int getPoint(int x, int y) {
		return imageData.getPixel(x,y);
	}
	/**
	 * Save the image.
	 */
	public void save(OutputStream outputStream) throws IOException {
		ImageLoader imageLoader = new ImageLoader();
		imageLoader.data = new ImageData[] { imageData };
		int format = SWT.IMAGE_PNG;
		if ("BMP".equals(getFileExtension())) { //$NON-NLS-1$
			format = SWT.IMAGE_BMP;
		} else if ("RLE".equals(getFileExtension())) { //$NON-NLS-1$
			format = SWT.IMAGE_BMP_RLE;
		} else if ("GIF".equals(getFileExtension())) { //$NON-NLS-1$
			format = SWT.IMAGE_GIF;
		} else if ("ICO".equals(getFileExtension())) { //$NON-NLS-1$
			format = SWT.IMAGE_ICO;
		} else if ("JPEG".equals(getFileExtension())) { //$NON-NLS-1$
			format = SWT.IMAGE_JPEG;
		} else if ("PNG".equals(getFileExtension())) { //$NON-NLS-1$
			format = SWT.IMAGE_PNG;
		}
		imageLoader.save(outputStream, format);
	}
	/**
	 * Return the width of the image.
	 */
	public int getWidth() {
		return imageData.width;
	}
	/**
	 * Return the height of the image.
	 */
	public int getHeight() {
		return imageData.height;
	}
}
