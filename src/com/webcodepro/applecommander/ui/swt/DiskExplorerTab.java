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

import com.webcodepro.applecommander.storage.AppleUtil;
import com.webcodepro.applecommander.storage.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.storage.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.BinaryFileFilter;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileEntryComparator;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.TextFileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;
import com.webcodepro.applecommander.ui.UserPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
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
	private Shell shell;
	private SashForm sashForm;
	private Tree directoryTree;
	private Table fileTable;
	private ToolBar toolBar;
	private ToolItem exportToolItem;
	private ToolItem importToolItem;
	private ToolItem deleteToolItem;
	private ToolItem saveToolItem;
	private ImageManager imageManager;

	private UserPreferences userPreferences = UserPreferences.getInstance();
	private FormattedDisk[] disks;
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
	public DiskExplorerTab(CTabFolder tabFolder, FormattedDisk[] disks, ImageManager imageManager) {
		this.disks = disks;
		this.shell = tabFolder.getShell();
		this.imageManager = imageManager;
		
		createFilesTab(tabFolder);
	}
	/**
	 * Dispose of resources.
	 */
	public void dispose() {
		sashForm.dispose();
		directoryTree.dispose();
		fileTable.dispose();
		exportToolItem.dispose();
		importToolItem.dispose();
		deleteToolItem.dispose();
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
						addDirectoriesToTree(item, entry);
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
		item.setText("Expand");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = directoryTree.getSelection();
				treeItem[0].setExpanded(true);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Collapse");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = directoryTree.getSelection();
				treeItem[0].setExpanded(false);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Expand All");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = directoryTree.getSelection();
				setDirectoryExpandedStates(treeItem[0], true);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Collapse All");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = directoryTree.getSelection();
				setDirectoryExpandedStates(treeItem[0], false);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());
		
		return menu;
	}
	/**
	 * Construct the popup menu for the file table on the File tab.
	 */
	protected Menu createFilePopupMenu() {
		Menu menu = new Menu(shell, SWT.POP_UP);
		
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Import...");
		item.setEnabled(disks[0].canCreateFile() && disks[0].canWriteFileData());
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				// FIXME
			}
		});

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Export");
		item.setEnabled(disks[0].canReadFileData());
		item.setMenu(createFileExportMenu(SWT.DROP_DOWN));

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Delete...");
		item.setEnabled(disks[0].canDeleteFile());
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
				awpFilter.setRendering(AppleWorksWordProcessorFileFilter.RENDER_AS_TEXT);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText("HTML");
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				awpFilter.setRendering(AppleWorksWordProcessorFileFilter.RENDER_AS_HTML);
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
			fileTable.setHeaderVisible(true);
			fileTable.setMenu(createFilePopupMenu());
			fileTable.addSelectionListener(new SelectionListener() {
				/**
				 * Single-click handler.
				 */
				public void widgetSelected(SelectionEvent event) {
					importToolItem.setEnabled(disks[0].canCreateFile() && disks[0].canWriteFileData());
					if (fileTable.getSelectionCount() > 0) {
						exportToolItem.setEnabled(disks[0].canReadFileData());
						deleteToolItem.setEnabled(disks[0].canDeleteFile());
					} else {
						exportToolItem.setEnabled(false);
						deleteToolItem.setEnabled(false);
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
	 * Sort the file table by the specified columnIndex.
	 */
	protected void sortFileTable(int columnIndex) {
		Collections.sort(currentFileList, new FileEntryComparator(columnIndex, currentFormat));
		fillFileTable(currentFileList);
	}
	/**
	 * Helper function for building fileTree.
	 */
	protected void addDirectoriesToTree(TreeItem directoryItem, FileEntry directoryEntry) {
		Iterator files = directoryEntry.getFiles().iterator();
		while (files.hasNext()) {
			final FileEntry entry = (FileEntry) files.next();
			if (entry.isDirectory()) {
				TreeItem item = new TreeItem(directoryItem, SWT.BORDER);
				item.setText(entry.getFilename());
				item.setData(entry);
				addDirectoriesToTree(item, entry);
			}
		}
	}
	/**
	 * Creates the FILE tab toolbar.
	 */
	private void createFileToolBar(Composite composite, Object layoutData) {
		toolBar = new ToolBar(composite, SWT.FLAT);
		if (layoutData != null) toolBar.setLayoutData(layoutData);

		ToolItem item = new ToolItem(toolBar, SWT.RADIO);
		item.setImage(imageManager.getStandardFileViewIcon());
		item.setText("Standard");
		item.setToolTipText("Displays files in standard format");
		item.setSelection(true);
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_STANDARD);
			}
		});
		item = new ToolItem(toolBar, SWT.RADIO);
		item.setImage(imageManager.getNativeFileViewIcon());
		item.setText("Native");
		item.setToolTipText("Displays files in native format for the operating system");
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_NATIVE);
			}
		});
		item = new ToolItem(toolBar, SWT.RADIO);
		item.setImage(imageManager.getDetailFileViewIcon());
		item.setText("Detail");
		item.setToolTipText("Displays files in with full details");
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				changeCurrentFormat(FormattedDisk.FILE_DISPLAY_DETAIL);
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		item = new ToolItem(toolBar, SWT.CHECK);
		item.setImage(imageManager.getDeletedFilesIcon());
		item.setText("Deleted");
		item.setToolTipText("Show deleted files");
		item.setEnabled(disks[0].supportsDeletedFiles());
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				ToolItem button = (ToolItem) e.getSource();
				showDeletedFiles = button.getSelection();
				fillFileTable(currentFileList);
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		importToolItem = new ToolItem(toolBar, SWT.PUSH);
		importToolItem.setImage(imageManager.getImportFileIcon());
		importToolItem.setText("Import...");
		importToolItem.setToolTipText("Import a file");
		importToolItem.setEnabled(true);
		importToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				// Start wizard:
				// FIXME - assumes 1st disk and does not support directories
				ImportWizard wizard = new ImportWizard(shell, 
					imageManager, disks[0]);
				wizard.open();
			}
		});
		
		exportToolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
		exportToolItem.setImage(imageManager.getExportFileIcon());
		exportToolItem.setText("Export...");
		exportToolItem.setToolTipText("Export a file");
		exportToolItem.setEnabled(false);
		exportToolItem.addSelectionListener(
			new DropDownSelectionListener(createFileExportMenu(SWT.NONE)));
		exportToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				if (event.detail != SWT.ARROW) {
					// Get a suggseted filter, if possible:
					FileEntry fileEntry = (FileEntry) fileTable.getSelection()[0].getData();
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
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		deleteToolItem = new ToolItem(toolBar, SWT.PUSH);
		deleteToolItem.setImage(imageManager.getDeleteFileIcon());
		deleteToolItem.setText("Delete");
		deleteToolItem.setToolTipText("Delete a file");
		deleteToolItem.setEnabled(false);
		deleteToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				deleteFile();
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		saveToolItem = new ToolItem(toolBar, SWT.PUSH);
		saveToolItem.setImage(imageManager.getSaveImageIcon());
		saveToolItem.setText("Save");
		saveToolItem.setToolTipText("Save disk image");
		saveToolItem.setEnabled(disks[0].hasChanged());	// same physical disk
		saveToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				save();
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
		List fileList = null;
		if (data instanceof FileEntry) {
			FileEntry directory = (FileEntry) data;
			fileList = directory.getFiles();
		} else  if (data instanceof FormattedDisk) {
			FormattedDisk disk = (FormattedDisk) data;
			fileList = disk.getFiles();
		}
		
		formatChanged = (currentFormat != newFormat);
		if (formatChanged || !fileList.equals(currentFileList)) {
			preserveColumnWidths();	// must be done before assigning newFormat
			currentFormat = newFormat;
			fillFileTable(fileList);
		}
	}
	/**
	 * Handle save.
	 */
	protected void save() {
		try {
			disks[0].save();
			saveToolItem.setEnabled(disks[0].hasChanged());
		} catch (IOException ex) {
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
	}
}
