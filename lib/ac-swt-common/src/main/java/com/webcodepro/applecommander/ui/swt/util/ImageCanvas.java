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
package com.webcodepro.applecommander.ui.swt.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Displays an image.
 * <p>
 * Date created: Nov 7, 2002 9:28:21 PM
 * @author Rob Greene
 */
public class ImageCanvas extends Canvas implements PaintListener {
	private final Image image;
	private final int width;
	private final int height;
	/**
	 * Constructor for ImageCanvas.
	 */
	public ImageCanvas(Composite parent, int style, Image image, Object layoutData) {
		this(parent, style, image, layoutData, 1);
	}
	public ImageCanvas(Composite parent, int style, Image image, Object layoutData, int multiplier) {
		super(parent, style | SWT.SHELL_TRIM | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
		this.image = image;
		this.width = image.getBounds().width * multiplier;
		this.height = image.getBounds().height * multiplier;
		setLayoutData(layoutData);
		addPaintListener(this);
	}
	/**
	 * Handle paint events.
	 */
	public void paintControl(PaintEvent event) {
		GC gc = event.gc;
		gc.drawImage(image, 0, 0, width, height);
		Rectangle client = getClientArea();
		int marginWidth = client.width - width;
		if (marginWidth > 0) {
			gc.fillRectangle(width, 0, marginWidth, client.height);
		}
		int marginHeight = client.height - height;
		if (marginHeight > 0) {
			gc.fillRectangle(0, height, client.width, marginHeight);
		}
	}
	/**
	 * Get the Image.
	 */
	public Image getImage() {
		return image;
	}
}
