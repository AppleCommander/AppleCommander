/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003 by Robert Greene
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

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.GraphicsFileFilter;

import java.io.ByteArrayInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * View a particular files content.
 * <p>
 * Date created: Dec 7, 2003
 * @author: Rob Greene
 */
public class FileViewerWindow {
	private Shell parentShell;
	private ImageManager imageManager;
	
	private Shell shell;
	private FileEntry fileEntry;
	
	private ScrolledComposite content;
	private ToolBar toolBar;
	private ToolItem imageToolItem;
	private ToolItem hexDumpToolItem;
	private ToolItem printToolItem;

	/**
	 * Construct the file viewer window.
	 */
	public FileViewerWindow(Shell parentShell, FileEntry fileEntry, ImageManager imageManager) {
		this.parentShell = shell;
		this.fileEntry = fileEntry;
		this.imageManager = imageManager;
	}
	
	/**
	 * Setup the File Viewer window and display (open) it.
	 */
	public void open() {
		shell = new Shell(parentShell, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());
		shell.setImage(imageManager.get(ImageManager.ICON_DISK));
		shell.setText("File Viewer - " + fileEntry.getFilename());
		shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					dispose(event);
				}
			});

		Composite composite = new Composite(shell, SWT.NULL);
		GridLayout gridLayout = new GridLayout(1, false);
		composite.setLayout(gridLayout);
		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		createFileToolBar(composite, gridData);

		content = new ScrolledComposite(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		gridData = new GridData(GridData.FILL_BOTH);
		content.setLayoutData(gridData);
		
		FileFilter filter = fileEntry.getSuggestedFilter();
		Color red = new Color(shell.getDisplay(), 255, 0, 0); 
		if (filter instanceof GraphicsFileFilter) {
			byte[] imageBytes = filter.filter(fileEntry);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
			ImageLoader imageLoader = new ImageLoader();
			ImageData[] imageData = imageLoader.load(inputStream);
			final Image image = new Image(shell.getDisplay(), imageData[0]);

			GridLayout layout = new GridLayout();
			content.setLayout(layout);
			gridData = new GridData();
			gridData.widthHint = imageData[0].width;
			gridData.heightHint = imageData[0].height;
			ImageCanvas imageCanvas = new ImageCanvas(content, SWT.NONE, image, gridData);
			content.setContent(imageCanvas);
			content.setExpandHorizontal(true);
			content.setExpandVertical(true);
			content.setMinWidth(imageData[0].width);
			content.setMinHeight(imageData[0].height);

//			ImageCanvas imageCanvas = new ImageCanvas(content, SWT.BORDER, image, gridData);
//			imageCanvas.addListener(SWT.KeyUp, this);



//			Canvas canvas = new Canvas (content, SWT.NONE);
//			content.setContent(canvas);
//			canvas.addPaintListener (new PaintListener () {
//				public void paintControl (PaintEvent e) {
//					e.gc.drawImage (image, 0, 0);
//				}
//			});

//			Label label = new Label(content, SWT.NONE);
//			label.setImage(image);
//			content.setContent(label);
		} else {
			// Garbage:
			final Composite c1 = new Composite(content, SWT.NONE);
			content.setContent(c1);
			c1.setBackground(red);
			GridLayout layout = new GridLayout();
			layout.numColumns = 4;
			c1.setLayout(layout);
			Button b1 = new Button (c1, SWT.PUSH);
			b1.setText("first button");
			c1.setSize(c1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
		
		shell.open();
	}
	
	/**
	 * Dispose of all shared resources.
	 */
	private void dispose(DisposeEvent event) {
		System.gc();
	}

	/**
	 * Creates the FILE tab toolbar.
	 */
	private void createFileToolBar(Composite composite, Object layoutData) {
		toolBar = new ToolBar(composite, SWT.FLAT);
//		toolBar.addListener(SWT.KeyUp, createToolbarCommandHandler());
		if (layoutData != null) toolBar.setLayoutData(layoutData);

		imageToolItem = new ToolItem(toolBar, SWT.RADIO);
		imageToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_AS_IMAGE));
		imageToolItem.setText("Image");
		imageToolItem.setToolTipText("Displays file as an image");
		imageToolItem.setSelection(true);
		imageToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				//changeCurrentFormat(FormattedDisk.FILE_DISPLAY_STANDARD);
			}
		});

		hexDumpToolItem = new ToolItem(toolBar, SWT.RADIO);
		hexDumpToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_IN_HEX));
		hexDumpToolItem.setText("Hex Dump");
		hexDumpToolItem.setToolTipText("Displays file as a hex dump");
		hexDumpToolItem.setSelection(false);
		hexDumpToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				//changeCurrentFormat(FormattedDisk.FILE_DISPLAY_STANDARD);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem copy = new ToolItem(toolBar, SWT.PUSH);
		copy.setText("Copy");
		copy.setText("Copies selection to the clipboard");
		copy.setEnabled(false);

		new ToolItem(toolBar, SWT.SEPARATOR);
		
		printToolItem = new ToolItem(toolBar, SWT.PUSH);
		printToolItem.setImage(imageManager.get(ImageManager.ICON_PRINT_FILE));
		printToolItem.setText("Print");
		printToolItem.setToolTipText("Print contents...");
		printToolItem.setEnabled(true);
		printToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				//saveAs();
			}
		});

		toolBar.pack();
	}
}
