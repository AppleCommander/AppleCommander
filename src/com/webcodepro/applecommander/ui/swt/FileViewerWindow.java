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

import java.io.ByteArrayInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import com.webcodepro.applecommander.storage.filters.HexDumpFileFilter;
import com.webcodepro.applecommander.storage.filters.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.ui.swt.util.ImageCanvas;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.ContentTypeAdapter;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.ImageCanvasAdapter;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.StyledTextAdapter;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.ApplesoftToken;
import com.webcodepro.applecommander.util.ApplesoftTokenizer;

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
	
	public ContentTypeAdapter contentTypeAdapter;
	/**
	 * Construct the file viewer window.
	 */
	public FileViewerWindow(Shell parentShell, FileEntry fileEntry, ImageManager imageManager) {
		this.parentShell = shell;
		this.fileEntry = fileEntry;
		this.imageManager = imageManager;
		this.nativeFilter = fileEntry.getSuggestedFilter();
	}

	/**
	 * Construct the file viewer window.
	 */
	public FileViewerWindow(Shell parentShell, FileEntry fileEntry, ImageManager imageManager, FileFilter nativeFilter) {
		this.parentShell = shell;
		this.fileEntry = fileEntry;
		this.imageManager = imageManager;
		this.nativeFilter = nativeFilter;
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
		content.addListener(SWT.KeyUp, createToolbarCommandHandler());
		
		courier = new Font(shell.getDisplay(), "Courier", 10, SWT.NORMAL);
		black = new Color(shell.getDisplay(), 0, 0, 0);
		blue = new Color(shell.getDisplay(), 0, 0, 192);
		green = new Color(shell.getDisplay(), 0, 192, 0);
		
		displayNativeFormat();
		
		shell.open();
		SwtUtil.setupPagingInformation(content);
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
	 * Creates the FILE tab toolbar.
	 */
	private void createFileToolBar(Composite composite, Object layoutData) {
		toolBar = new ToolBar(composite, SWT.FLAT);
		toolBar.addListener(SWT.KeyUp, createToolbarCommandHandler());
		if (layoutData != null) toolBar.setLayoutData(layoutData);
		
		if (nativeFilter instanceof ApplesoftFileFilter || nativeFilter instanceof IntegerBasicFileFilter) {
			nativeToolItem = new ToolItem(toolBar, SWT.RADIO);
			nativeToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_AS_BASIC_PROGRAM));
			nativeToolItem.setText("BASIC");
			nativeToolItem.setToolTipText("Displays file as BASIC program (F2)");
			nativeToolItem.setSelection(true);
		} else if (nativeFilter instanceof AppleWorksDataBaseFileFilter) {
			nativeToolItem = new ToolItem(toolBar, SWT.RADIO);
			nativeToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_AS_DATABASE));
			nativeToolItem.setText("Database");
			nativeToolItem.setToolTipText("Displays file as a database file (F2)");
			nativeToolItem.setSelection(true);
		} else if (nativeFilter instanceof AppleWorksSpreadSheetFileFilter) {
			nativeToolItem = new ToolItem(toolBar, SWT.RADIO);
			nativeToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_AS_SPREADSHEET));
			nativeToolItem.setText("Spreadsheet");
			nativeToolItem.setToolTipText("Displays file as a spreadsheet file (F2)");
			nativeToolItem.setSelection(true);
		} else if (nativeFilter instanceof AppleWorksWordProcessorFileFilter) {
			nativeToolItem = new ToolItem(toolBar, SWT.RADIO);
			nativeToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_AS_WORDPROCESSOR));
			nativeToolItem.setText("Wordprocessor");
			nativeToolItem.setToolTipText("Displays file as a wordprocessor file (F2)");
			nativeToolItem.setSelection(true);
		} else if (nativeFilter instanceof GraphicsFileFilter) {
			nativeToolItem = new ToolItem(toolBar, SWT.RADIO);
			nativeToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_AS_IMAGE));
			nativeToolItem.setText("Image");
			nativeToolItem.setToolTipText("Displays file as an image (F2)");
			nativeToolItem.setSelection(true);
		} else if (nativeFilter instanceof TextFileFilter) {
			nativeToolItem = new ToolItem(toolBar, SWT.RADIO);
			nativeToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE));
			nativeToolItem.setText("Text");
			nativeToolItem.setToolTipText("Displays file as a text file (F2)");
			nativeToolItem.setSelection(true);
		} else if (nativeFilter instanceof AssemblySourceFileFilter) {
			nativeToolItem = new ToolItem(toolBar, SWT.RADIO);
			nativeToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_AS_TEXTFILE));
			nativeToolItem.setText("Assembly");
			nativeToolItem.setToolTipText("Displays file as assembly source file (F2)");
			nativeToolItem.setSelection(true);
		}
		if (nativeToolItem != null) {
			nativeToolItem.addSelectionListener(new SelectionAdapter () {
				public void widgetSelected(SelectionEvent e) {
					displayNativeFormat();
				}
			});
		}
		
		// FIXME Refactoring ideas...
//		thingMap.add(AssemblySourceFileFilter.class, new Thing("Assembly", 
//			"Displays file as assembly source file (F2)"),
//			ImageManager.ICON_VIEW_AS_TEXTFILE), true));
//		// etc		
//		if (nativeFilter != null) {
//			Thing thing = (Thing) thingMap.get(nativeFilter.getClass());
//			thing.create(toolBar);
//		}

		hexDumpToolItem = new ToolItem(toolBar, SWT.RADIO);
		hexDumpToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_IN_HEX));
		hexDumpToolItem.setText("Hex Dump");
		hexDumpToolItem.setToolTipText("Displays file as a hex dump (F3)");
		hexDumpToolItem.setSelection(false);
		hexDumpToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				displayHexFormat();
			}
		});

		rawDumpToolItem = new ToolItem(toolBar, SWT.RADIO);
		rawDumpToolItem.setImage(imageManager.get(ImageManager.ICON_VIEW_IN_RAW_HEX));
		rawDumpToolItem.setText("Raw Dump");
		rawDumpToolItem.setToolTipText("Displays file as a raw hex dump (F4)");
		rawDumpToolItem.setSelection(false);
		rawDumpToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				displayRawFormat();
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		copyToolItem = new ToolItem(toolBar, SWT.PUSH);
		copyToolItem.setImage(imageManager.get(ImageManager.ICON_COPY));
		copyToolItem.setText("Copy");
		copyToolItem.setToolTipText("Copies selection to the clipboard (CTRL+C)");
		copyToolItem.setEnabled(true);
		copyToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				copy();
			}
		});

		new ToolItem(toolBar, SWT.SEPARATOR);
		
		printToolItem = new ToolItem(toolBar, SWT.PUSH);
		printToolItem.setImage(imageManager.get(ImageManager.ICON_PRINT_FILE));
		printToolItem.setText("Print");
		printToolItem.setToolTipText("Print contents... (CTRL+P)");
		printToolItem.setEnabled(true);
		printToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				print();
			}
		});

		toolBar.pack();
	}
	
	/**
	 * Print current file.
	 */
	protected void print() {
		contentTypeAdapter.print();
	}
	
	/**
	 * Select all text within the widget.
	 */
	protected void selectAll() {
		contentTypeAdapter.selectAll();
	}

	/**
	 * Perform copy operation.
	 */
	protected void copy() {
		contentTypeAdapter.copy();
	}
	
	/**
	 * Display the file in its native format.
	 */
	protected void displayNativeFormat() {
		Control oldContent = content.getContent();
		if (oldContent != null) {
			oldContent.dispose();
			content.setContent(null);
		}
		
		if (nativeToolItem != null) nativeToolItem.setSelection(true);
		hexDumpToolItem.setSelection(false);
		rawDumpToolItem.setSelection(false);
		
		copyToolItem.setEnabled(true);
		
		if (nativeFilter instanceof ApplesoftFileFilter) {
			StyledText styledText = new StyledText(content, SWT.NONE);
			styledText.setForeground(black);
			styledText.setFont(courier);
			styledText.setEditable(false);

			ApplesoftTokenizer tokenizer = new ApplesoftTokenizer(fileEntry);
			boolean firstLine = true;
			while (tokenizer.hasMoreTokens()) {
				ApplesoftToken token = tokenizer.getNextToken();
				if (token == null) {
					continue;	// should be end of program...
				} else if (token.isLineNumber()) {
					if (firstLine) {
						firstLine = false;
					} else {
						styledText.append("\n");
					}
					styledText.append(Integer.toString(token.getLineNumber()));
					styledText.append(" ");
				} else if (token.isCommandSeparator() || token.isExpressionSeparator()) {
					styledText.append(token.getStringValue());
				} else if (token.isEndOfCommand()) {
					styledText.append("\n");
				} else if (token.isString()) {
					int caretOffset = styledText.getCharCount();
					styledText.append(token.getStringValue());
					StyleRange styleRange = new StyleRange();
					styleRange.start = caretOffset;
					styleRange.length = token.getStringValue().length();
					styleRange.foreground = green;
					styledText.setStyleRange(styleRange);
				} else if (token.isToken()) {
					int caretOffset = styledText.getCharCount();
					styledText.append(token.getTokenString());
					StyleRange styleRange = new StyleRange();
					styleRange.start = caretOffset;
					styleRange.length = token.getTokenString().length();
					//styleRange.fontStyle = SWT.BOLD;
					styleRange.foreground = blue;
					styledText.setStyleRange(styleRange);
				}
			}
			Point size = styledText.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			content.setContent(styledText);
			content.setExpandHorizontal(true);
			content.setExpandVertical(true);
			content.setMinWidth(size.x);
			content.setMinHeight(size.y);
			content.getContent().addListener(SWT.KeyUp, createToolbarCommandHandler());
			
			contentTypeAdapter = new StyledTextAdapter(styledText, fileEntry.getFilename());
		} else if (nativeFilter instanceof IntegerBasicFileFilter) {
			String basicDump = new String(nativeFilter.filter(fileEntry));
			createTextWidget(content, basicDump);
		} else if (nativeFilter instanceof AppleWorksDataBaseFileFilter) {
			String basicDump = new String(nativeFilter.filter(fileEntry));
			createTextWidget(content, basicDump);
		} else if (nativeFilter instanceof AppleWorksSpreadSheetFileFilter) {
			String basicDump = new String(nativeFilter.filter(fileEntry));
			createTextWidget(content, basicDump);
		} else if (nativeFilter instanceof AppleWorksWordProcessorFileFilter) {
			String basicDump = new String(nativeFilter.filter(fileEntry));
			createTextWidget(content, basicDump);
		} else if (nativeFilter instanceof GraphicsFileFilter) {
			copyToolItem.setEnabled(false);

			byte[] imageBytes = nativeFilter.filter(fileEntry);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
			ImageLoader imageLoader = new ImageLoader();
			ImageData[] imageData = imageLoader.load(inputStream);
			final Image image = new Image(shell.getDisplay(), imageData[0]);

			GridLayout layout = new GridLayout();
			content.setLayout(layout);
			GridData gridData = new GridData();
			gridData.widthHint = imageData[0].width;
			gridData.heightHint = imageData[0].height;
			ImageCanvas imageCanvas = new ImageCanvas(content, SWT.NONE, image, gridData);
			content.setContent(imageCanvas);
			content.setExpandHorizontal(true);
			content.setExpandVertical(true);
			content.setMinWidth(imageData[0].width);
			content.setMinHeight(imageData[0].height);
			
			contentTypeAdapter = new ImageCanvasAdapter(imageCanvas, fileEntry.getFilename());
		} else if (nativeFilter instanceof TextFileFilter) {
			String textDump = new String(nativeFilter.filter(fileEntry));
			createTextWidget(content, textDump);
		} else if (nativeFilter instanceof AssemblySourceFileFilter) {
			String textDump = new String(nativeFilter.filter(fileEntry));
			createTextWidget(content, textDump);
		} else {
			displayHexFormat();
		}
	}
	
	/**
	 * Create a StyledText widget for displaying plain text.
	 */
	protected void createTextWidget(Composite composite, String text) {
		copyToolItem.setEnabled(true);

		StyledText styledText = new StyledText(composite, SWT.NONE);
		styledText.setText(text);
		styledText.setFont(courier);
		styledText.setEditable(false);
		//styledText.setWordWrap(true);		// seems to throw size out-of-whack
		Point size = styledText.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		content.setContent(styledText);
		content.setExpandHorizontal(true);
		content.setExpandVertical(true);
		content.setMinWidth(size.x);
		content.setMinHeight(size.y);
		content.getContent().addListener(SWT.KeyUp, createToolbarCommandHandler());

		contentTypeAdapter = new StyledTextAdapter(styledText, fileEntry.getFilename());
	}
	
	/**
	 * Display file content as a hex dump.
	 */
	protected void displayHexFormat() {
		copyToolItem.setEnabled(true);

		Control oldContent = content.getContent();
		if (oldContent != null) {
			oldContent.dispose();
			content.setContent(null);
		}

		if (nativeToolItem != null) nativeToolItem.setSelection(false);
		hexDumpToolItem.setSelection(true);
		rawDumpToolItem.setSelection(false);

		FileFilter filter = new HexDumpFileFilter();
		String hexDump = new String(filter.filter(fileEntry));
		createTextWidget(content, hexDump);
	}
	
	/**
	 * Display file content as hex dump of raw disk bytes.
	 */
	protected void displayRawFormat() {
		Control oldContent = content.getContent();
		if (oldContent != null) {
			oldContent.dispose();
			content.setContent(null);
		}

		if (nativeToolItem != null) nativeToolItem.setSelection(false);
		hexDumpToolItem.setSelection(false);
		rawDumpToolItem.setSelection(true);

		String rawDump = AppleUtil.getHexDump(
			fileEntry.getFormattedDisk().getFileData(fileEntry));
		createTextWidget(content, rawDump);
	}

	/**
	 * The toolbar command handler contains the global toolbar
	 * actions. The intent is that the listener is then added to 
	 * multiple visual components.
	 */
	private Listener createToolbarCommandHandler() {
		return new Listener() {
			public void handleEvent(Event event) {
				if (event.type == SWT.KeyUp) {
					if ((event.stateMask & SWT.CTRL) != 0) {	// CTRL+key
						switch (event.character) {
							case CTRL_C:
								copy();
								break;
							case CTRL_A:
								selectAll();
								break;
							case CTRL_P:
								print();
								break;
						}
					} else if ((event.stateMask & SWT.ALT) == 0) { // key alone
						switch (event.keyCode) {
							case SWT.F2:	// the "native" file format (image, text, etc)
								displayNativeFormat();
								break;
							case SWT.F3:	// Hex format
								displayHexFormat();
								break;
							case SWT.F4:	// "Raw" hex format
								displayRawFormat();
								break;
						}
					}
				}
			}
		};
	}
}
