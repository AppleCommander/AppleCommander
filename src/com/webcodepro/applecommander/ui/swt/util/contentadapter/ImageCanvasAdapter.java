/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2004 by Robert Greene
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
package com.webcodepro.applecommander.ui.swt.util.contentadapter;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.printing.Printer;

import com.webcodepro.applecommander.ui.swt.util.ImageCanvas;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;

/**
 * Content-specific adapter for an ImageCanvas.
 * 
 * @author Rob Greene
 */
public class ImageCanvasAdapter implements ContentTypeAdapter {
	private ImageCanvas imageCanvas;
	private String printJobName;
	
	public ImageCanvasAdapter(ImageCanvas imageCanvas, String printJobName) {
		this.imageCanvas = imageCanvas;
		this.printJobName = printJobName;
	}
	
	public void print() {
		final Printer printer = SwtUtil.showPrintDialog(imageCanvas);
		if (printer == null) return;	// Print was cancelled
		new Thread(new Runnable() {
			public void run() {
				printer.startJob(getPrintJobName());
				printer.startPage();
				Point dpi = printer.getDPI();
				Image image = getImageCanvas().getImage();
				int imageWidth = image.getImageData().width;
				int imageHeight = image.getImageData().height;
				int printedWidth = imageWidth * (dpi.x / 96);
				int printedHeight = imageHeight * (dpi.y / 96);
				GC gc = new GC(printer);
				gc.drawImage(image,
					0, 0, imageWidth, imageHeight,
					0, 0, printedWidth, printedHeight);
				printer.endPage();
				printer.endJob();
				gc.dispose();
				printer.dispose();
			}
		}).start();
	}
	
	public void selectAll() {
		// N/A?
	}
	
	public void copy() {
		// TODO: Can SWT copy an image to the clipboard?
		//	Clipboard clipboard = new Clipboard(shell.getDisplay());
		//	String[] typeNames = clipboard.getAvailableTypeNames();
		// look at the typeNames - nothing that looks like an image?!
		//	clipboard.dispose();
	}
	
	protected ImageCanvas getImageCanvas() {
		return imageCanvas;
	}
	
	protected String getPrintJobName() {
		return printJobName;
	}
}