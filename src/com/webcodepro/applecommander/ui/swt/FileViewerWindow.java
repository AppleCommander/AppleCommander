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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksDataBaseFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksSpreadSheetFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.storage.filters.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.filters.AssemblySourceFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.filters.PascalTextFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.ui.swt.filteradapter.ApplesoftFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.FilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.GraphicsFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.HexFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.RawDumpFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.TextFilterAdapter;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.ContentTypeAdapter;

/**
 * View a particular files content.
 * <p>
 * Date created: Dec 7, 2003
 * @author: Rob Greene
 */
public class FileViewerWindow {
	private static final char CTRL_A = 'A' - '@';
	private static final char CTRL_P = 'P' - '@';
	private static final char CTRL_C = 'C' - '@';

	private Shell parentShell;
	private ImageManager imageManager;
	
	private Shell shell;
	private FileEntry fileEntry;
	private FileFilter nativeFilter;
	
	private ScrolledComposite content;
	private ToolBar toolBar;
	private ToolItem nativeToolItem;
	private ToolItem hexDumpToolItem;
	private ToolItem rawDumpToolItem;
	private ToolItem printToolItem;
	private ToolItem copyToolItem;
	
	private Font courier;
	private Color black;
	private Color blue;
	private Color green;
	
	private ContentTypeAdapter contentTypeAdapter;
	private Map nativeFilterAdapterMap;
	private FilterAdapter nativeFilterAdapter;
	private FilterAdapter hexFilterAdapter;
	private FilterAdapter rawDumpFilterAdapter;
	
	/**
	 * Construct the file viewer window.
	 */
	public FileViewerWindow(Shell parentShell, FileEntry fileEntry, ImageManager imageManager) {
		this(parentShell, fileEntry, imageManager, fileEntry.getSuggestedFilter());
	}

	/**
	 * Construct the file viewer window.
	 */
	public FileViewerWindow(Shell parentShell, FileEntry fileEntry, ImageManager imageManager, FileFilter nativeFilter) {
		this.parentShell = shell;
		this.fileEntry = fileEntry;
		this.imageManager = imageManager;
		this.nativeFilter = nativeFilter;
		
		createFilterAdapterMap();
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
		createToolBar(composite, gridData);

		content = new ScrolledComposite(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		gridData = new GridData(GridData.FILL_BOTH);
		content.setLayoutData(gridData);
		content.addListener(SWT.KeyUp, createToolbarCommandHandler());
		
		courier = new Font(shell.getDisplay(), "Courier", 10, SWT.NORMAL);
		black = new Color(shell.getDisplay(), 0, 0, 0);
		blue = new Color(shell.getDisplay(), 0, 0, 192);
		green = new Color(shell.getDisplay(), 0, 192, 0);
		
		nativeFilterAdapter.display();
		
		shell.open();
		SwtUtil.setupPagingInformation(content);
	}

	/**
	 * Setup all possible specialized FilterAdapters.
	 */	
	protected void createFilterAdapterMap() {
		nativeFilterAdapterMap = new HashMap();
		
		nativeFilterAdapterMap.put(ApplesoftFileFilter.class, 
			new ApplesoftFilterAdapter(this, "Applesoft", 
				"Displays file as an Applesoft BASIC program (F2)", 
				imageManager.get(ImageManager.ICON_VIEW_AS_BASIC_PROGRAM)
			));
		nativeFilterAdapterMap.put(AppleWorksDataBaseFileFilter.class, 
			new TextFilterAdapter(this, "Database", 
				"Displays file as a database file (F2)", 
				imageManager.get(ImageManager.ICON_VIEW_AS_DATABASE)
			));
		nativeFilterAdapterMap.put(AppleWorksSpreadSheetFileFilter.class, 
			new TextFilterAdapter(this, "Spreadsheet", 
				"Displays file as a spreadsheet file (F2)", 
				imageManager.get(ImageManager.ICON_VIEW_AS_SPREADSHEET)
			));
		nativeFilterAdapterMap.put(AppleWorksWordProcessorFileFilter.class, 
			new TextFilterAdapter(this, "Wordprocessor", 
				"Displays file as a wordprocessor file (F2)", 
				imageManager.get(ImageManager.ICON_VIEW_AS_WORDPROCESSOR)
			));
		nativeFilterAdapterMap.put(AssemblySourceFileFilter.class, 
			new TextFilterAdapter(this, "Assembly", 
				"Displays file as assembly source file (F2)", 
				imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE)
			));
		nativeFilterAdapterMap.put(GraphicsFileFilter.class, 
			new GraphicsFilterAdapter(this, "Image", 
				"Displays file as an image (F2)", 
				imageManager.get(ImageManager.ICON_VIEW_AS_IMAGE)
			));
		nativeFilterAdapterMap.put(IntegerBasicFileFilter.class, 
			new TextFilterAdapter(this, "Integer BASIC", 
				"Displays file as an Integer BASIC program (F2)", 
				imageManager.get(ImageManager.ICON_VIEW_AS_BASIC_PROGRAM)
			));
		nativeFilterAdapterMap.put(PascalTextFileFilter.class, 
			new TextFilterAdapter(this, "Pascal Text", 
				"Displays file as Pascal text file (F2)", 
				imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE)
			));
		nativeFilterAdapterMap.put(TextFileFilter.class,
			new TextFilterAdapter(this, "Text",
				"Displays file as a text file (F2)",
				imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE)
			));
	}
	
	/**
	 * Dispose of all shared resources.
	 */
	private void dispose(DisposeEvent event) {
		courier.dispose();
		black.dispose();
		blue.dispose();
		green.dispose();
		System.gc();
	}

	/**
	 * Creates the toolbar.
	 */
	private void createToolBar(Composite composite, Object layoutData) {
		toolBar = new ToolBar(composite, SWT.FLAT);
		toolBar.addListener(SWT.KeyUp, createToolbarCommandHandler());
		if (layoutData != null) toolBar.setLayoutData(layoutData);
		
		if (nativeFilter != null) {
			nativeFilterAdapter = (FilterAdapter) nativeFilterAdapterMap.get(nativeFilter.getClass());
			if (nativeFilterAdapter != null) {
				nativeToolItem = nativeFilterAdapter.create(toolBar);
				nativeToolItem.setSelection(true);
			} 
		}
		hexDumpToolItem = createHexDumpToolItem();
		if (nativeFilterAdapter == null) {
			// Default button changes for these instances.
			hexDumpToolItem.setSelection(true);
			// Prevent NullPointerExceptions if the nativeFilterAdapter does not apply.
			nativeFilterAdapter = hexFilterAdapter;
		}
		rawDumpToolItem = createRawDumpToolItem();
		new ToolItem(toolBar, SWT.SEPARATOR);
		copyToolItem = createCopyToolItem();
		new ToolItem(toolBar, SWT.SEPARATOR);
		printToolItem = createPrintToolItem();
		toolBar.pack();
	}
	
	/**
	 * Create the hex dump tool item (button).
	 */
	protected ToolItem createHexDumpToolItem() {
		hexFilterAdapter = new HexFilterAdapter(this, "Hex Dump", 
				"Displays file as a hex dump (F3)", 
				imageManager.get(ImageManager.ICON_VIEW_IN_HEX));
		hexFilterAdapter.setHexSelected();
		ToolItem toolItem = hexFilterAdapter.create(toolBar);
		return toolItem;
	}
	
	/**
	 * Create the raw dump tool item (button).
	 */
	protected ToolItem createRawDumpToolItem() {
		rawDumpFilterAdapter = new RawDumpFilterAdapter(this, "Raw Dump", 
				"Displays file as a raw hex dump (F4)", 
				imageManager.get(ImageManager.ICON_VIEW_IN_RAW_HEX));
		rawDumpFilterAdapter.setDumpSelected();
		ToolItem toolItem = rawDumpFilterAdapter.create(toolBar);
		return toolItem;
	}
	
	/**
	 * Create the copy tool item (button).
	 */
	protected ToolItem createCopyToolItem() {
		ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);
		toolItem.setImage(imageManager.get(ImageManager.ICON_COPY));
		toolItem.setText("Copy");
		toolItem.setToolTipText("Copies selection to the clipboard (CTRL+C)");
		toolItem.setEnabled(true);
		toolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				contentTypeAdapter.copy();
			}
		});
		return toolItem;
	}
	
	/**
	 * Create the print tool item (button).
	 */
	protected ToolItem createPrintToolItem() {
		ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);
		toolItem.setImage(imageManager.get(ImageManager.ICON_PRINT_FILE));
		toolItem.setText("Print");
		toolItem.setToolTipText("Print contents... (CTRL+P)");
		toolItem.setEnabled(true);
		toolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				contentTypeAdapter.print();
			}
		});
		return toolItem;
	}

	/**
	 * The toolbar command handler contains the global toolbar
	 * actions. The intent is that the listener is then added to 
	 * multiple visual components.
	 */
	public Listener createToolbarCommandHandler() {
		return new Listener() {
			public void handleEvent(Event event) {
				if (event.type == SWT.KeyUp) {
					if ((event.stateMask & SWT.CTRL) != 0) {	// CTRL+key
						switch (event.character) {
							case CTRL_C:
								contentTypeAdapter.copy();
								break;
							case CTRL_A:
								contentTypeAdapter.selectAll();
								break;
							case CTRL_P:
								contentTypeAdapter.print();
								break;
						}
					} else if ((event.stateMask & SWT.ALT) == 0) { // key alone
						switch (event.keyCode) {
							case SWT.F2:	// the "native" file format (image, text, etc)
								nativeFilterAdapter.display();
								setFilterToolItemSelection(true, false, false);
								break;
							case SWT.F3:	// Hex format
								hexFilterAdapter.display();
								setFilterToolItemSelection(false, true, false);
								break;
							case SWT.F4:	// "Raw" hex format
								rawDumpFilterAdapter.display();
								setFilterToolItemSelection(false, false, true);
								break;
						}
					}
				}
			}
		};
	}

	public FileFilter getFileFilter() {
		return nativeFilter;
	}
	public FileEntry getFileEntry() {
		return fileEntry;
	}
	public ToolItem getCopyToolItem() {
		return copyToolItem;
	}
	public ScrolledComposite getComposite() {
		return content;
	}
	public void setContentTypeAdapter(ContentTypeAdapter adapter) {
		this.contentTypeAdapter = adapter;
	}
	public Font getCourierFont() {
		return courier;
	}
	public Color getBlackColor() {
		return black;
	}
	public Color getGreenColor() {
		return green;
	}
	public Color getBlueColor() {
		return blue;
	}
	public void setFilterToolItemSelection(boolean nativeSelected, boolean hexSelected, boolean dumpSelected) {
		if (nativeToolItem != null) nativeToolItem.setSelection(nativeSelected);
		hexDumpToolItem.setSelection(hexSelected);
		rawDumpToolItem.setSelection(dumpSelected);
	}
}
