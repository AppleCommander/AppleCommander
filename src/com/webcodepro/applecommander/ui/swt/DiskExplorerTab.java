/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-3 by Robert Greene
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.webcodepro.applecommander.compiler.ApplesoftCompiler;
import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileEntryComparator;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;
import com.webcodepro.applecommander.storage.filters.AppleWorksDataBaseFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksSpreadSheetFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.storage.filters.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.filters.PascalTextFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.storage.os.prodos.ProdosDiskSizeDoesNotMatchException;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.NibbleOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import com.webcodepro.applecommander.ui.ImportSpecification;
import com.webcodepro.applecommander.ui.UserPreferences;
import com.webcodepro.applecommander.ui.swt.util.DropDownSelectionListener;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.ui.swt.wizard.compilefile.CompileWizard;
import com.webcodepro.applecommander.ui.swt.wizard.exportfile.ExportWizard;
import com.webcodepro.applecommander.ui.swt.wizard.importfile.ImportWizard;
import com.webcodepro.applecommander.util.AppleUtil;

/**
 * Build the Disk File tab for the Disk Window.
 * <p>
 * Date created: Nov 17, 2002 9:46:53 PM
 * @author Rob Greene
 */
public class DiskExplorerTab {
	private static final char CTRL_C = 'C' - '@';
	private static final char CTRL_D = 'D' - '@';
	private static final char CTRL_E = 'E' - '@';
	private static final char CTRL_I = 'I' - '@';
	private static final char CTRL_P = 'P' - '@';
	private static final char CTRL_S = 'S' - '@';
	private static final char CTRL_V = 'V' - '@';

	// These are given to us from DiskWindow	
	private Shell shell;
	private ImageManager imageManager;
	private DiskWindow diskWindow;
	private FormattedDisk[] disks;
	
	private SashForm sashForm;
	private Tree directoryTree;
	private Table fileTable;
	private ToolBar toolBar;
	private ToolItem standardFormatToolItem;
	private ToolItem nativeFormatToolItem;
	private ToolItem detailFormatToolItem;
	private ToolItem showDeletedFilesToolItem;
	private ToolItem exportToolItem;
	private ToolItem importToolItem;
	private ToolItem compileToolItem;
	private ToolItem viewFileItem;
	private ToolItem printToolItem;
	private ToolItem deleteToolItem;
	private ToolItem saveToolItem;
	private ToolItem saveAsToolItem;
	private ToolItem changeOrderToolItem;
	private Menu changeOrderMenu;

	private UserPreferences userPreferences = UserPreferences.getInstance();
	private FileFilter fileFilter;
	private GraphicsFileFilter graphicsFilter = new GraphicsFileFilter();
	private AppleWorksWordProcessorFileFilter awpFilter = new AppleWorksWordProcessorFileFilter();

	private int currentFormat = FormattedDisk.FILE_DISPLAY_STANDARD;
	private boolean formatChanged;
	private List currentFileList;
	private Map columnWidths = new HashMap();
	private boolean showDeletedFiles;

	/**
	 * Create the DISK INFO tab.
	 */
	public DiskExplorerTab(CTabFolder tabFolder, FormattedDisk[] disks, ImageManager imageManager, DiskWindow diskWindow) {
		this.disks = disks;
		this.shell = tabFolder.getShell();
		this.imageManager = imageManager;
		this.diskWindow = diskWindow;
		
		createFilesTab(tabFolder);
	}
	/**
	 * Dispose of resources.
	 */
	public void dispose() {
		sashForm.dispose();
		directoryTree.dispose();
		fileTable.dispose();
		standardFormatToolItem.dispose();
		nativeFormatToolItem.dispose();
		detailFormatToolItem.dispose();
		showDeletedFilesToolItem.dispose();
		exportToolItem.dispose();
		importToolItem.dispose();
		deleteToolItem.dispose();
		compileToolItem.dispose();
		viewFileItem.dispose();
		toolBar.dispose();
		changeOrderToolItem.dispose();

		directoryTree = null;
		fileTable = null;
		currentFileList = null;
	}
	/**
	 * Create the FILES tab.
	 */
	protected void createFilesTab(CTabFolder tabFolder) {
		CTabItem ctabitem = new CTabItem(tabFolder, SWT.NULL);
		ctabitem.setText("Files");

		Composite composite = new Composite(tabFolder, SWT.NULL);
		ctabitem.setControl(composite);
		GridLayout gridLayout = new GridLayout(1, false);
		composite.setLayout(gridLayout);
		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		createFileToolBar(composite, gridData);
		
		sashForm = new SashForm(composite, SWT.NONE);
		sashForm.setOrientation(SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		sashForm.setLayoutData(gridData);

		directoryTree = new Tree(sashForm, SWT.SINGLE | SWT.BORDER);
		directoryTree.setMenu(createDirectoryPopupMenu());
		directoryTree.addSelectionListener(new SelectionListener() {
			/**
			 * Single-click handler.
			 */
			public void widgetSelected(SelectionEvent event) {
				changeCurrentFormat(currentFormat);		// minor hack
			}
			/**
			 * Double-click handler.
			 */
			public void widgetDefaultSelected(SelectionEvent event) {
				Tree item = (Tree) event.getSource();
				TreeItem[] treeItem = item.getSelection();
				treeItem[0].setExpanded(!treeItem[0].getExpanded());
			}
		});
		directoryTree.addListener(SWT.KeyUp, createDirectoryKeyboardHandler());
		directoryTree.addListener(SWT.KeyUp, createToolbarCommandHandler());

		fileTable = new Table(sashForm, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		fileTable.setHeaderVisible(true);

		sashForm.setWeights(new int[] {1,2});

		for (int i=0; i<disks.length; i++) {
			TreeItem diskItem = new TreeItem(directoryTree, SWT.BORDER);
			diskItem.setText(disks[i].getDiskName());
			diskItem.setData(disks[i]);
			directoryTree.setSelection(new TreeItem[] { diskItem });
			
			if (disks[i].canHaveDirectories()) {
				Iterator files = disks[i].getFiles().iterator();
				while (files.hasNext()) {
					FileEntry entry = (FileEntry) files.next();
					if (entry.isDirectory()) {
						TreeItem item = new TreeItem(diskItem, SWT.BORDER);
						item.setText(entry.getFilename());
						item.setData(entry);
						addDirectoriesToTree(item, (DirectoryEntry)entry);
					}
				}
			}
		}
			
		computeColumnWidths(FormattedDisk.FILE_DISPLAY_STANDARD);
		computeColumnWidths(FormattedDisk.FILE_DISPLAY_NATIVE);
		computeColumnWidths(FormattedDisk.FILE_DISPLAY_DETAIL);

		formatChanged = true;
		fillFileTable(disks[0].getFiles());
		directoryTree.setSelection(new TreeItem[] { directoryTree.getItems()[0] });
	}
	/**
	 * Construct the popup menu for the directory table on the File tab.
	 * Using the first logical disk as the indicator for all logical disks.
	 */
	protected Menu createDirectoryPopupMenu() {
		Menu menu = new Menu(shell, SWT.POP_UP);
		
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Expand\t+");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = directoryTree.getSelection();
				treeItem[0].setExpanded(true);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Collapse\t-");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = directoryTree.getSelection();
				treeItem[0].setExpanded(false);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Expand All\tCtrl +");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = directoryTree.getSelection();
				setDirectoryExpandedStates(treeItem[0], true);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Collapse All\tCtrl -");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = directoryTree.getSelection();
				setDirectoryExpandedStates(treeItem[0], false);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Import...\tCTRL+I");
		item.setImage(imageManager.get(ImageManager.ICON_IMPORT_FILE));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				importFiles();
			}
		});
		item.setEnabled(disks[0].canCreateFile() && disks[0].canWriteFileData());
		
		return menu;
	}
	/**
	 * Construct the popup menu for the file table on the File tab.
	 */
	protected Menu createFilePopupMenu() {
		Menu menu = new Menu(shell, SWT.POP_UP);
		menu.addMenuListener(new MenuAdapter() {
			/**
			 * Toggle all sub-menu MenuItems to the proper state to reflect
			 * the current file extension chosen.
			 */
			public void menuShown(MenuEvent event) {
				Menu theMenu = (Menu) event.getSource();
				MenuItem[] subItems = theMenu.getItems();
				FileEntry fileEntry = getSelectedFileEntry();
				// View File
				subItems[0].setEnabled(disks[0].canReadFileData() 
					&& fileEntry != null && !fileEntry.isDeleted() 
					&& !fileEntry.isDirectory());
				subItems[1].setEnabled(disks[0].canReadFileData() 
					&& fileEntry != null && !fileEntry.isDeleted() 
					&& !fileEntry.isDirectory());
				// Compile File
				subItems[3].setEnabled(disks[0].canReadFileData()
					&& fileEntry != null && fileEntry.canCompile()
					&& !fileEntry.isDeleted());
				// Export File
				subItems[5].setEnabled(disks[0].canReadFileData()
					&& fileEntry != null && !fileEntry.isDeleted()
					&& !fileEntry.isDirectory());
				subItems[6].setEnabled(disks[0].canReadFileData()
					&& fileEntry != null && !fileEntry.isDeleted()
					&& !fileEntry.isDirectory());
				// Delete File
				subItems[8].setEnabled(disks[0].canDeleteFile()
					&& fileEntry != null && !fileEntry.isDeleted());
			}
		});
		
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&View Wizard\tCtrl+V");
		item.setAccelerator(SWT.CTRL+'V');
		item.setImage(imageManager.get(ImageManager.ICON_VIEW_FILE));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				viewFile(null);
			}
		});

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("View As");
		item.setMenu(createFileViewMenu(SWT.DROP_DOWN));

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&Compile...\tCtrl+C");
		item.setAccelerator(SWT.CTRL+'C');
		item.setImage(imageManager.get(ImageManager.ICON_COMPILE_FILE));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				compileFileWizard();
			}
		});
		
		item = new MenuItem(menu, SWT.SEPARATOR);
		
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&Export Wizard...\tCtrl+E");
		item.setAccelerator(SWT.CTRL+'E');
		item.setImage(imageManager.get(ImageManager.ICON_EXPORT_FILE));

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Export As...");
		item.setMenu(createFileExportMenu(SWT.DROP_DOWN));

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&Delete...\tCtrl+D");
		item.setAccelerator(SWT.CTRL+'D');
		item.setImage(imageManager.get(ImageManager.ICON_DELETE_FILE));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				deleteFile();
			}
		});
		
		return menu;
	}
	/**
	 * Construct the popup menu for the view as right-click option.
	 */
	protected Menu createFileViewMenu(int style) {
		Menu menu = new Menu(shell, style);
		
		MenuItem item = new MenuItem(menu, SWT.NONE);
		item.setText("Text");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				viewFile(TextFileFilter.class);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText("Graphics");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				viewFile(GraphicsFileFilter.class);
			}
		});
		
		return menu;
	}
	/**
	 * Construct the popup menu for the export button on the toolbar.
	 */
	protected Menu createFileExportMenu(int style) {
		Menu menu = new Menu(shell, style);
		
		MenuItem item = new MenuItem(menu, SWT.NONE);
		item.setText("Raw disk data...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = null;
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText("Binary...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = new BinaryFileFilter();
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText("Applesoft Basic...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = new ApplesoftFileFilter();
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText("Integer Basic...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = new IntegerBasicFileFilter();
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText("ASCII Text...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = new TextFileFilter();
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText("Pascal Text...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = new PascalTextFileFilter();
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText("AppleWorks Spreadsheet File...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = new AppleWorksSpreadSheetFileFilter();
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText("AppleWorks Database File...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = new AppleWorksDataBaseFileFilter();
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.NONE);
		item.setText("AppleWorks WordProcessor File...");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = awpFilter;
				exportFile(null);
			}
		});
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Rendering");
		Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
		item.setMenu(subMenu);
		subMenu.addMenuListener(new MenuAdapter() {
			/**
			 * Toggle all sub-menu MenuItems to the proper state to reflect
			 * the current file extension chosen.
			 */
			public void menuShown(MenuEvent event) {
				Menu theMenu = (Menu) event.getSource();
				MenuItem[] subItems = theMenu.getItems();
				subItems[0].setSelection(awpFilter.isTextRendering());
				subItems[1].setSelection(awpFilter.isHtmlRendering());
				subItems[2].setSelection(awpFilter.isRtfRendering());
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("Text");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				awpFilter.selectTextRendering();
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("HTML");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				awpFilter.selectHtmlRendering();
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("RTF");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				awpFilter.selectRtfRendering();
			}
		});
		
		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.NONE);
		item.setText("Graphics...");
		item.setEnabled(graphicsFilter.isCodecAvailable());
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fileFilter = graphicsFilter;
				exportFile(null);
			}
		});
		
		// Add graphics mode
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Mode");
		item.setEnabled(graphicsFilter.isCodecAvailable());
		subMenu = new Menu(shell, SWT.DROP_DOWN);
		item.setMenu(subMenu);
		subMenu.addMenuListener(new MenuAdapter() {
			/**
			 * Toggle all sub-menu MenuItems to the proper state to reflect
			 * the current file extension chosen.
			 */
			public void menuShown(MenuEvent event) {
				Menu theMenu = (Menu) event.getSource();
				MenuItem[] subItems = theMenu.getItems();
				subItems[0].setSelection(graphicsFilter.isHiresBlackAndWhiteMode());
				subItems[1].setSelection(graphicsFilter.isHiresColorMode());
				subItems[2].setSelection(graphicsFilter.isDoubleHiresBlackAndWhiteMode());
				subItems[3].setSelection(graphicsFilter.isDoubleHiresColorMode());
				subItems[4].setSelection(graphicsFilter.isSuperHires16Mode());
				subItems[5].setSelection(graphicsFilter.isSuperHires3200Mode());
				subItems[6].setSelection(graphicsFilter.isQuickDraw2Icon());
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("Hi-Res B&W");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				graphicsFilter.setMode(GraphicsFileFilter.MODE_HGR_BLACK_AND_WHITE);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("Hi-Res Color");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				graphicsFilter.setMode(GraphicsFileFilter.MODE_HGR_COLOR);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("Double Hi-Res B&W");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				graphicsFilter.setMode(GraphicsFileFilter.MODE_DHR_BLACK_AND_WHITE);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("Double Hi-Res COLOR");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				graphicsFilter.setMode(GraphicsFileFilter.MODE_DHR_COLOR);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("Super Hires");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				graphicsFilter.setMode(GraphicsFileFilter.MODE_SHR_16);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("Super Hires 3200 color");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				graphicsFilter.setMode(GraphicsFileFilter.MODE_SHR_3200);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("QuickDraw II Icon file (ICN)");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				graphicsFilter.setMode(GraphicsFileFilter.MODE_QUICKDRAW2_ICON);
			}
		});
		
		// Add graphics formats, if any are defined.
		String[] formats = graphicsFilter.getFileExtensions();
		if (formats != null && formats.length > 0) {
			item = new MenuItem(menu, SWT.CASCADE);
			item.setText("Format");
			item.setEnabled(graphicsFilter.isCodecAvailable());
			subMenu = new Menu(shell, SWT.DROP_DOWN);
			item.setMenu(subMenu);
			subMenu.addMenuListener(new MenuAdapter() {
				/**
				 * Toggle all sub-menu MenuItems to the proper state to reflect
				 * the current file extension chosen.
				 */
				public void menuShown(MenuEvent event) {
					Menu theMenu = (Menu) event.getSource();
					MenuItem[] subItems = theMenu.getItems();
					for (int i=0; i<subItems.length; i++) {
						subItems[i].setSelection(subItems[i].getText().
							equals(graphicsFilter.getExtension()));
					}
				}
			});
			// Add all graphics formats...
			for (int i=0; i<formats.length; i++) {
				item = new MenuItem(subMenu, SWT.RADIO);
				item.setText(formats[i]);
				item.addSelectionListener(new SelectionAdapter() {
					/**
					 * Set the file extension to use.
					 */
					public void widgetSelected(SelectionEvent event) {
						MenuItem menuItem = (MenuItem) event.getSource();
						graphicsFilter.setExtension(menuItem.getText());
					}
				});
			}
		}
		
		return menu;
	}	
	/**
	 * Change the "expanded" state of the node.
	 */
	protected void setDirectoryExpandedStates(TreeItem treeItem, boolean expand) {
		treeItem.setExpanded(expand);
		TreeItem[] treeItems = treeItem.getItems();
		for (int i=0; i<treeItems.length; i++) {
			setDirectoryExpandedStates(treeItems[i], expand);
		}
	}
	/**
	 * Pre-compute column widths for the file tab.
	 * These can and are over-ridden by user sizing.
	 */
	protected void computeColumnWidths(int format) {
		List headers = disks[0].getFileColumnHeaders(format);
		int[] headerWidths = new int[headers.size()];
		GC gc = new GC(shell);
		for (int i=0; i<headers.size(); i++) {
			FileColumnHeader header = (FileColumnHeader) headers.get(i);
			if (header.getTitle().length() >= header.getMaximumWidth()) {
				headerWidths[i] = gc.stringExtent(header.getTitle()).x + gc.stringExtent("WW").x;
			} else {
				headerWidths[i] = gc.stringExtent("W").x * header.getMaximumWidth();
			}
		}
		gc.dispose();
		gc = null;
		columnWidths.put(new Integer(format), headerWidths);
	}
	/**
	 * Preserve the column widths.
	 */
	protected void preserveColumnWidths() {
		TableColumn[] columns = fileTable.getColumns();
		int[] widths = new int[columns.length];
		for (int i=0; i<columns.length; i++) {
			widths[i] = columns[i].getWidth();
		}
		columnWidths.put(new Integer(currentFormat), widths);
	}
	/**
	 * Display files in the fileTable.
	 */
	protected void fillFileTable(List fileList) {
		int[] weights = sashForm.getWeights();

		if (formatChanged) {
			fileTable.dispose();
			fileTable = new Table(sashForm, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
			fileTable.addListener(SWT.KeyUp, createFileKeyboardHandler());
			fileTable.addListener(SWT.KeyUp, createToolbarCommandHandler());
			fileTable.setHeaderVisible(true);
			fileTable.setMenu(createFilePopupMenu());
			fileTable.addSelectionListener(new SelectionListener() {
				/**
				 * Single-click handler.
				 */
				public void widgetSelected(SelectionEvent event) {
					importToolItem.setEnabled(disks[0].canCreateFile() && disks[0].canWriteFileData());
					if (fileTable.getSelectionCount() > 0) {
						FileEntry fileEntry = getSelectedFileEntry();
						exportToolItem.setEnabled(disks[0].canReadFileData());
						deleteToolItem.setEnabled(disks[0].canDeleteFile());
						compileToolItem.setEnabled(fileEntry != null && fileEntry.canCompile());
						viewFileItem.setEnabled(true);
					} else {
						exportToolItem.setEnabled(false);
						deleteToolItem.setEnabled(false);
						compileToolItem.setEnabled(false);
						viewFileItem.setEnabled(false);
					}
				}
				/**
				 * Double-click handler.
				 */
				public void widgetDefaultSelected(SelectionEvent event) {
					viewFile(null);
				}
			});
			TableColumn column = null;
			List headers = disks[0].getFileColumnHeaders(currentFormat);
			int[] widths = (int[])columnWidths.get(new Integer(currentFormat));
			for (int i=0; i<headers.size(); i++) {
				FileColumnHeader header = (FileColumnHeader) headers.get(i);
				int align = header.isCenterAlign() ? SWT.CENTER :
					header.isLeftAlign() ? SWT.LEFT : SWT.RIGHT;
				column = new TableColumn(fileTable, align);
				column.setText(header.getTitle());
				column.setWidth(widths[i]);
				final int columnIndex = i;
				column.addSelectionListener(new SelectionAdapter() {		
					public void widgetSelected(SelectionEvent e) {
						sortFileTable(columnIndex);
					}
				});
			}
		} else {
			fileTable.removeAll();
		}

		Iterator files = fileList.iterator();
		while (files.hasNext()) {
			FileEntry entry = (FileEntry) files.next();
			if (showDeletedFiles || !entry.isDeleted()) {
				TableItem item = new TableItem(fileTable, 0);
				List data = entry.getFileColumnData(currentFormat);
				for (int i=0; i<data.size(); i++) {
					item.setText(i, (String)data.get(i));
				}
				item.setData(entry);
			}
		}
		
		sashForm.setWeights(weights);
		formatChanged = false;
		currentFileList = fileList;

		// disable all file-level operations:
		exportToolItem.setEnabled(false);
		deleteToolItem.setEnabled(false);
		compileToolItem.setEnabled(false);
		viewFileItem.setEnabled(false);
	}
	/**
	 * Open up the Export Wizard dialog box.
	 */
	protected void exportFileWizard() {
		// Get a sugeseted filter, if possible:
		FileEntry fileEntry = getSelectedFileEntry();
		if (fileEntry != null) {
			fileFilter = fileEntry.getSuggestedFilter();
		}
		// Start wizard:
		ExportWizard wizard = new ExportWizard(shell, 
			imageManager, fileEntry.getFormattedDisk());
		wizard.setFileFilter(fileFilter);
		wizard.setDirectory(userPreferences.getExportDirectory());
		wizard.open();
		if (wizard.isWizardCompleted()) {
			fileFilter = wizard.getFileFilter();
			String exportDirectory = wizard.getDirectory();
			exportFile(exportDirectory);
		}
	}
	/**
	 * Export all selected files.
	 */
	private void exportFile(String directory) {
		boolean promptForIndividualFiles = (directory == null);
		TableItem[] selection = fileTable.getSelection();
		for (int i=0; i<selection.length; i++) {
			TableItem tableItem = selection[i];
			FileEntry fileEntry = (FileEntry) tableItem.getData();
			String filename = null;
			if (promptForIndividualFiles) {
				FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
				fileDialog.setFilterPath(userPreferences.getExportDirectory());
				if (fileFilter != null) {
					fileDialog.setFileName(fileFilter.getSuggestedFileName(fileEntry));
				} else {
					fileDialog.setFileName(fileEntry.getFilename());
				}
				filename = fileDialog.open();
				directory = fileDialog.getFilterPath();
			} else {
				filename = directory + File.separator + AppleUtil.
					getNiceFilename(fileFilter.getSuggestedFileName(fileEntry));
			}
			if (filename != null) {
				userPreferences.setExportDirectory(directory);
				try {
					File file = new File(filename);
					if (file.exists()) {
						Shell finalShell = shell;
						MessageBox box = new MessageBox(finalShell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
						box.setText("File already exists!");
						box.setMessage(
							"The file '" + filename + "' already exists. "
							+ "Do you want to over-write it?");
						if (box.open() == SWT.NO) {
							return;	// do not overwrite file
						}
					}
					byte[] data = null;
					if (fileFilter != null) {
						data = fileFilter.filter(fileEntry);
					} else {
						data = fileEntry.getFormattedDisk().getFileData(fileEntry);
					}
					OutputStream outputStream = new FileOutputStream(file);
					outputStream.write(data);
					outputStream.close();
				} catch (Exception ex) {
					Shell finalShell = shell;
					String errorMessage = ex.getMessage();
					if (errorMessage == null) {
						errorMessage = ex.getClass().getName();
					}
					MessageBox box = new MessageBox(finalShell, 
						SWT.ICON_ERROR | SWT.OK | SWT.CANCEL);
					box.setText("Unable to export file data!");
					box.setMessage(
						  "Unable to export '" + filename + "'.\n\n"
					    + "AppleCommander was unable to save the disk\n"
					    + "data.  The system error given was '"
					    + errorMessage + "'\n\n"
						+ "Sorry!\n\n"
						+ "Press OK to continue export or CANCEL to cancel export.");
					int button = box.open();
					if (button == SWT.CANCEL) break;	// break out of loop
				}
			}
		}
	}
	/**
	 * Launch the compile file wizard.
	 */
	protected void compileFileWizard() {
		FileEntry fileEntry = getSelectedFileEntry();
		CompileWizard wizard = new CompileWizard(shell, 
			imageManager, fileEntry.getFormattedDisk());
		wizard.setDirectory(userPreferences.getCompileDirectory());
		wizard.open();
		if (wizard.isWizardCompleted()) {
			String compileDirectory = wizard.getDirectory();
			compileFile(compileDirectory);
		}
	}
	/**
	 * Compile all selected files.
	 * FIXME: This is a near duplicate of exportFile.  Can they be merged?
	 */
	private void compileFile(String directory) {
		boolean promptForIndividualFiles = (directory == null);
		TableItem[] selection = fileTable.getSelection();
		for (int i=0; i<selection.length; i++) {
			TableItem tableItem = selection[i];
			FileEntry fileEntry = (FileEntry) tableItem.getData();
			String filename = null;
			if (promptForIndividualFiles) {
				FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
				fileDialog.setFilterPath(userPreferences.getCompileDirectory());
				fileDialog.setFileName(fileEntry.getFilename() + ".S");
				filename = fileDialog.open();
				directory = fileDialog.getFilterPath();
			} else {
				filename = directory + File.separator + AppleUtil.
					getNiceFilename(fileEntry.getFilename() + ".S");
			}
			if (filename != null) {
				userPreferences.setCompileDirectory(directory);
				try {
					File file = new File(filename);
					if (file.exists()) {
						Shell finalShell = shell;
						MessageBox box = new MessageBox(finalShell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
						box.setText("File already exists!");
						box.setMessage(
							"The file '" + filename + "' already exists. "
							+ "Do you want to over-write it?");
						if (box.open() == SWT.NO) {
							return;	// do not overwrite file
						}
					}
					ApplesoftCompiler compiler = new ApplesoftCompiler(fileEntry);
					byte[] assembly = compiler.compile();
					OutputStream outputStream = new FileOutputStream(file);
					outputStream.write(assembly);
					outputStream.close();
				} catch (Exception ex) {
					Shell finalShell = shell;
					String errorMessage = ex.getMessage();
					if (errorMessage == null) {
						errorMessage = ex.getClass().getName();
					}
					MessageBox box = new MessageBox(finalShell, 
						SWT.ICON_ERROR | SWT.OK | SWT.CANCEL);
					box.setText("Unable to compile file!");
					box.setMessage(
						  "Unable to compile '" + filename + "'.\n\n"
						+ "AppleCommander was unable to compile the file.\n"
						+ "The system error given was '"
						+ errorMessage + "'\n\n"
						+ "Sorry!\n\n"
						+ "Press OK to continue compiles or CANCEL to cancel compiles.");
					int button = box.open();
					if (button == SWT.CANCEL) break;	// break out of loop
				}
			}
		}
	}
	/**
	 * Delete the currently selected files.
	 */
	protected void deleteFile() {
		TableItem[] selection = fileTable.getSelection();

		MessageBox box = new MessageBox(shell, 
			SWT.ICON_ERROR | SWT.YES | SWT.NO);
		box.setText("Are you sure?");
		box.setMessage(
			"Are you sure you want to delete "
			+ ((selection.length > 1) ? "these files" : "this file")
			+ "?\n\n"
			+ "Choose YES to proceed or NO to cancel.");
		int button = box.open();
		if (button == SWT.YES) {
			for (int i=0; i<selection.length; i++) {
				TableItem tableItem = selection[i];
				FileEntry fileEntry = (FileEntry) tableItem.getData();
				fileEntry.delete();
			}
			fillFileTable(currentFileList);
			saveToolItem.setEnabled(true);
		}
	}
	/**
	 * Start the import wizard and import the selected files.
	 */
	protected void importFiles() {
		//FIXME: This code has become really ugly!
		TreeItem treeItem = directoryTree.getSelection()[0];
		DirectoryEntry directory = (DirectoryEntry) treeItem.getData();
		ImportWizard wizard = new ImportWizard(shell, 
			imageManager, directory.getFormattedDisk());
		wizard.open();
		if (wizard.isWizardCompleted()) {
			Shell dialog = null;
			try {
				List specs = wizard.getImportSpecifications();
				// Progress meter for import wizard:
				dialog = new Shell(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
				dialog.setText("Importing files...");
				GridLayout layout = new GridLayout();
				layout.horizontalSpacing = 5;
				layout.makeColumnsEqualWidth = false;
				layout.marginHeight = 5;
				layout.marginWidth = 5;
				layout.numColumns = 2;
				layout.verticalSpacing = 5;
				dialog.setLayout(layout);
				Label label = new Label(dialog, SWT.NONE);
				label.setText("Processing:");
				Label countLabel = new Label(dialog, SWT.NONE);
				GridData gridData = new GridData();
				gridData.widthHint = 300;
				countLabel.setLayoutData(gridData);
				label = new Label(dialog, SWT.NONE);
				label.setText("Filename:");
				Label nameLabel = new Label(dialog, SWT.NONE);
				gridData = new GridData();
				gridData.widthHint = 300;
				nameLabel.setLayoutData(gridData);
				gridData = new GridData(GridData.FILL_HORIZONTAL);
				gridData.horizontalSpan = 2;
				gridData.grabExcessHorizontalSpace = true;
				ProgressBar progressBar = new ProgressBar(dialog, SWT.NONE);
				progressBar.setLayoutData(gridData);
				progressBar.setMinimum(0);
				progressBar.setMaximum(specs.size());
				dialog.pack();
				SwtUtil.center(shell, dialog);
				dialog.open();
				// begin the import:
				for (int i=0; i<specs.size(); i++) {
					ImportSpecification spec = 
						(ImportSpecification) specs.get(i);
					countLabel.setText("File " + (i+1) + " of " + specs.size());
					nameLabel.setText(spec.getSourceFilename());
					progressBar.setSelection(i);
					ByteArrayOutputStream buffer = 
						new ByteArrayOutputStream();
					InputStream input = 
						new FileInputStream(spec.getSourceFilename());
					int data;
					while ((data = input.read()) != -1) {
						buffer.write(data);
					}
					FileEntry fileEntry = directory.createFile();
					fileEntry.setFilename(spec.getTargetFilename());
					fileEntry.setFiletype(spec.getFiletype());
					if (fileEntry.needsAddress()) {
						fileEntry.setAddress(spec.getAddress());
					}
					try {
						fileEntry.setFileData(buffer.toByteArray());
					} catch (ProdosDiskSizeDoesNotMatchException ex) {
						MessageBox yesNoPrompt = new MessageBox(shell,
							SWT.ICON_QUESTION | SWT.YES | SWT.NO);
						yesNoPrompt.setText("Resize disk?");
						yesNoPrompt.setMessage("This disk needs to be resized to match "
							+ "the formatted capacity.  This should be an "
							+ "ApplePC HDV disk iamge - they typically start "
							+ "at 0 bytes and grow to the maximum capacity "
							+ "(32MB).  Resize the disk?");
						int answer = yesNoPrompt.open();
						if (answer == SWT.YES) {
							ProdosFormatDisk prodosDisk = (ProdosFormatDisk) 
								fileEntry.getFormattedDisk();
							prodosDisk.resizeDiskImage();
							fileEntry.setFileData(buffer.toByteArray());
						}
					}
				}
			} catch (Exception ex) {
				MessageBox box = new MessageBox(shell, 
					SWT.ICON_ERROR | SWT.OK);
				box.setText("Unable to import file(s)!");
				box.setMessage(
					  "An error occured during import.\n\n"
				    + "'" + ex.getMessage() + "'");
				box.open();
			}
			dialog.close();
			dialog.dispose();
			changeCurrentFormat(currentFormat);
			saveToolItem.setEnabled(true);
		}
	}
	/**
	 * Sort the file table by the specified columnIndex.
	 */
	protected void sortFileTable(int columnIndex) {
		Collections.sort(currentFileList, new FileEntryComparator(columnIndex, currentFormat));
		fillFileTable(currentFileList);
	}
	/**
	 * Helper function for building fileTree.
	 */
	protected void addDirectoriesToTree(TreeItem directoryItem, DirectoryEntry directoryEntry) {
		Iterator files = directoryEntry.getFiles().iterator();
		while (files.hasNext()) {
			final FileEntry entry = (FileEntry) files.next();
			if (entry.isDirectory()) {
				TreeItem item = new TreeItem(directoryItem, SWT.BORDER);
				item.setText(entry.getFilename());
				item.setData(entry);
				addDirectoriesToTree(item, (DirectoryEntry)entry);
			}
		}
	}
	/**
	 * Creates the FILE tab toolbar.
	 */
	private void createFileToolBar(Composite composite, Object layoutData) {
		toolBar = new ToolBar(composite, SWT.FLAT);
		toolBar.addListener(SWT.KeyUp, createToolbarCommandHandler());
		if (layoutData != null) toolBar.setLayoutData(layoutData);

		standardFormatToolItem = new ToolItem(toolBar, SWT.RADIO);
		standardFormatToolItem.setImage(imageManager.get(ImageManager.ICON_STANDARD_FILE_VIEW));
		standardFormatToolItem.setText("Standard");
		standardFormatToolItem.setToolTipText("Displays files in standard format (F2)");
		standardFormatToolItem.setSelection(true);
		standardFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_STANDARD);
			}
		});
		nativeFormatToolItem = new ToolItem(toolBar, SWT.RADIO);
		nativeFormatToolItem.setImage(imageManager.get(ImageManager.ICON_NATIVE_FILE_VIEW));
		nativeFormatToolItem.setText("Native");
		nativeFormatToolItem.setToolTipText("Displays files in native format for the operating system (F3)");
		nativeFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_NATIVE);
			}
		});
		detailFormatToolItem = new ToolItem(toolBar, SWT.RADIO);
		detailFormatToolItem.setImage(imageManager.get(ImageManager.ICON_DETAIL_FILE_VIEW));
		detailFormatToolItem.setText("Detail");
		detailFormatToolItem.setToolTipText("Displays files in with full details (F4)");
		detailFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_DETAIL);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		showDeletedFilesToolItem = new ToolItem(toolBar, SWT.CHECK);
		showDeletedFilesToolItem.setImage(imageManager.get(ImageManager.ICON_SHOW_DELETED_FILES));
		showDeletedFilesToolItem.setText("Deleted");
		showDeletedFilesToolItem.setToolTipText("Show deleted files (F5)");
		showDeletedFilesToolItem.setEnabled(disks[0].supportsDeletedFiles());
		showDeletedFilesToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				showDeletedFiles = showDeletedFilesToolItem.getSelection();
				fillFileTable(currentFileList);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		importToolItem = new ToolItem(toolBar, SWT.PUSH);
		importToolItem.setImage(imageManager.get(ImageManager.ICON_IMPORT_FILE));
		importToolItem.setText("Import...");
		importToolItem.setToolTipText("Import a file (CTRL+I)");
		importToolItem.setEnabled(disks[0].canCreateFile() && disks[0].canWriteFileData());
		importToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				importFiles();
			}
		});
		
		exportToolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
		exportToolItem.setImage(imageManager.get(ImageManager.ICON_EXPORT_FILE));
		exportToolItem.setText("Export...");
		exportToolItem.setToolTipText("Export a file (CTRL+E)");
		exportToolItem.setEnabled(false);
		exportToolItem.addSelectionListener(
			new DropDownSelectionListener(createFileExportMenu(SWT.NONE)));
		exportToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				if (event.detail != SWT.ARROW) {
					exportFileWizard();
				}
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		compileToolItem = new ToolItem(toolBar, SWT.PUSH);
		compileToolItem.setImage(imageManager.get(ImageManager.ICON_COMPILE_FILE));
		compileToolItem.setText("Compile");
		compileToolItem.setToolTipText("Compile a BASIC program (CTRL+C)");
		compileToolItem.setEnabled(false);
		compileToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				if (event.detail != SWT.ARROW) {
					compileFileWizard();
				}
			}
		});
		viewFileItem = new ToolItem(toolBar, SWT.PUSH);
		viewFileItem.setImage(imageManager.get(ImageManager.ICON_VIEW_FILE));
		viewFileItem.setText("View");
		viewFileItem.setToolTipText("View file (CTRL+V)");
		viewFileItem.setEnabled(false);
		viewFileItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				if (event.detail != SWT.ARROW) {
					viewFile(null);
				}
			}
		});
		printToolItem = new ToolItem(toolBar, SWT.PUSH);
		printToolItem.setImage(imageManager.get(ImageManager.ICON_PRINT_FILE));
		printToolItem.setText("Print");
		printToolItem.setToolTipText("Print directory listing...");
		printToolItem.setEnabled(true);
		printToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				if (event.detail != SWT.ARROW) {
					print();
				}
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		deleteToolItem = new ToolItem(toolBar, SWT.PUSH);
		deleteToolItem.setImage(imageManager.get(ImageManager.ICON_DELETE_FILE));
		deleteToolItem.setText("Delete");
		deleteToolItem.setToolTipText("Delete a file (CTRL+D)");
		deleteToolItem.setEnabled(false);
		deleteToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				deleteFile();
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		changeOrderToolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
		changeOrderToolItem.setImage(imageManager.get(ImageManager.ICON_CHANGE_IMAGE_ORDER));
		changeOrderToolItem.setText("Re-order...");
		changeOrderToolItem.setToolTipText("Change image order (CTRL+O)");
		ImageOrder imageOrder = disks[0].getImageOrder();
		changeOrderToolItem.setEnabled(
			(imageOrder.isBlockDevice() 
				&& imageOrder.getBlocksOnDevice() == Disk.PRODOS_BLOCKS_ON_140KB_DISK)
			|| (imageOrder.isTrackAndSectorDevice() 
				&& imageOrder.getSectorsPerDisk() == Disk.DOS33_SECTORS_ON_140KB_DISK));
		changeOrderMenu = createChangeImageOrderMenu(SWT.NONE);
		changeOrderToolItem.addSelectionListener(
			new DropDownSelectionListener(changeOrderMenu));
		changeOrderToolItem.addSelectionListener(new SelectionAdapter () {
			/** 
			 * Whenever the button is clicked, force the menu to be shown
			 */
			public void widgetSelected(SelectionEvent event) {
				Rectangle rect = changeOrderToolItem.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = toolBar.toDisplay(pt);
				changeOrderMenu.setLocation(pt.x, pt.y);
				changeOrderMenu.setVisible(true);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		saveToolItem = new ToolItem(toolBar, SWT.PUSH);
		saveToolItem.setImage(imageManager.get(ImageManager.ICON_SAVE_DISK_IMAGE));
		saveToolItem.setText("Save");
		saveToolItem.setToolTipText("Save disk image (CTRL+S)");
		saveToolItem.setEnabled(disks[0].hasChanged());	// same physical disk
		saveToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				save();
			}
		});

		saveAsToolItem = new ToolItem(toolBar, SWT.PUSH);
		saveAsToolItem.setImage(imageManager.get(ImageManager.ICON_SAVE_DISK_IMAGE_AS));
		saveAsToolItem.setText("Save As");
		saveAsToolItem.setToolTipText("Save disk image as... (CTRL+SHIFT+S)");
		saveAsToolItem.setEnabled(true);	// We can always Save As...
		saveAsToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				saveAs();
			}
		});

		toolBar.pack();
	}
	/**
	 * Change the current format and refresh the display.
	 */
	protected void changeCurrentFormat(int newFormat) {
		TreeItem selection = directoryTree.getSelection()[0];
		Object data = selection.getData();
		DirectoryEntry directory = (DirectoryEntry) data;
		List fileList = directory.getFiles();
		
		formatChanged = (currentFormat != newFormat);
		if (formatChanged || !fileList.equals(currentFileList)) {
			preserveColumnWidths();	// must be done before assigning newFormat
			currentFormat = newFormat;
			fillFileTable(fileList);

			// Ensure that the control buttons are set appropriately.
			// Primarly required for keyboard interface.
			standardFormatToolItem.setSelection(
				currentFormat == FormattedDisk.FILE_DISPLAY_STANDARD);
			nativeFormatToolItem.setSelection(
				currentFormat == FormattedDisk.FILE_DISPLAY_NATIVE);
			detailFormatToolItem.setSelection(
				currentFormat == FormattedDisk.FILE_DISPLAY_DETAIL);
		}
	}
	/**
	 * Handle SaveAs.
	 */
	protected void saveAs() {
		FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
		fileDialog.setFilterPath(userPreferences.getSaveDirectory());
		fileDialog.setFileName(disks[0].getFilename());
		fileDialog.setText("Please choose a location and name for your disk image:");
		String fullpath = fileDialog.open();
		userPreferences.setSaveDirectory(fileDialog.getFilterPath());
		if (fullpath == null) {
			return;	// user pressed cancel
		}
		try {
			disks[0].saveAs(fullpath);
			diskWindow.setStandardWindowTitle();
			saveToolItem.setEnabled(disks[0].hasChanged());
		} catch (IOException ex) {
			showSaveError(ex);
		}
	}
	/**
	 * Handle save.
	 * If this is the first time a disk has been saved (a new image),
	 * default to the SaveAs behavior.
	 */
	protected void save() {
		try {
			if (disks[0].isNewImage()) {
				saveAs();	// no directory -> assume a new/unsaved image
				return;
			}
			disks[0].save();
			saveToolItem.setEnabled(disks[0].hasChanged());
		} catch (IOException ex) {
			showSaveError(ex);
		}
	}
	/**
	 * Display the Save error dialog box.
	 * @see #save
	 * @see #saveAs
	 */
	protected void showSaveError(IOException ex) {
		Shell finalShell = shell;
		String errorMessage = ex.getMessage();
		if (errorMessage == null) {
			errorMessage = ex.getClass().getName();
		}
		MessageBox box = new MessageBox(finalShell, 
			SWT.ICON_ERROR | SWT.CLOSE);
		box.setText("Unable to save disk image!");
		box.setMessage(
			  "Unable to save '" + disks[0].getFilename() + "'.\n\n"
		    + "AppleCommander was unable to save the disk\n"
		    + "image.  The system error given was '"
		    + errorMessage + "'\n\n"
			+ "Sorry!");
		box.open();
	}
	/**
	 * Create the keyboard handler for the directory pane.
	 * These are keys that are <em>only</em> active in the directory
	 * viewer.  See createToolbarCommandHandler for the general application
	 * keyboard handler.  
	 * @see #createToolbarCommandHandler
	 */
	private Listener createDirectoryKeyboardHandler() {
		return new Listener() {
			public void handleEvent(Event event) {
				if (event.type == SWT.KeyUp) {
					TreeItem[] treeItem = null;
					if ((event.stateMask & SWT.CTRL) != 0) {
						switch (event.character) {
							case '-':
								treeItem = directoryTree.getSelection();
								setDirectoryExpandedStates(treeItem[0], false);
								break;
							case '+':
								treeItem = directoryTree.getSelection();
								setDirectoryExpandedStates(treeItem[0], true);
								break;
						}
					} else {	// assume no control and no alt
						switch (event.character) {
							case '-':
								treeItem = directoryTree.getSelection();
								treeItem[0].setExpanded(false);
								break;
							case '+':
								treeItem = directoryTree.getSelection();
								treeItem[0].setExpanded(true);
								break;
						}
					}
				}
			}		
		};
	}
	/**
	 * Open up the view file window for the currently selected file.
	 */
	protected void viewFile(Class fileFilterClass) {
		FileEntry fileEntry = getSelectedFileEntry();
		if (fileEntry.isDeleted()) {
			showErrorDialogBox("Unable to view a deleted file!",
				"Sorry, you cannot view a deleted file.", null);
		} else if (fileEntry.isDirectory()) {
			TreeItem item = findDirectoryItem(directoryTree.getSelection()[0].getItems(), fileEntry.getFilename(), 1, 0);
			if (item != null) {
				directoryTree.showItem(item);
				directoryTree.setSelection(new TreeItem[] { item });
				changeCurrentFormat(currentFormat);		// minor hack
			}
		} else {	// Assuming a normal file!
			FileViewerWindow window = null;
			FileFilter fileFilter = null;
			try {
				fileFilter = (FileFilter) fileFilterClass.newInstance();
			} catch (NullPointerException ex) {
				// This is expected
			} catch (InstantiationException e) {
				showSystemErrorDialogBox(e);
			} catch (IllegalAccessException e) {
				showSystemErrorDialogBox(e);
			}
			if (fileFilter != null) {
				window = new FileViewerWindow(shell, fileEntry, imageManager, fileFilter);
			} else {
				window = new FileViewerWindow(shell, fileEntry, imageManager);
			}
			window.open();
		}
	}
	/**
	 * Display an error dialog box with the OK button.
	 * Note that the Throwable message may be embedded in the displayed message.
	 * TODO: Should this be a shared method somewhere?
	 */
	protected void showErrorDialogBox(String title, String message, Throwable throwable) {
		MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		box.setText(title);
		if (throwable != null) {
			message.replaceAll("%MESSAGE%", throwable.getMessage());
		}
		box.setMessage(message);
		box.open();
	}
	/**
	 * Display a system-level error dialog box.
	 */
	protected void showSystemErrorDialogBox(Throwable throwable) {
		showErrorDialogBox("A system error occurred!",
			"A system error occurred.  The message given was '%MESSAGE%'.",
			throwable);
	}
	/**
	 * Locate a named item in the directory tree.
	 */
	protected TreeItem findDirectoryItem(TreeItem[] treeItems, String name, int maxDepth, int currentDepth) {
		if (maxDepth == currentDepth) return null;
		for (int i=0; i<treeItems.length; i++) {
			if (name.equals(treeItems[i].getText())) {
				return treeItems[i];
			} else if (treeItems[i].getItems() != null) {
				TreeItem item = findDirectoryItem(treeItems[i].getItems(), name, maxDepth, currentDepth+1);
				if (item != null) {
					return item;
				}
			}
		}
		return null;
	}
	/**
	 * Create the keyboard handler for the file pane.
	 * These are keys that are <em>only</em> active in the file
	 * viewer.  See createToolbarCommandHandler for the general application
	 * keyboard handler.  
	 * @see #createToolbarCommandHandler
	 */
	private Listener createFileKeyboardHandler() {
		return new Listener() {
			public void handleEvent(Event event) {
				FileEntry fileEntry = getSelectedFileEntry();
				if (fileEntry != null && event.type == SWT.KeyUp && (event.stateMask & SWT.CTRL) != 0) {
					switch (event.character) {
						case CTRL_C:	// Compile Wizard
							if (compileToolItem.isEnabled()) {
								compileFileWizard();
							}
							break;
						case CTRL_D:	// Delete file
							if (deleteToolItem.isEnabled()) {
								deleteFile();
							}
							break;
						case CTRL_E:	// Export Wizard
							exportFileWizard();
							break;
						case CTRL_V:	// View file
							viewFile(null);
							break;
					}		
				}
			}
		};
	}
	/**
	 * The toolbar command handler contains the global toolbar
	 * actions.  This does not include file-specific actions.
	 * The intent is that the listener is then added to multiple
	 * visual components (i.e., the file listing as well as the
	 * directory listing).
	 */
	private Listener createToolbarCommandHandler() {
		return new Listener() {
			public void handleEvent(Event event) {
				if (event.type == SWT.KeyUp) {
					if ((event.stateMask & SWT.CTRL) != 0) {	// CTRL key held
						if ((event.stateMask & SWT.SHIFT) != 0) {	// SHIFT key held
							switch (event.character) {
								case CTRL_S:	// Save As...
									saveAs();
									break;
							}
						} else {
							switch (event.character) {
								case CTRL_I:	// Import Wizard
									importFiles();
									break;
								case CTRL_P:	// Print...
									print();
									break;
								case CTRL_S:	// Save
									if (saveToolItem.isEnabled()) {
										save();
									}
									break;
							}
						}
					} else {	// No CTRL key
						switch (event.keyCode) {
							case SWT.F2:	// Standard file display
								changeCurrentFormat(FormattedDisk.FILE_DISPLAY_STANDARD);
								break;
							case SWT.F3:	// Native file display
								changeCurrentFormat(FormattedDisk.FILE_DISPLAY_NATIVE);
								break;
							case SWT.F4:	// Detail file display
								changeCurrentFormat(FormattedDisk.FILE_DISPLAY_DETAIL);
								break;
							case SWT.F5:	// Show deleted files
								showDeletedFiles = !showDeletedFilesToolItem.getSelection();
								showDeletedFilesToolItem.setSelection(showDeletedFiles);
								fillFileTable(currentFileList);
								break;
						}
					}
				}
			}
		};
	}
	/**
	 * Get the currently selected FileEntry.  Note that this
	 * can return null if there are none selected.  Also, if there
	 * are multiple files selected, this is not complete.
	 */
	protected FileEntry getSelectedFileEntry() {
		FileEntry fileEntry = null;
		if (fileTable.getSelectionIndex() >= 0) {
			fileEntry = (FileEntry) fileTable.getItem(fileTable.getSelectionIndex()).getData();
		}
		return fileEntry;
	}
	/**
	 * Internal class that controls printing of a file listing.
	 */
	private class Printing implements Runnable {
		private Printer printer;
		private int y;
		private int x;
		private Rectangle clientArea;
		private GC gc;
		private List fileHeaders;
		private int[] columnWidths;
		private int[] columnPosition;
		private Font normalFont;
		private Font headerFont;
		private String filename;
		private int page = 1;
		private int dpiY;
		private int dpiX;
		public Printing(Printer printer) {
			this.printer = printer;
		}
		public void run() {
			if (printer.startJob(disks[0].getFilename())) {
				clientArea = printer.getClientArea();
				dpiY = printer.getDPI().y;
				dpiX = printer.getDPI().x;
				// Setup 1" margin:
				Rectangle trim = printer.computeTrim(0, 0, 0, 0);
				clientArea.x = dpiX + trim.x; 				
				clientArea.y = dpiY + trim.y;
				clientArea.width -= (clientArea.x + trim.width);
				clientArea.height -= (clientArea.y + trim.height);
				// Set default values: 
				y = clientArea.y;
				x = clientArea.x;
				gc = new GC(printer);
				int fontSize = 12;
				if (currentFormat == FormattedDisk.FILE_DISPLAY_NATIVE) {
					fontSize = 10;
				} else if (currentFormat == FormattedDisk.FILE_DISPLAY_DETAIL) {
					fontSize = 8;
				}
				normalFont = new Font(printer, "", fontSize, SWT.NORMAL);
				headerFont = new Font(printer, "", fontSize, SWT.BOLD);
				for (int i=0; i<disks.length; i++) {
					FormattedDisk disk = disks[i];
					filename = disk.getFilename();
					fileHeaders =  disk.getFileColumnHeaders(currentFormat);
					gc.setFont(headerFont);
					computeHeaderWidths();
					printFileHeaders();
					gc.setFont(normalFont);
					println(disk.getDiskName());
					printFiles(disk, 1);
				}
				if (y != clientArea.y) {	// partial page
					printFooter();
					printer.endPage();
				}
				printer.endJob();
			}
		}
		protected void computeHeaderWidths() {
			int totalWidth = 0;
			int[] widths = new int[fileHeaders.size()];
			for (int i=0; i<fileHeaders.size(); i++) {
				FileColumnHeader header = (FileColumnHeader) fileHeaders.get(i);
				widths[i] = (header.getMaximumWidth() >= header.getTitle().length()) ?
					header.getMaximumWidth() : header.getTitle().length();
				totalWidth+= widths[i];
			}
			columnWidths = new int[fileHeaders.size()];
			columnPosition = new int[fileHeaders.size()];
			int position = clientArea.x;
			for (int i=0; i<fileHeaders.size(); i++) {
				columnWidths[i] = (widths[i] * clientArea.width) / totalWidth;
				columnPosition[i] = position; 
				position+= columnWidths[i];
			}
		}
		protected void printFileHeaders() {
			for (int i=0; i<fileHeaders.size(); i++) {
				FileColumnHeader header = (FileColumnHeader) fileHeaders.get(i);
				print(i, header.getTitle(), header.getAlignment());
			}
			println("");
		}
		protected void print(int column, String text, int alignment) {
			int x0 = columnPosition[column];
			int x1 = (column+1 < columnPosition.length) ?
					columnPosition[column+1] : clientArea.width;
			int w = columnWidths[column];
			switch (alignment) {
				case FileColumnHeader.ALIGN_LEFT:
					x = x0;
					break;
				case FileColumnHeader.ALIGN_CENTER:
					x = x0 + (w - gc.stringExtent(text).x)/2;
					break;
				case FileColumnHeader.ALIGN_RIGHT:
					x = x1 - gc.stringExtent(text).x;
					break;
			}
			gc.drawString(text,x,y);
		}
		protected void println(String string) {
			if (y == clientArea.y) {	// start of page
				printer.startPage();
				printHeader();
				y++;	// hack
				printFileHeaders();
			}
			gc.drawString(string, x, y);
			x = clientArea.x;
			y+= gc.stringExtent(string).y;
			if (y > (clientArea.y + clientArea.height)) {	// filled a page
				printFooter();
				printer.endPage();
				y = clientArea.y;
			}
		}
		protected void printHeader() {
			Point point = gc.stringExtent(filename);
			gc.drawString(filename, 
				clientArea.x + (clientArea.width - point.x)/2, 
				y - dpiY + point.y);
		}
		protected void printFooter() {
			String text = "Page " + Integer.toString(page);
			Point point = gc.stringExtent(text);
			gc.drawString(text, 
				clientArea.x + (clientArea.width - point.x)/2, 
				clientArea.y + clientArea.height + dpiY - point.y);
			page++;
		}
		protected void printFiles(DirectoryEntry directory, int level) {
			Iterator iterator = directory.getFiles().iterator();
			while (iterator.hasNext()) {
				FileEntry fileEntry = (FileEntry) iterator.next();
				if (!fileEntry.isDeleted() || showDeletedFiles) {
					List columns = fileEntry.getFileColumnData(currentFormat);
					for (int i=0; i<columns.size(); i++) {
						FileColumnHeader header = (FileColumnHeader) fileHeaders.get(i);
						String text = (String)columns.get(i);
						if ("name".equalsIgnoreCase(header.getTitle())) {
							for (int l=0; l<level; l++) {
								text = "  " + text;
							}
						}
						print(i, text, header.getAlignment());
					}
					println("");
					if (fileEntry.isDirectory()) {
						printFiles((DirectoryEntry)fileEntry, level+1);
					}
				}
			}
		}
	}
	/**
	 * Print the file listing for this disk. 
	 */
	protected void print() {
		PrintDialog printDialog = new PrintDialog(shell);
		PrinterData printerData = printDialog.open();
		if (printerData == null) {
			// cancelled
			return;
		}
		final Printer printer = new Printer(printerData);
		new Thread() {
			public void run() {
				new Printing(printer).run();
				printer.dispose();
			}
		}.start();
	}
	
	/**
	 * Change the disk to a new image order.  It is assumed that the order is
	 * appropriate - that should be handled by the menuing.
	 */
	protected void changeImageOrder(String extension, ImageOrder newImageOrder) {
		try {
			disks[0].changeImageOrder(newImageOrder);
			String filename = disks[0].getFilename();
			if (disks[0].isCompressed()) {	// extra ".gz" at end
				int chop = filename.lastIndexOf(".", filename.length()-4);
				filename = filename.substring(0, chop+1) + extension + ".gz";
			} else {
				int chop = filename.lastIndexOf(".");
				filename = filename.substring(0, chop+1) + extension;
			}
			disks[0].setFilename(filename);
			diskWindow.setStandardWindowTitle();
		} catch (Throwable t) {
			Shell finalShell = shell;
			String errorMessage = t.getMessage();
			if (errorMessage == null) {
				errorMessage = t.getClass().getName();
			}
			MessageBox box = new MessageBox(finalShell, 
				SWT.ICON_ERROR | SWT.OK);
			box.setText("Unable to change image order!");
			box.setMessage(
				  "Unable to reorder disk image.\n\n"
				+ "AppleCommander was unable to change the disk order.\n"
				+ "The system error given was '"
				+ errorMessage + "'\n\n"
				+ "Sorry!\n\n"
				+ "Press OK to continue.");
			box.open();
		}
	}

	/**
	 * Construct the popup menu for the export button on the toolbar.
	 */
	protected Menu createChangeImageOrderMenu(int style) {
		Menu menu = new Menu(shell, style);
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent event) {
				Menu theMenu = (Menu) event.getSource();
				MenuItem[] subItems = theMenu.getItems();
				// Nibble Order (*.nib)
				subItems[0].setSelection(disks[0].isNibbleOrder());
				// DOS Order (*.dsk)
				subItems[1].setSelection(disks[0].isDosOrder());
				// ProDOS Order (*.po)
				subItems[2].setSelection(disks[0].isProdosOrder());
			}
		});
			
		MenuItem item = new MenuItem(menu, SWT.RADIO);
		item.setText("Nibble Order (*.nib)");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (!disks[0].isNibbleOrder()) {
					NibbleOrder nibbleOrder = new NibbleOrder(
						new ByteArrayImageLayout(Disk.APPLE_140KB_NIBBLE_DISK));
					nibbleOrder.format();
					changeImageOrder("nib", nibbleOrder);
				}
			}
		});

		item = new MenuItem(menu, SWT.RADIO);
		item.setText("DOS Order (*.dsk)");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (!disks[0].isDosOrder()) {
					changeImageOrder("dsk", new DosOrder(
						new ByteArrayImageLayout(Disk.APPLE_140KB_DISK)));
				}
			}
		});

		item = new MenuItem(menu, SWT.RADIO);
		item.setText("ProDOS Order (*.po)");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (!disks[0].isProdosOrder()) {
					changeImageOrder("po", new ProdosOrder(
						new ByteArrayImageLayout(Disk.APPLE_140KB_DISK)));
				}
			}
		});
		
		return menu;
	}
}
