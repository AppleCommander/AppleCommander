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

import com.webcodepro.applecommander.compiler.ApplesoftCompiler;
import com.webcodepro.applecommander.storage.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.storage.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.BinaryFileFilter;
import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileEntryComparator;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.ProdosDiskSizeDoesNotMatchException;
import com.webcodepro.applecommander.storage.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.TextFileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;
import com.webcodepro.applecommander.ui.ImportSpecification;
import com.webcodepro.applecommander.ui.UserPreferences;
import com.webcodepro.applecommander.util.AppleUtil;

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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

/**
 * Build the Disk File tab for the Disk Window.
 * <p>
 * Date created: Nov 17, 2002 9:46:53 PM
 * @author: Rob Greene
 */
public class DiskExplorerTab {
	private static final char CTRL_C = 'C' - '@';
	private static final char CTRL_D = 'D' - '@';
	private static final char CTRL_E = 'E' - '@';
	private static final char CTRL_I = 'I' - '@';
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
	private ToolItem deleteToolItem;
	private ToolItem saveToolItem;
	private ToolItem saveAsToolItem;

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
		item.setImage(imageManager.getImportFileIcon());
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
				subItems[0].setEnabled(fileEntry != null);		// FIXME
				// Compile File
				subItems[1].setEnabled(fileEntry != null && fileEntry.canCompile());
				// Export File
				subItems[3].setEnabled(fileEntry != null);
				// Delete File
				subItems[5].setEnabled(fileEntry != null);
			}
		});
		
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&View\tCtrl+V");
		item.setAccelerator(SWT.CTRL+'V');
		item.setImage(imageManager.getViewFileIcon());

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&Compile...\tCtrl+C");
		item.setAccelerator(SWT.CTRL+'C');
		item.setImage(imageManager.getCompileIcon());
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				compileFileWizard();
			}
		});
		
		item = new MenuItem(menu, SWT.SEPARATOR);
		
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&Export\tCtrl+E");
		item.setAccelerator(SWT.CTRL+'E');
		item.setEnabled(disks[0].canReadFileData());
		item.setMenu(createFileExportMenu(SWT.DROP_DOWN));
		item.setImage(imageManager.getExportFileIcon());

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&Delete...\tCtrl+D");
		item.setAccelerator(SWT.CTRL+'D');
		item.setEnabled(disks[0].canDeleteFile());
		item.setImage(imageManager.getDeleteFileIcon());
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				deleteFile();
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
						// FIXME: Need appropriate logic..
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
					// No action defined at this time
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
		//importToolItem.setEnabled(false);
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
		standardFormatToolItem.setImage(imageManager.getStandardFileViewIcon());
		standardFormatToolItem.setText("Standard");
		standardFormatToolItem.setToolTipText("Displays files in standard format (F2)");
		standardFormatToolItem.setSelection(true);
		standardFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_STANDARD);
			}
		});
		nativeFormatToolItem = new ToolItem(toolBar, SWT.RADIO);
		nativeFormatToolItem.setImage(imageManager.getNativeFileViewIcon());
		nativeFormatToolItem.setText("Native");
		nativeFormatToolItem.setToolTipText("Displays files in native format for the operating system (F3)");
		nativeFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_NATIVE);
			}
		});
		detailFormatToolItem = new ToolItem(toolBar, SWT.RADIO);
		detailFormatToolItem.setImage(imageManager.getDetailFileViewIcon());
		detailFormatToolItem.setText("Detail");
		detailFormatToolItem.setToolTipText("Displays files in with full details (F4)");
		detailFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_DETAIL);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		showDeletedFilesToolItem = new ToolItem(toolBar, SWT.CHECK);
		showDeletedFilesToolItem.setImage(imageManager.getDeletedFilesIcon());
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
		importToolItem.setImage(imageManager.getImportFileIcon());
		importToolItem.setText("Import...");
		importToolItem.setToolTipText("Import a file (CTRL+I)");
		importToolItem.setEnabled(disks[0].canCreateFile() && disks[0].canWriteFileData());
		importToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				importFiles();
			}
		});
		
		exportToolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
		exportToolItem.setImage(imageManager.getExportFileIcon());
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
		compileToolItem.setImage(imageManager.getCompileIcon());
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
		viewFileItem.setImage(imageManager.getViewFileIcon());
		viewFileItem.setText("View");
		viewFileItem.setToolTipText("View file (CTRL+V)");
		viewFileItem.setEnabled(false);
		viewFileItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				if (event.detail != SWT.ARROW) {
				}
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		deleteToolItem = new ToolItem(toolBar, SWT.PUSH);
		deleteToolItem.setImage(imageManager.getDeleteFileIcon());
		deleteToolItem.setText("Delete");
		deleteToolItem.setToolTipText("Delete a file (CTRL+D)");
		deleteToolItem.setEnabled(false);
		deleteToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				deleteFile();
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		saveToolItem = new ToolItem(toolBar, SWT.PUSH);
		saveToolItem.setImage(imageManager.getSaveImageIcon());
		saveToolItem.setText("Save");
		saveToolItem.setToolTipText("Save disk image (CTRL+S)");
		saveToolItem.setEnabled(disks[0].hasChanged());	// same physical disk
		saveToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				save();
			}
		});

		saveAsToolItem = new ToolItem(toolBar, SWT.PUSH);
		saveAsToolItem.setImage(imageManager.getSaveAsIcon());
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
		fileDialog.setFileName(disks[0].getFilename());
		fileDialog.setText("Please choose a location and name for your disk image:");
		String fullpath = fileDialog.open();
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
							// TODO
							FileViewerWindow window = new FileViewerWindow(shell, fileEntry, imageManager);
							window.open();
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
}
