/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2003-2022 by Robert Greene
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
import java.util.Optional;

import com.webcodepro.applecommander.storage.filters.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.filteradapter.ApplesoftFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.BusinessBASICFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.DisassemblyFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.FilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.GraphicsFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.HexFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.RawDumpFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.ShapeTableFilterAdapter;
import com.webcodepro.applecommander.ui.swt.filteradapter.TextFilterAdapter;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.ContentTypeAdapter;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * View a particular files content.
 * <p>
 * Date created: Dec 7, 2003
 * @author Rob Greene
 */
public class FileViewerWindow {
	private static final char CTRL_A = 'A' - '@';
	private static final char CTRL_P = 'P' - '@';
	private static final char CTRL_C = 'C' - '@';
	
	private final TextBundle textBundle = UiBundle.getInstance();

	private final Shell parentShell;
	private final ImageManager imageManager;
	
	private Shell shell;
	private final FileEntry fileEntry;
	private final FileFilter nativeFilter;
	
	private ScrolledComposite content;
	private ToolBar toolBar;
	private ToolItem nativeToolItem;
	private ToolItem hexDumpToolItem;
	private ToolItem rawDumpToolItem;
	private ToolItem copyToolItem;
	// May or may not be setup
    private Optional<ToolItem> disassemblyToolItem = Optional.empty();
    private Optional<ToolItem> shapeTableToolItem = Optional.empty();
	
	private Font courier;

	private ContentTypeAdapter contentTypeAdapter;
	private Map<Class<?>,FilterAdapter> nativeFilterAdapterMap;
	private FilterAdapter nativeFilterAdapter;
	private FilterAdapter hexFilterAdapter;
	private FilterAdapter rawDumpFilterAdapter;
	private FilterAdapter disassemblyFilterAdapter;
	private FilterAdapter shapeTableFilterAdapter;
	
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
		shell.setText(textBundle.format("FileViewerWindow.Title", //$NON-NLS-1$ 
				fileEntry.getFilename()));
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

		courier = new Font(shell.getDisplay(), "Courier", 10, SWT.NORMAL); //$NON-NLS-1$

		nativeFilterAdapter.display();
		
		shell.open();
		SwtUtil.setupPagingInformation(content);
	}

	/**
	 * Setup all possible specialized FilterAdapters.
	 */	
	protected void createFilterAdapterMap() {
		nativeFilterAdapterMap = new HashMap<>();
		
		nativeFilterAdapterMap.put(ApplesoftFileFilter.class, 
			new ApplesoftFilterAdapter(this, textBundle.get("FileViewerWindow.ApplesoftButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.ApplesoftTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_BASIC_PROGRAM)
			));
		nativeFilterAdapterMap.put(BusinessBASICFileFilter.class, 
			new BusinessBASICFilterAdapter(this, textBundle.get("FileViewerWindow.BusinessBASICButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.BusinessBASICTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_BASIC_PROGRAM)
			));
		nativeFilterAdapterMap.put(AppleWorksDataBaseFileFilter.class, 
			new TextFilterAdapter(this, textBundle.get("FileViewerWindow.DatabaseButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.DatabaseTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_DATABASE)
			));
		nativeFilterAdapterMap.put(AppleWorksSpreadSheetFileFilter.class, 
			new TextFilterAdapter(this, textBundle.get("FileViewerWindow.SpreadsheetButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.SpreadsheetTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_SPREADSHEET)
			));
		nativeFilterAdapterMap.put(AppleWorksWordProcessorFileFilter.class, 
				new TextFilterAdapter(this, textBundle.get("FileViewerWindow.WordprocessorButton"),  //$NON-NLS-1$
					textBundle.get("FileViewerWindow.WordprocessorTooltip"),  //$NON-NLS-1$
					imageManager.get(ImageManager.ICON_VIEW_AS_WORDPROCESSOR)
				));
		nativeFilterAdapterMap.put(GutenbergFileFilter.class, 
				new TextFilterAdapter(this, textBundle.get("FileViewerWindow.WordprocessorButton"),  //$NON-NLS-1$
					textBundle.get("FileViewerWindow.WordprocessorTooltip"),  //$NON-NLS-1$
					imageManager.get(ImageManager.ICON_VIEW_AS_WORDPROCESSOR)
				));
		nativeFilterAdapterMap.put(AssemblySourceFileFilter.class, 
			new TextFilterAdapter(this, textBundle.get("FileViewerWindow.AssemblyButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.AssemblyTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE)
			));
		nativeFilterAdapterMap.put(GraphicsFileFilter.class, 
			new GraphicsFilterAdapter(this, textBundle.get("FileViewerWindow.ImageButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.ImageTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_IMAGE)
			));
		nativeFilterAdapterMap.put(IntegerBasicFileFilter.class, 
			new TextFilterAdapter(this, textBundle.get("FileViewerWindow.IntegerBasicButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.IntegerBasicTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_BASIC_PROGRAM)
			));
		nativeFilterAdapterMap.put(PascalTextFileFilter.class, 
			new TextFilterAdapter(this, textBundle.get("FileViewerWindow.PascalTextButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.PascalTextTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE)
			));
        nativeFilterAdapterMap.put(PascalCodeFileFilter.class,
                new TextFilterAdapter(this, textBundle.get("FileViewerWindow.PascalCodeButton"),
                        textBundle.get("FileViewerWindow.PascalCodeTooltip"),
                        imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE)
                ));
		nativeFilterAdapterMap.put(TextFileFilter.class,
			new TextFilterAdapter(this, textBundle.get("FileViewerWindow.TextButton"), //$NON-NLS-1$
				textBundle.get("FileViewerWindow.TextTooltip"), //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE)
			));
		nativeFilterAdapterMap.put(DisassemblyFileFilter.class, 
		    new DisassemblyFilterAdapter(this, textBundle.get("FileViewerWindow.DisassemblyButton"),
                textBundle.get("FileViewerWindow.DisassemblyTooltip"),
                imageManager.get(ImageManager.ICON_DISASSEMBLE_FILE)
            ));
        nativeFilterAdapterMap.put(ShapeTableFileFilter.class, 
            new ShapeTableFilterAdapter(this, textBundle.get("FileViewerWindow.ShapeTableButton"),
                textBundle.get("FileViewerWindow.ShapeTableTooltip"),
                imageManager.get(ImageManager.ICON_SHAPE_TABLE)
            ));
        nativeFilterAdapterMap.put(MBASICFileFilter.class,
            new TextFilterAdapter(this, textBundle.get("FileViewerWindow.MBASICButton"),
                    textBundle.get("FileViewerWindow.MBASICTooltip"),
                    imageManager.get(ImageManager.ICON_VIEW_AS_BASIC_PROGRAM)
            ));
	}
	
	/**
	 * Dispose of all shared resources.
	 */
	protected void dispose(DisposeEvent event) {
		courier.dispose();
		if (nativeFilterAdapter != null) nativeFilterAdapter.dispose();
		hexFilterAdapter.dispose();
		rawDumpFilterAdapter.dispose();
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
			nativeFilterAdapter = nativeFilterAdapterMap.get(nativeFilter.getClass());
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
		// Add the disassembly button only if it's not the default and if this filetype has a start address.
		if (fileEntry != null && fileEntry.needsAddress() && !(nativeFilter instanceof DisassemblyFileFilter)) {
		    disassemblyToolItem = Optional.of(createDisassemblyToolItem());
		    shapeTableToolItem = Optional.of(createShapeTableToolItem());
		}
		new ToolItem(toolBar, SWT.SEPARATOR);
		copyToolItem = createCopyToolItem();
		new ToolItem(toolBar, SWT.SEPARATOR);
		createPrintToolItem();
		toolBar.pack();
	}
	
	/**
	 * Create the hex dump tool item (button).
	 */
	protected ToolItem createHexDumpToolItem() {
		hexFilterAdapter = new HexFilterAdapter(this, textBundle.get("FileViewerWindow.HexDumpButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.HexDumpTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_IN_HEX));
		hexFilterAdapter.setHexSelected();
		ToolItem toolItem = hexFilterAdapter.create(toolBar);
		return toolItem;
	}
	
	/**
	 * Create the raw dump tool item (button).
	 */
	protected ToolItem createRawDumpToolItem() {
		rawDumpFilterAdapter = new RawDumpFilterAdapter(this, textBundle.get("FileViewerWindow.RawDumpButton"),  //$NON-NLS-1$
				textBundle.get("FileViewerWindow.RawDumpTooltip"),  //$NON-NLS-1$
				imageManager.get(ImageManager.ICON_VIEW_IN_RAW_HEX));
		rawDumpFilterAdapter.setDumpSelected();
		ToolItem toolItem = rawDumpFilterAdapter.create(toolBar);
		return toolItem;
	}

   /**
     * Create the disassembly tool item (button).
     */
    protected ToolItem createDisassemblyToolItem() {
        disassemblyFilterAdapter = new DisassemblyFilterAdapter(this, textBundle.get("FileViewerWindow.DisassemblyButton"),  //$NON-NLS-1$
                textBundle.get("FileViewerWindow.DisassemblyTooltip"),  //$NON-NLS-1$
                imageManager.get(ImageManager.ICON_DISASSEMBLE_FILE));
        disassemblyFilterAdapter.setDisassemblySelected();
        ToolItem toolItem = disassemblyFilterAdapter.create(toolBar);
        return toolItem;
    }

    /**
     * Create the shape table tool item (button).
     */
    protected ToolItem createShapeTableToolItem() {
        shapeTableFilterAdapter = new ShapeTableFilterAdapter(this, textBundle.get("FileViewerWindow.ShapeTableButton"),
                textBundle.get("FileViewerWindow.ShapeTableTooltip"),
                imageManager.get(ImageManager.ICON_SHAPE_TABLE));
        shapeTableFilterAdapter.setShapeTableSelected();
        ToolItem toolItem = shapeTableFilterAdapter.create(toolBar);
        return toolItem;
    }

	/**
	 * Create the copy tool item (button).
	 */
	protected ToolItem createCopyToolItem() {
		ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);
		toolItem.setImage(imageManager.get(ImageManager.ICON_COPY));
		toolItem.setText(textBundle.get("FileViewerWindow.CopyButton")); //$NON-NLS-1$
		toolItem.setToolTipText(textBundle.get("FileViewerWindow.CopyTooltip")); //$NON-NLS-1$
		toolItem.setEnabled(true);
		toolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getContentTypeAdapter().copy();
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
		toolItem.setText(textBundle.get("PrintButton")); //$NON-NLS-1$
		toolItem.setToolTipText(textBundle.get("FileViewerWindow.PrintTooltip")); //$NON-NLS-1$
		toolItem.setEnabled(true);
		toolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				getContentTypeAdapter().print();
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
								getContentTypeAdapter().copy();
								break;
							case CTRL_A:
								getContentTypeAdapter().selectAll();
								break;
							case CTRL_P:
								getContentTypeAdapter().print();
								break;
						}
					} else if ((event.stateMask & SWT.ALT) == 0) { // key alone
						switch (event.keyCode) {
							case SWT.F2:	// the "native" file format (image, text, etc)
								getNativeFilterAdapter().display();
								setFilterToolItemSelection(true, false, false, false, false);
								break;
							case SWT.F3:	// Hex format
								getHexFilterAdapter().display();
								setFilterToolItemSelection(false, true, false, false, false);
								break;
							case SWT.F4:	// "Raw" hex format
								getRawDumpFilterAdapter().display();
								setFilterToolItemSelection(false, false, true, false, false);
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
	public void setFilterToolItemSelection(boolean nativeSelected, boolean hexSelected, boolean dumpSelected,
	        boolean disassemblySelected, boolean shapeTableSelected) {
		if (nativeToolItem != null) nativeToolItem.setSelection(nativeSelected);
		hexDumpToolItem.setSelection(hexSelected);
		rawDumpToolItem.setSelection(dumpSelected);
		disassemblyToolItem.ifPresent(toolItem -> toolItem.setSelection(disassemblySelected));
		shapeTableToolItem.ifPresent(toolItem -> toolItem.setSelection(shapeTableSelected));
	}
	protected ContentTypeAdapter getContentTypeAdapter() {
		return contentTypeAdapter;
	}
	protected FilterAdapter getHexFilterAdapter() {
		return hexFilterAdapter;
	}
	protected FilterAdapter getNativeFilterAdapter() {
		return nativeFilterAdapter;
	}
	protected FilterAdapter getRawDumpFilterAdapter() {
		return rawDumpFilterAdapter;
	}
}
