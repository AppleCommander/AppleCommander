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
package com.webcodepro.applecommander.ui.swt.filteradapter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.ui.swt.FileViewerWindow;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.ContentTypeAdapter;

/**
 * Represents a visual adapter for a FileFilter.  Generally, the display method is the
 * only variance between the many FileFilters available.
 * 
 * @author Rob Greene
 */
public abstract class FilterAdapter {
	private final FileViewerWindow window;
	private Image image;
	private String text;
	private String toolTipText;
	private ToolItem toolItem;
	private boolean nativeSelected = true;
	private boolean hexSelected = false;
	private boolean dumpSelected = false;
	
	public FilterAdapter(FileViewerWindow window, String text, String toolTipText, 
			Image image) {
		this.text = text;
		this.window = window;
		this.toolTipText = toolTipText;
		this.image = image;
		setNativeSelected();
	}

	public abstract void display();

	public void dispose() {
		// nothing to dispose
	}

	public ToolItem create(ToolBar toolBar) {
		if (toolItem == null) {
			toolItem = new ToolItem(toolBar, SWT.RADIO);
			toolItem.setImage(getImage());
			toolItem.setText(getText());
			toolItem.setToolTipText(getToolTipText());
			toolItem.setSelection(false); 
			toolItem.addSelectionListener(new SelectionAdapter () {
				public void widgetSelected(SelectionEvent e) {
					display();
					getWindow().setFilterToolItemSelection(
						isNativeSelected(), isHexSelected(), isDumpSelected());
				}
			});
		}
		return toolItem;
	}

	protected Image getImage() {
		return image;
	}
	protected String getText() {
		return text;
	}
	protected String getToolTipText() {
		return toolTipText;
	}
	protected FileFilter getFileFilter() {
		return window.getFileFilter();
	}
	protected FileEntry getFileEntry() {
		return window.getFileEntry();
	}
	protected ToolItem getCopyToolItem() {
		return window.getCopyToolItem();
	}
	protected ScrolledComposite getComposite() {
		return window.getComposite();
	}
	protected void setContentTypeAdapter(ContentTypeAdapter adapter) {
		window.setContentTypeAdapter(adapter);
	}
	protected Font getCourierFont() {
		return window.getCourierFont();
	}
	protected Listener getToolbarCommandHandler() {
		return window.createToolbarCommandHandler();
	}
	protected ToolItem getToolItem() {
		return toolItem;
	}
	protected Color getBlackColor() {
		return window.getBlackColor();
	}
	protected Color getGreenColor() {
		return window.getGreenColor();
	}
	protected Color getBlueColor() {
		return window.getBlueColor();
	}
	public void setDumpSelected() {
		nativeSelected = false;
		hexSelected = false;
		dumpSelected = true;
	}
	public void setHexSelected() {
		nativeSelected = false;
		hexSelected = true;
		dumpSelected = false;
	}
	public void setNativeSelected() {
		nativeSelected = true;
		hexSelected = false;
		dumpSelected = false;
	}
	protected boolean isDumpSelected() {
		return dumpSelected;
	}
	protected boolean isHexSelected() {
		return hexSelected;
	}
	protected boolean isNativeSelected() {
		return nativeSelected;
	}
	protected FileViewerWindow getWindow() {
		return window;
	}
}