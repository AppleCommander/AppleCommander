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
package com.webcodepro.applecommander.ui.swt;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Displays an image.
 * <p>
 * Date created: Nov 7, 2002 9:28:21 PM
 * @author: Rob Greene
 */
public class ImageCanvas extends Canvas implements PaintListener {
	private Image image;
	/**
	 * Constructor for ImageCanvas.
	 */
	public ImageCanvas(Composite parent, int style, Image image, Object layoutData) {
		super(parent, style);
		this.image = image;
		setLayoutData(layoutData);
		setSize(image.getImageData().width, image.getImageData().height);
		addPaintListener(this);
	}
	/**
	 * Handle paint events.
	 */
	public void paintControl(PaintEvent event) {
		event.gc.drawImage(
			image,
			0,
			0,
			image.getImageData().width,
			image.getImageData().height,
			0,
			0,
			image.getImageData().width,
			image.getImageData().height);
	}
	/**
	 * Get the Image.
	 */
	public Image getImage() {
		return image;
	}
}
