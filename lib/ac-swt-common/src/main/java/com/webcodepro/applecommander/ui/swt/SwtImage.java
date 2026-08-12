/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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
package com.webcodepro.applecommander.ui.swt;

import com.webcodepro.applecommander.storage.filters.imagehandlers.AppleImage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

import java.io.OutputStream;

/**
 * SwtImage is a specific implementation of AppleImage that handles all
 * SWT related coding surrounding image manipulation.
 * <p>
 * Date Created: Mar 25, 2003
 * @author Rob Greene
 */
public class SwtImage extends AppleImage {
	private final ImageData imageData;
	/**
	 * Create SwtImage.  Verifies all (known) required classes are available
	 * as well as sets up the class.
	 */
	public SwtImage(int width, int height) throws ClassNotFoundException {
		super(new String[] { "BMP", "RLE", "JPEG", "ICO" });
		Class.forName("org.eclipse.swt.graphics.ImageLoader");
		Class.forName("org.eclipse.swt.graphics.ImageData");
		Class.forName("org.eclipse.swt.graphics.Image");
		Class.forName("org.eclipse.swt.SWT");
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
	public void save(OutputStream outputStream) {
		ImageLoader imageLoader = new ImageLoader();
		imageLoader.data = new ImageData[] { imageData };
		int format = switch (getFileExtension()) {
			case "BMP" -> SWT.IMAGE_BMP;
			case "RLE" -> SWT.IMAGE_BMP_RLE;
			case "GIF" -> SWT.IMAGE_GIF;
			case "ICO" -> SWT.IMAGE_ICO;
			case "JPEG" -> SWT.IMAGE_JPEG;
			default -> SWT.IMAGE_PNG;
		};
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
