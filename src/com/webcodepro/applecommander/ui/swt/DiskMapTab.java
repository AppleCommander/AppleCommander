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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskUsage;

/**
 * Build the Disk Map tab for the Disk Window.
 * <p>
 * Date created: Nov 17, 2002 9:27:44 PM
 * @author Rob Greene
 */
public class DiskMapTab {
	private FormattedDisk disk;
	// used locally - not shared between windows; hopefully will
	// not be a resource drain!
	private Color freeFill;
	private Color usedFill;
	private Color black;
	private Color gray;
	/**
	 * Construct the DiskMapTab.
	 */
	public DiskMapTab(CTabFolder tabFolder, FormattedDisk disk) {
		this.disk = disk;

		// these items are reused; need to dispose of them when done!
		freeFill = new Color(tabFolder.getDisplay(), 100,200,100);
		usedFill = new Color(tabFolder.getDisplay(), 200,100,100);
		black = new Color(tabFolder.getDisplay(), 0,0,0);
		gray = new Color(tabFolder.getDisplay(), 50,50,50);
		
		createDiskMapTab(tabFolder);
	}
	/**
	 * Create the DISK MAP tab.
	 */
	protected void createDiskMapTab(CTabFolder tabFolder) {	
		CTabItem item = new CTabItem(tabFolder, SWT.NULL);
		if (disk.getLogicalDiskNumber() > 0) {
			item.setText("Disk Map #" + disk.getLogicalDiskNumber());
		} else {
			item.setText("Disk Map");
		}
		
		Canvas canvas = new Canvas(tabFolder, SWT.NULL);
		GridLayout grid = new GridLayout(2, false);
		canvas.setLayout(grid);
		item.setControl(canvas);
		
		String[] labels = disk.getBitmapLabels();

		// ROW #1 - title
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.heightHint = 20;
		Label title = new Label(canvas, SWT.LEFT);
		StringBuffer buf = new StringBuffer();
		if (labels.length == 1) {
			buf.append("This disk is organized by the ");
			buf.append(labels[0].toLowerCase());
			buf.append(". Therefore, no organization has been forced on the ");
			buf.append(labels[0].toLowerCase());
			buf.append(" layout.");
		} else {
			buf.append("This disk is organized by ");
			for (int i=0; i<labels.length; i++) {
				if (i > 0) buf.append(" and ");
				buf.append(labels[i].toLowerCase());
			}
			buf.append(", and this implies a rigid layout to the disk. ");
			buf.append("This will be reflected in the disk map.");
		}
		title.setText(buf.toString());
		title.setLayoutData(data);
		
		// ROW #2 - blank and horizontal ruler
		data = new GridData();
		data.heightHint = 20;
		data.widthHint = 20;
		Composite blank = new Composite(canvas, SWT.NULL);
		blank.setLayoutData(data);
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.heightHint = 20;
		Canvas ruler = new Canvas(canvas, SWT.NULL);
		ruler.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				paintHorizontalRuler(event);
			}
		});
		ruler.setLayoutData(data);
		
		// ROW #3 - vertical ruler and map
		data = new GridData(GridData.VERTICAL_ALIGN_FILL);
		data.widthHint = 20;
		ruler = new Canvas(canvas, SWT.NULL);
		ruler.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				paintVerticalRuler(event);
			}
		});
		ruler.setLayoutData(data);
		data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		Canvas map = new Canvas(canvas, SWT.BORDER);
		map.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				paintMap(event);
			}
		});
		map.setLayoutData(data);
		
		// ROW #5
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 20;
		data.horizontalSpan = 2;
		Canvas legend = new Canvas(canvas, SWT.NULL);
		legend.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				paintLegend(event);
			}
		});
		legend.setLayoutData(data);
	}
	/**
	 * Dispose of resources.
	 */
	public void dispose() {
		freeFill.dispose();
		usedFill.dispose();
		black.dispose();
		gray.dispose();
	}
	/**
	 * Handle paint requests for horizontal ruler.
	 */
	private void paintHorizontalRuler(PaintEvent event) {
		String label = (disk.getBitmapLabels()[0] + "s").toUpperCase();
		Canvas canvas = (Canvas) event.widget;
		Rectangle area = canvas.getClientArea();
		event.gc.drawLine(area.x, area.y + area.height/2, area.x + area.width, area.y + area.height/2);
		Point size = event.gc.textExtent(label);
		event.gc.drawString(label, area.x + area.width/2 - size.x, area.y + area.height/2 - size.y/2);
	}
	/**
	 * Handle paint requests for vertical ruler.
	 */
	private void paintVerticalRuler(PaintEvent event) {
		String label = (disk.getBitmapLabels()[0] + "s").toUpperCase();
		if (disk.getBitmapLabels().length == 2) {
			label = (disk.getBitmapLabels()[1] + "s").toUpperCase();
		}
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<label.length(); i++) {
			if (i>0) buf.append("\n");
			buf.append(label.charAt(i));
		}
		label = buf.toString();
		Canvas canvas = (Canvas) event.widget;
		Rectangle area = canvas.getClientArea();
		event.gc.drawLine(area.x + area.width/2, area.y, area.x + area.width/2, area.y + area.height);
		Point size = event.gc.textExtent(label);
		event.gc.drawText(label, area.x + area.width/2 - size.x/2, area.y + area.height/2 - size.y/2);
	}
	/**
	 * Handle paint requests for disk map.
	 */
	private void paintMap(PaintEvent event) {
		if (disk.getDiskUsage() == null) {
			paintNoMap(event);
		} else if (disk.getBitmapDimensions() == null) {
			paintBlockMap(event);
		} else {
			paintSectorMap(event);
		}
	}
	/**
	 * Handle paint requests for legend.
	 */
	private void paintLegend(PaintEvent event) {
		Color background = event.gc.getBackground();
		Canvas canvas = (Canvas) event.widget;

		int height = event.gc.getFontMetrics().getHeight();
		String freeText = " = Free";
		String usedText = " = Used";
		int padding = 50;	// space between items
		
		int totalWidth =
			(height + 5) * 2		// free/used box
			+ event.gc.textExtent(freeText).x
			+ event.gc.textExtent(usedText).x;
			
		int offset = canvas.getClientArea().width / 2 - totalWidth;
		
		Rectangle box = new Rectangle(offset, 0, height, height);
		drawBox(box, event.gc, freeFill, black, gray);
		offset+= height;
		event.gc.setBackground(background);
		event.gc.drawText(freeText, offset, 0);
		offset+= event.gc.textExtent(freeText).x;

		offset+= padding;
		box = new Rectangle(offset, 0, height, height);
		drawBox(box, event.gc, usedFill, black, gray);
		offset+= height;
		event.gc.setBackground(background);
		event.gc.drawText(" = Used", offset, 0);
	}
	/**
	 * Display message to user regarding no disk map being available.
	 */
	private void paintNoMap(PaintEvent event) {
		event.gc.drawString("A disk map is unavailable.", 0,  0);
	}
	/**
	 * Paint a track/sector map.
	 */	
	private void paintSectorMap(PaintEvent event) {
		int[] dimensions = disk.getBitmapDimensions();
		int ydim = dimensions[1];
		int xdim = dimensions[0];
		
		paintDiskMap(xdim, ydim, event);
	}
	/**
	 * Paint a block map.
	 */	
	private void paintBlockMap(PaintEvent event) {
		Canvas canvas = (Canvas) event.widget;
		Rectangle area = canvas.getClientArea();

		double blocks = disk.getBitmapLength();
		double width = area.width;
		double height = area.height;
		double factor = Math.sqrt(blocks / (width * height));
		int xdim = (int) (width * factor + 0.5);
		int ydim = (int) (height * factor + 0.5);
		if (xdim * ydim < blocks) {
			xdim++;
		}
		if (xdim * ydim < blocks) {
			ydim++;
		}
		
		paintDiskMap(xdim, ydim, event);
	}
	/**
	 * Paint a map with the given dimensions.
	 */
	private void paintDiskMap(int xdim, int ydim, PaintEvent event) {
		Canvas canvas = (Canvas) event.widget;
		Rectangle area = canvas.getClientArea();
		area.width-= 2;
		area.height-= 2;

		int[] ypos = new int[ydim + 1];
		for (int i=0; i<ydim; i++) {
			ypos[i] = (i * area.height) / ydim + 1;
		}
		ypos[ydim] = area.height;
		int[] xpos = new int[xdim + 1];
		for (int i=0; i<xdim; i++) {
			xpos[i] = (i * area.width) / xdim + 1;
		}
		xpos[xdim] = area.width;
		
		Image image = new Image(canvas.getDisplay(), area);
		GC gc = new GC(image);
		int x = 0;
		int y = 0;
		DiskUsage usage = disk.getDiskUsage();
		for (x=0; x<xdim && usage.hasNext(); x++) {
			for (y=0; y<ydim && usage.hasNext(); y++) {
				usage.next();
				boolean free = usage.isFree();
				Rectangle box = new Rectangle(xpos[x], ypos[y], 
						xpos[x+1]-xpos[x], ypos[y+1]-ypos[y]);
				drawBox(box, gc, free ? freeFill : usedFill, black, gray);
			}
		}
		event.gc.drawImage(image, 0, 0);
		gc.dispose();
		image.dispose();
	}
	/**
	 * Draw a box on the screen.  The shadowed box is only drawn if there is
	 * enough space within the box; otherwise, the box is just filled in with
	 * the fill color.  Additionally, drawBox ensures that a square is drawn.
	 */
	protected void drawBox(Rectangle box, GC gc, Color fill, Color outline, Color shadow) {
		if (box.width >= 10 && box.height >= 10) {
			// square the rectangle shape:
			int size = Math.min(box.height, box.width);
			box.height = size + ((box.height - size) / 2);
			box.width = size + ((box.width - size) / 2);
			// offset internal box:
			box.x+= 2;
			box.y+= 2;
			box.width-= 5;
			box.height-= 5;
			// draw!
			gc.setBackground(shadow);
			gc.fillRectangle(box);
			box.x-= 2;
			box.y-= 2;
			gc.setBackground(fill);
			gc.fillRectangle(box);
			gc.setForeground(outline);
			gc.drawRectangle(box);
		} else {
			// just fill:
			gc.setBackground(fill);
			gc.fillRectangle(box);
		}
	}
}
