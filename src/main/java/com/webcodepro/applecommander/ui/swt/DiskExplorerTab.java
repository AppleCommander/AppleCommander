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
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileEntryComparator;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.FileColumnHeader;
import com.webcodepro.applecommander.storage.filters.AppleWorksDataBaseFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksSpreadSheetFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.storage.filters.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.filters.AssemblySourceFileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.BusinessBASICFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.GutenbergFileFilter;
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
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.UserPreferences;
import com.webcodepro.applecommander.ui.swt.util.DropDownSelectionListener;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.ui.swt.wizard.compilefile.CompileWizard;
import com.webcodepro.applecommander.ui.swt.wizard.exportfile.ExportWizard;
import com.webcodepro.applecommander.ui.swt.wizard.importfile.ImportWizard;
import com.webcodepro.applecommander.util.AppleUtil;
import com.webcodepro.applecommander.util.Host;
import com.webcodepro.applecommander.util.StreamUtil;
import com.webcodepro.applecommander.util.TextBundle;

import io.github.applecommander.applesingle.AppleSingle;

/**
 * Build the Disk File tab for the Disk Window.
 * <p>
 * Date created: Nov 17, 2002 9:46:53 PM
 * @author Rob Greene
 *
 * Changed at: Dec 1, 2017
 * @author Lisias Toledo
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
	private Menu changeImageOrderMenu;

	private UserPreferences userPreferences = UserPreferences.getInstance();
	private TextBundle textBundle = UiBundle.getInstance();
	private FileFilter fileFilter;
	private GraphicsFileFilter graphicsFilter = new GraphicsFileFilter();
	private AppleWorksWordProcessorFileFilter awpFilter = new AppleWorksWordProcessorFileFilter();
	private GutenbergFileFilter gutenbergFilter = new GutenbergFileFilter();

	private int currentFormat = FormattedDisk.FILE_DISPLAY_STANDARD;
	private boolean formatChanged;
	private List<FileEntry> currentFileList;
	private Map<Integer,int[]> columnWidths = new HashMap<>();
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
		ctabitem.setText(textBundle.get("FilesTab")); //$NON-NLS-1$

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
				try {
					changeCurrentFormat(getCurrentFormat()); // minor hack
	            } catch (DiskException e) {
	                DiskExplorerTab.this.diskWindow.handle(e);
	            }
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
				try {
					Iterator<FileEntry> files = disks[i].getFiles().iterator();
					while (files.hasNext()) {
						FileEntry entry = (FileEntry) files.next();
						if (entry.isDirectory()) {
							TreeItem item = new TreeItem(diskItem, SWT.BORDER);
							item.setText(entry.getFilename());
							item.setData(entry);
							addDirectoriesToTree(item, (DirectoryEntry)entry);
						}
					}
	            } catch (DiskException e) {
	                this.diskWindow.handle(e);
	            }
			}
		}

		computeColumnWidths(FormattedDisk.FILE_DISPLAY_STANDARD);
		computeColumnWidths(FormattedDisk.FILE_DISPLAY_NATIVE);
		computeColumnWidths(FormattedDisk.FILE_DISPLAY_DETAIL);

		formatChanged = true;
		try {
			fillFileTable(disks[0].getFiles());
        } catch (DiskException e) {
            this.diskWindow.handle(e);
        }
		directoryTree.setSelection(new TreeItem[] { directoryTree.getItems()[0] });
	}
	/**
	 * Construct the popup menu for the directory table on the File tab.
	 * Using the first logical disk as the indicator for all logical disks.
	 */
	protected Menu createDirectoryPopupMenu() {
		Menu menu = new Menu(shell, SWT.POP_UP);
		
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("ExpandMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = getDirectoryTree().getSelection();
				treeItem[0].setExpanded(true);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("CollapseMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = getDirectoryTree().getSelection();
				treeItem[0].setExpanded(false);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("ExpandAllMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = getDirectoryTree().getSelection();
				setDirectoryExpandedStates(treeItem[0], true);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("CollapseAllMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				TreeItem[] treeItem = getDirectoryTree().getSelection();
				setDirectoryExpandedStates(treeItem[0], false);
			}
		});
		item.setEnabled(disks[0].canHaveDirectories());

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("CreateDirectoryMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				// FIXME need to create directory
			}
		});
		item.setEnabled(disks[0].canCreateDirectories());
		
		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("ImportMenuItem")); //$NON-NLS-1$
		item.setImage(imageManager.get(ImageManager.ICON_IMPORT_FILE));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					importFiles();
	            } catch (DiskException e) {
	            	DiskExplorerTab.this.diskWindow.handle(e);
	            }
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
				subItems[0].setEnabled(getDisk(0).canReadFileData() 
					&& fileEntry != null && !fileEntry.isDeleted() 
					&& !fileEntry.isDirectory());
				subItems[1].setEnabled(getDisk(0).canReadFileData() 
					&& fileEntry != null && !fileEntry.isDeleted() 
					&& !fileEntry.isDirectory());
				// Compile File
				subItems[3].setEnabled(getDisk(0).canReadFileData()
					&& fileEntry != null && fileEntry.canCompile()
					&& !fileEntry.isDeleted());
				// Export File
				subItems[5].setEnabled(getDisk(0).canReadFileData()
					&& fileEntry != null && !fileEntry.isDeleted()
					&& !fileEntry.isDirectory());
				subItems[6].setEnabled(getDisk(0).canReadFileData()
					&& fileEntry != null && !fileEntry.isDeleted()
					&& !fileEntry.isDirectory());
				// Delete File
				subItems[8].setEnabled(getDisk(0).canDeleteFile()
					&& fileEntry != null && !fileEntry.isDeleted());
			}
		});
		
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("ViewWizardMenuItem")); //$NON-NLS-1$
		item.setAccelerator(SWT.CTRL+'V');
		item.setImage(imageManager.get(ImageManager.ICON_VIEW_FILE));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					viewFile(null);
	            } catch (DiskException e) {
	            	DiskExplorerTab.this.diskWindow.handle(e);
	            }
			}
		});
	
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("ViewAsMenuItem")); //$NON-NLS-1$
		item.setMenu(createFileViewMenu(SWT.DROP_DOWN));
	
		item = new MenuItem(menu, SWT.SEPARATOR);
	
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("CompileMenuItem")); //$NON-NLS-1$
		item.setAccelerator(SWT.CTRL+'C');
		item.setImage(imageManager.get(ImageManager.ICON_COMPILE_FILE));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				compileFileWizard();
			}
		});
		
		item = new MenuItem(menu, SWT.SEPARATOR);
		
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("ExportWizardMenuItem")); //$NON-NLS-1$
		item.setAccelerator(SWT.CTRL+'E');
		item.setImage(imageManager.get(ImageManager.ICON_EXPORT_FILE));
	
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("ExportAsMenuItem")); //$NON-NLS-1$
		item.setMenu(createFileExportMenu(SWT.DROP_DOWN));
	
		item = new MenuItem(menu, SWT.SEPARATOR);
	
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("DeleteMenuItem")); //$NON-NLS-1$
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
		item.setText(textBundle.get("ViewAsTextMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					viewFile(TextFileFilter.class);
	            } catch (DiskException e) {
	            	DiskExplorerTab.this.diskWindow.handle(e);
	            }
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("VeiwAsGraphicsMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					viewFile(GraphicsFileFilter.class);
	            } catch (DiskException e) {
	            	DiskExplorerTab.this.diskWindow.handle(e);
	            }
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
		item.setText(textBundle.get("ExportAsRawDiskDataMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(null);
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsBinaryMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new BinaryFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsApplesoftBasicMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new ApplesoftFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsIntegerBasicMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new IntegerBasicFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsBusinessBASICMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new BusinessBASICFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsAsciiTextMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new TextFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsFormattedAssemblyMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new AssemblySourceFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsPascalTextMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new PascalTextFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("GutenbergRenderingMenuItem")); //$NON-NLS-1$
		Menu subMenu2 = new Menu(shell, SWT.DROP_DOWN);
		item.setMenu(subMenu2);
		item = new MenuItem(subMenu2, SWT.NONE);
		item.setText(textBundle.get("WordProcessorRenderAsTextMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGutenbergFilter().selectTextRendering();
				setFileFilter(getGutenbergFilter());
				exportFile(null);
			}
		});
		item = new MenuItem(subMenu2, SWT.NONE);
		item.setText(textBundle.get("WordProcessorRenderAsHtmlMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGutenbergFilter().selectHtmlRendering();
				setFileFilter(getGutenbergFilter());
				exportFile(null);
			}
		});
		item = new MenuItem(subMenu2, SWT.NONE);
		item.setText(textBundle.get("WordProcessorRenderAsRtfMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGutenbergFilter().selectRtfRendering();
				setFileFilter(getGutenbergFilter());
				exportFile(null);
			}
		});
		
		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsAppleWorksSpreadsheetFileMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new AppleWorksSpreadSheetFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsAppleWorksDatabaseFileMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(new AppleWorksDataBaseFileFilter());
				exportFile(null);
			}
		});

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsAppleWorksWordProcessorFileMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(getAwpFilter());
				exportFile(null);
			}
		});
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("WordProcessorRenderingMenuItem")); //$NON-NLS-1$
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
				subItems[0].setSelection(getAwpFilter().isTextRendering());
				subItems[1].setSelection(getAwpFilter().isHtmlRendering());
				subItems[2].setSelection(getAwpFilter().isRtfRendering());
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("WordProcessorRenderAsTextMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				getAwpFilter().selectTextRendering();
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("WordProcessorRenderAsHtmlMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				getAwpFilter().selectHtmlRendering();
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("WordProcessorRenderAsRtfMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate rendering style.
			 */
			public void widgetSelected(SelectionEvent event) {
				getAwpFilter().selectRtfRendering();
			}
		});
		
		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.NONE);
		item.setText(textBundle.get("ExportAsGraphicsMenuItem")); //$NON-NLS-1$
		item.setEnabled(GraphicsFileFilter.isCodecAvailable());
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setFileFilter(getGraphicsFilter());
				exportFile(null);
			}
		});
		
		// Add graphics mode
		item = new MenuItem(menu, SWT.CASCADE);
		item.setText(textBundle.get("ExportGraphicsModeMenuItem")); //$NON-NLS-1$
		item.setEnabled(GraphicsFileFilter.isCodecAvailable());
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
				subItems[0].setSelection(getGraphicsFilter().isHiresBlackAndWhiteMode());
				subItems[1].setSelection(getGraphicsFilter().isHiresColorMode());
				subItems[2].setSelection(getGraphicsFilter().isDoubleHiresBlackAndWhiteMode());
				subItems[3].setSelection(getGraphicsFilter().isDoubleHiresColorMode());
				subItems[4].setSelection(getGraphicsFilter().isSuperHires16Mode());
				subItems[5].setSelection(getGraphicsFilter().isSuperHires3200Mode());
				subItems[6].setSelection(getGraphicsFilter().isQuickDraw2Icon());
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("ExportGraphicsAsHiresBlackAndWhiteMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_HGR_BLACK_AND_WHITE);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("ExportGraphicsAsHiresColorMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_HGR_COLOR);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("ExportGraphicsAsDoubleHiresBlackAndWhiteMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_DHR_BLACK_AND_WHITE);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("ExportGraphicsAsDoubleHiresColorMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_DHR_COLOR);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("ExportGraphicsAsSuperHiresMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_SHR_16);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("ExportGraphicsAsSuperHires3200ColorMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_SHR_3200);
			}
		});
		item = new MenuItem(subMenu, SWT.RADIO);
		item.setText(textBundle.get("ExportGraphicsAsQuickDraw2IconMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			/**
			 * Set the appropriate graphics mode.
			 */
			public void widgetSelected(SelectionEvent event) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_QUICKDRAW2_ICON);
			}
		});
		
		// Add graphics formats, if any are defined.
		String[] formats = GraphicsFileFilter.getFileExtensions();
		if (formats != null && formats.length > 0) {
			item = new MenuItem(menu, SWT.CASCADE);
			item.setText(textBundle.get("ExportGraphicsFormatMenuItem")); //$NON-NLS-1$
			item.setEnabled(GraphicsFileFilter.isCodecAvailable());
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
							equals(getGraphicsFilter().getExtension()));
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
						getGraphicsFilter().setExtension(menuItem.getText());
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
		List<FileColumnHeader> headers = disks[0].getFileColumnHeaders(format);
		int[] headerWidths = new int[headers.size()];
		GC gc = new GC(shell);
		for (int i=0; i<headers.size(); i++) {
			FileColumnHeader header = (FileColumnHeader) headers.get(i);
			if (header.getTitle().length() >= header.getMaximumWidth()) {
				headerWidths[i] = gc.stringExtent(header.getTitle()).x + 
					2 * gc.stringExtent(textBundle.get("WidestCharacter")).x;  //$NON-NLS-1$
			} else {
				headerWidths[i] = gc.stringExtent(
						textBundle.get("WidestCharacter")).x  //$NON-NLS-1$
						* header.getMaximumWidth();
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
	protected void fillFileTable(List<FileEntry> fileList) {
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
					getImportToolItem().setEnabled(getDisk(0).canCreateFile() && getDisk(0).canWriteFileData());
					if (getFileTable().getSelectionCount() > 0) {
						FileEntry fileEntry = getSelectedFileEntry();
						getExportToolItem().setEnabled(getDisk(0).canReadFileData());
						getDeleteToolItem().setEnabled(getDisk(0).canDeleteFile());
						getCompileToolItem().setEnabled(fileEntry != null && fileEntry.canCompile());
						getViewFileToolItem().setEnabled(true);
					} else {
						getExportToolItem().setEnabled(false);
						getDeleteToolItem().setEnabled(false);
						getCompileToolItem().setEnabled(false);
						getViewFileToolItem().setEnabled(false);
					}
				}
				/**
				 * Double-click handler.
				 */
				public void widgetDefaultSelected(SelectionEvent event) {
					try {
						viewFile(null);
		            } catch (DiskException e) {
		            	DiskExplorerTab.this.diskWindow.handle(e);
		            }
				}
			});
			TableColumn column = null;
			List<FileColumnHeader> headers = disks[0].getFileColumnHeaders(currentFormat);
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

		Iterator<FileEntry> files = fileList.iterator();
		while (files.hasNext()) {
			FileEntry entry = (FileEntry) files.next();
			if (showDeletedFiles || !entry.isDeleted()) {
				TableItem item = new TableItem(fileTable, 0);
				List<String> data = entry.getFileColumnData(currentFormat);
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
	protected void exportFile(String directory) {
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
			} else if (fileFilter != null) {
				filename = directory + File.separator + AppleUtil.
					getNiceFilename(fileFilter.getSuggestedFileName(fileEntry));
			} else {
				filename = directory + File.separator + AppleUtil.
					getNiceFilename(fileEntry.getFilename());
			}
			if (filename != null) {
				userPreferences.setExportDirectory(directory);
				try {
					File file = new File(filename);
					if (file.exists()) {
						int answer = SwtUtil.showYesNoDialog(shell, 
								textBundle.get("FileExistsTitle"), //$NON-NLS-1$
								textBundle.format("FileExistsMessage", filename)); //$NON-NLS-1$
						if (answer == SWT.NO) {
							return; // do not overwrite file
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
				    ex.printStackTrace();
					String errorMessage = ex.getMessage();
					if (errorMessage == null) {
						errorMessage = ex.getClass().getName();
					}
					int answer = SwtUtil.showOkCancelErrorDialog(shell,
							textBundle.get("ExportErrorTitle"), //$NON-NLS-1$
							textBundle.format("ExportErrorMessage",  //$NON-NLS-1$
									new Object[] { filename, errorMessage }));
					if (answer == SWT.CANCEL) break;	// break out of loop
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
				fileDialog.setFileName(fileEntry.getFilename() + ".S"); //$NON-NLS-1$
				filename = fileDialog.open();
				directory = fileDialog.getFilterPath();
			} else {
				filename = directory + File.separator + AppleUtil.
					getNiceFilename(fileEntry.getFilename() + ".S"); //$NON-NLS-1$
			}
			if (filename != null) {
				userPreferences.setCompileDirectory(directory);
				try {
					File file = new File(filename);
					if (file.exists()) {
						int answer = SwtUtil.showYesNoDialog(shell,
								textBundle.get("FileExistsTitle"), //$NON-NLS-1$
								textBundle.format("FileExistsMessage", filename)); //$NON-NLS-1$
						if (answer == SWT.NO) {
							return;	// do not overwrite file
						}
					}
					ApplesoftCompiler compiler = new ApplesoftCompiler(fileEntry);
					byte[] assembly = compiler.compile();
					OutputStream outputStream = new FileOutputStream(file);
					outputStream.write(assembly);
					outputStream.close();
				} catch (Exception ex) {
					String errorMessage = ex.getMessage();
					if (errorMessage == null) {
						errorMessage = ex.getClass().getName();
					}
					int answer = SwtUtil.showOkCancelErrorDialog(shell,
							textBundle.get("UnableToCompileTitle"), //$NON-NLS-1$
							textBundle.format("UnableToCompileMessage", //$NON-NLS-1$
									new Object[] { filename, errorMessage }));
					if (answer == SWT.CANCEL) break;	// break out of loop
				}
			}
		}
	}
	/**
	 * Delete the currently selected files.
	 */
	protected void deleteFile() {
		TableItem[] selection = fileTable.getSelection();

		String message = (selection.length > 1) ?
				textBundle.get("DeletePromptMultipleFiles") : //$NON-NLS-1$
				textBundle.get("DeletePromptSingleFile") //$NON-NLS-1$
				+ textBundle.get("DeletePromptTrailer"); //$NON-NLS-1$
		int answer = SwtUtil.showYesNoDialog(shell,
				textBundle.get("DeletePromptTitle"), //$NON-NLS-1$
				message);
		if (answer == SWT.YES) {
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
	 * @throws DiskException
	 */
	protected void importFiles() throws DiskException {
		//FIXME: This code has become really ugly!
		TreeItem treeItem = directoryTree.getSelection()[0];
		DirectoryEntry directory = (DirectoryEntry) treeItem.getData();
		ImportWizard wizard = new ImportWizard(shell,
			imageManager, directory.getFormattedDisk());
		wizard.open();
		if (wizard.isWizardCompleted()) {
			Shell dialog = null;
			try {
				List<ImportSpecification> specs = wizard.getImportSpecifications();
				// Progress meter for import wizard:
				dialog = new Shell(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
				dialog.setText(textBundle.get("ImportingFilesTitle")); //$NON-NLS-1$
				GridLayout layout = new GridLayout();
				layout.horizontalSpacing = 5;
				layout.makeColumnsEqualWidth = false;
				layout.marginHeight = 5;
				layout.marginWidth = 5;
				layout.numColumns = 2;
				layout.verticalSpacing = 5;
				dialog.setLayout(layout);
				Label label = new Label(dialog, SWT.NONE);
				label.setText(textBundle.get("ImportingFilesProcessingLabel")); //$NON-NLS-1$
				Label countLabel = new Label(dialog, SWT.NONE);
				GridData gridData = new GridData();
				gridData.widthHint = 300;
				countLabel.setLayoutData(gridData);
				label = new Label(dialog, SWT.NONE);
				label.setText(textBundle.get("ImportingFilesFilenameLabel")); //$NON-NLS-1$
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
					countLabel.setText(textBundle.format("FileNofM", //$NON-NLS-1$
							new Object[] { new Integer(i+1), new Integer(specs.size()) }));
					nameLabel.setText(spec.getSourceFilename());
					progressBar.setSelection(i);
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					InputStream input =  new FileInputStream(spec.getSourceFilename());
					StreamUtil.copy(input, buffer);
					byte[] fileData = buffer.toByteArray();
					FileEntry fileEntry = directory.createFile();
					fileEntry.setFilename(spec.getTargetFilename());
					fileEntry.setFiletype(spec.getFiletype());
					if (spec.isRawFileImport()) {
						disks[0].setFileData(fileEntry, fileData);
					} else {
						if (AppleSingle.test(fileData)) {
							AppleSingle as = AppleSingle.read(fileData);
							fileData = as.getDataFork();
						}
						if (fileEntry.needsAddress()) {
							fileEntry.setAddress(spec.getAddress());
						}
						try {
							fileEntry.setFileData(fileData);
						} catch (ProdosDiskSizeDoesNotMatchException ex) {
							int answer = SwtUtil.showYesNoDialog(shell,
									textBundle.get("ResizeDiskTitle"), //$NON-NLS-1$
									textBundle.get("ResizeDiskMessage")); //$NON-NLS-1$
							if (answer == SWT.YES) {
								ProdosFormatDisk prodosDisk = (ProdosFormatDisk) 
									fileEntry.getFormattedDisk();
								prodosDisk.resizeDiskImage();
								fileEntry.setFileData(fileData);
							}
						}
					}
				}
			} catch (Exception ex) {
				SwtUtil.showErrorDialog(shell,
						textBundle.get("ImportErrorTitle"), //$NON-NLS-1$
						textBundle.format("ImportErrorMessage", ex.getMessage())); //$NON-NLS-1$
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
	 * @throws DiskException
	 */
	protected void addDirectoriesToTree(TreeItem directoryItem, DirectoryEntry directoryEntry) throws DiskException {
		Iterator<FileEntry> files = directoryEntry.getFiles().iterator();
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
		standardFormatToolItem.setText(textBundle.get("StandardViewToolItem")); //$NON-NLS-1$
		standardFormatToolItem.setToolTipText(textBundle.get("StandardViewHoverText")); //$NON-NLS-1$
		standardFormatToolItem.setSelection(true);
		standardFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				try {
					changeCurrentFormat(FormattedDisk.FILE_DISPLAY_STANDARD);
	            } catch (DiskException e) {
	            	DiskExplorerTab.this.diskWindow.handle(e);
	            }
			}
		});
		nativeFormatToolItem = new ToolItem(toolBar, SWT.RADIO);
		nativeFormatToolItem.setImage(imageManager.get(ImageManager.ICON_NATIVE_FILE_VIEW));
		nativeFormatToolItem.setText(textBundle.get("NativeViewToolItem")); //$NON-NLS-1$
		nativeFormatToolItem.setToolTipText(textBundle.get("NativeViewHoverText")); //$NON-NLS-1$
		nativeFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				try {
					changeCurrentFormat(FormattedDisk.FILE_DISPLAY_NATIVE);
	            } catch (DiskException e) {
	            	DiskExplorerTab.this.diskWindow.handle(e);
	            }
			}
		});
		detailFormatToolItem = new ToolItem(toolBar, SWT.RADIO);
		detailFormatToolItem.setImage(imageManager.get(ImageManager.ICON_DETAIL_FILE_VIEW));
		detailFormatToolItem.setText(textBundle.get("DetailViewToolItem")); //$NON-NLS-1$
		detailFormatToolItem.setToolTipText(textBundle.get("DetailViewHoverText")); //$NON-NLS-1$
		detailFormatToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				try {
					changeCurrentFormat(FormattedDisk.FILE_DISPLAY_DETAIL);
	            } catch (DiskException e) {
	            	DiskExplorerTab.this.diskWindow.handle(e);
	            }
			}
		});

		new ToolItem(toolBar, SWT.SEPARATOR);

		showDeletedFilesToolItem = new ToolItem(toolBar, SWT.CHECK);
		showDeletedFilesToolItem.setImage(imageManager.get(ImageManager.ICON_SHOW_DELETED_FILES));
		showDeletedFilesToolItem.setText(textBundle.get("ShowDeletedFilesToolItem")); //$NON-NLS-1$
		showDeletedFilesToolItem.setToolTipText(textBundle.get("ShowDeletedFilesHoverText")); //$NON-NLS-1$
		showDeletedFilesToolItem.setEnabled(disks[0].supportsDeletedFiles());
		showDeletedFilesToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				ToolItem toolItem = (ToolItem) e.getSource();	// show deleted files
				setShowDeletedFiles(toolItem.getSelection());
				fillFileTable(getCurrentFileList());
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		importToolItem = new ToolItem(toolBar, SWT.PUSH);
		importToolItem.setImage(imageManager.get(ImageManager.ICON_IMPORT_FILE));
		importToolItem.setText(textBundle.get("ImportWizardToolItem")); //$NON-NLS-1$
		importToolItem.setToolTipText(textBundle.get("ImportWizardHoverText")); //$NON-NLS-1$
		importToolItem.setEnabled(disks[0].canCreateFile() && disks[0].canWriteFileData());
		importToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				try {
					importFiles();
	            } catch (DiskException e) {
	            	DiskExplorerTab.this.diskWindow.handle(e);
	            }
			}
		});

		exportToolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
		exportToolItem.setImage(imageManager.get(ImageManager.ICON_EXPORT_FILE));
		exportToolItem.setText(textBundle.get("ExportWizardToolItem")); //$NON-NLS-1$
		exportToolItem.setToolTipText(textBundle.get("ExportWizardHoverText")); //$NON-NLS-1$
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
		compileToolItem.setText(textBundle.get("CompileWizardToolItem")); //$NON-NLS-1$
		compileToolItem.setToolTipText(textBundle.get("CompileWizardHoverText")); //$NON-NLS-1$
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
		viewFileItem.setText(textBundle.get("ViewFileToolItem")); //$NON-NLS-1$
		viewFileItem.setToolTipText(textBundle.get("ViewFileHoverText")); //$NON-NLS-1$
		viewFileItem.setEnabled(false);
		viewFileItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent event) {
				if (event.detail != SWT.ARROW) { 
					try {
						viewFile(null);
		            } catch (DiskException e) {
		            	DiskExplorerTab.this.diskWindow.handle(e);
		            }
				}
			}
		});
		printToolItem = new ToolItem(toolBar, SWT.PUSH);
		printToolItem.setImage(imageManager.get(ImageManager.ICON_PRINT_FILE));
		printToolItem.setText(textBundle.get("PrintButton")); //$NON-NLS-1$
		printToolItem.setToolTipText(textBundle.get("PrintDirectoryHoverText")); //$NON-NLS-1$
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
		deleteToolItem.setText(textBundle.get("DeleteFileToolItem")); //$NON-NLS-1$
		deleteToolItem.setToolTipText(textBundle.get("DeleteFileHoverText")); //$NON-NLS-1$
		deleteToolItem.setEnabled(false);
		deleteToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				deleteFile();
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		changeOrderToolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
		changeOrderToolItem.setImage(imageManager.get(ImageManager.ICON_CHANGE_IMAGE_ORDER));
		changeOrderToolItem.setText(textBundle.get("ChangeDiskOrderToolItem")); //$NON-NLS-1$
		changeOrderToolItem.setToolTipText(textBundle.get("ChangeDiskOrderHoverText")); //$NON-NLS-1$
		ImageOrder imageOrder = disks[0].getImageOrder();
		changeOrderToolItem.setEnabled(
			(imageOrder.isBlockDevice() 
				&& imageOrder.getBlocksOnDevice() == Disk.PRODOS_BLOCKS_ON_140KB_DISK)
			|| (imageOrder.isTrackAndSectorDevice() 
				&& imageOrder.getSectorsPerDisk() == Disk.DOS33_SECTORS_ON_140KB_DISK));
		changeOrderToolItem.addSelectionListener(
			new DropDownSelectionListener(getChangeImageOrderMenu()));
		changeOrderToolItem.addSelectionListener(new SelectionAdapter () {
			/** 
			 * Whenever the button is clicked, force the menu to be shown
			 */
			public void widgetSelected(SelectionEvent event) {
				ToolItem toolItem = (ToolItem) event.getSource(); // change order tool item
				Rectangle rect = toolItem.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = getToolBar().toDisplay(pt);
				getChangeImageOrderMenu().setLocation(pt.x, pt.y);
				getChangeImageOrderMenu().setVisible(true);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);

		saveToolItem = new ToolItem(toolBar, SWT.PUSH);
		saveToolItem.setImage(imageManager.get(ImageManager.ICON_SAVE_DISK_IMAGE));
		saveToolItem.setText(textBundle.get("SaveDiskImageToolItem")); //$NON-NLS-1$
		saveToolItem.setToolTipText(textBundle.get("SaveDiskImageHoverText")); //$NON-NLS-1$
		saveToolItem.setEnabled(disks[0].hasChanged());	// same physical disk
		saveToolItem.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				save();
			}
		});

		saveAsToolItem = new ToolItem(toolBar, SWT.PUSH);
		saveAsToolItem.setImage(imageManager.get(ImageManager.ICON_SAVE_DISK_IMAGE_AS));
		saveAsToolItem.setText(textBundle.get("SaveDiskImageAsToolItem")); //$NON-NLS-1$
		saveAsToolItem.setToolTipText(textBundle.get("SaveDiskImageAsHoverText")); //$NON-NLS-1$
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
	 * @throws DiskException
	 */
	protected void changeCurrentFormat(int newFormat) throws DiskException {
		TreeItem selection = directoryTree.getSelection()[0];
		Object data = selection.getData();
		DirectoryEntry directory = (DirectoryEntry) data;
		List<FileEntry> fileList = directory.getFiles();
		
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
		fileDialog.setFileName(Host.getFileName(disks[0].getFilename()));
		fileDialog.setText(textBundle.get("SaveDiskImageAsPrompt")); //$NON-NLS-1$
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
		box.setText(textBundle.get("SaveDiskImageErrorTitle")); //$NON-NLS-1$
		box.setMessage(textBundle.format("SaveDiskImageErrorMessage", //$NON-NLS-1$
				new Object[] { getDisk(0).getFilename(), errorMessage }));
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
								treeItem = getDirectoryTree().getSelection();
								setDirectoryExpandedStates(treeItem[0], false);
								break;
							case '+':
								treeItem = getDirectoryTree().getSelection();
								setDirectoryExpandedStates(treeItem[0], true);
								break;
						}
					} else {	// assume no control and no alt
						switch (event.character) {
							case '-':
								treeItem = getDirectoryTree().getSelection();
								treeItem[0].setExpanded(false);
								break;
							case '+':
								treeItem = getDirectoryTree().getSelection();
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
	 * @throws DiskException
	 */
	protected void viewFile(Class<? extends FileFilter> fileFilterClass) throws DiskException {
		FileEntry fileEntry = getSelectedFileEntry();
		if (fileEntry.isDeleted()) {
			SwtUtil.showErrorDialog(shell, textBundle.get("DeleteFileErrorTitle"), //$NON-NLS-1$
				textBundle.get("DeleteFileErrorMessage")); //$NON-NLS-1$
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
				fileFilter = fileFilterClass.newInstance();
			} catch (NullPointerException ex) {
				// This is expected
			} catch (InstantiationException e) {
				SwtUtil.showSystemErrorDialog(shell, e);
			} catch (IllegalAccessException e) {
				SwtUtil.showSystemErrorDialog(shell, e);
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
					try { 
						switch (event.character) {
							case CTRL_C:	// Compile Wizard
								if (getCompileToolItem().isEnabled()) {
									compileFileWizard();
								}
								break;
							case CTRL_D:	// Delete file
								if (getDeleteToolItem().isEnabled()) {
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
					} catch (DiskException e) {
						DiskExplorerTab.this.diskWindow.handle(e);
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
							try {
								switch (event.character) {
									case CTRL_I:	// Import Wizard
										importFiles();
										break;
									case CTRL_P:	// Print...
										print();
										break;
									case CTRL_S:	// Save
										if (getSaveToolItem().isEnabled()) {
											save();
										}
										break;
								}
				            } catch (DiskException e) {
				            	DiskExplorerTab.this.diskWindow.handle(e);
				            }
						}
					} else {	// No CTRL key
					    if ((event.stateMask & SWT.ALT) != SWT.ALT)	// Ignore ALT key combinations like alt-F4!
						try {
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
									setShowDeletedFiles(!getShowDeletedFilesToolItem().getSelection());
									getShowDeletedFilesToolItem().setSelection(isShowDeletedFiles());
									fillFileTable(getCurrentFileList());
									break;
							}
			            } catch (DiskException e) {
			            	DiskExplorerTab.this.diskWindow.handle(e);
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
		private List<FileColumnHeader> fileHeaders;
		private int[] printColumnWidths;
		private int[] printColumnPosition;
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
			if (printer.startJob(getDisk(0).getFilename())) {
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
				if (getCurrentFormat() == FormattedDisk.FILE_DISPLAY_NATIVE) {
					fontSize = 10;
				} else if (getCurrentFormat() == FormattedDisk.FILE_DISPLAY_DETAIL) {
					fontSize = 8;
				}
				normalFont = new Font(printer, new String(), fontSize, SWT.NORMAL);
				headerFont = new Font(printer, new String(), fontSize, SWT.BOLD);
				for (int i=0; i<getDisks().length; i++) {
					FormattedDisk disk = getDisk(i);
					filename = disk.getFilename();
					fileHeaders =  disk.getFileColumnHeaders(getCurrentFormat());
					gc.setFont(headerFont);
					computeHeaderWidths();
					printFileHeaders();
					gc.setFont(normalFont);
					println(disk.getDiskName());
					try {
						printFiles(disk, 1);
		            } catch (DiskException e) {
		            	DiskExplorerTab.this.diskWindow.handle(e);
		            }
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
			printColumnWidths = new int[fileHeaders.size()];
			printColumnPosition = new int[fileHeaders.size()];
			int position = clientArea.x;
			for (int i=0; i<fileHeaders.size(); i++) {
				printColumnWidths[i] = (widths[i] * clientArea.width) / totalWidth;
				printColumnPosition[i] = position; 
				position+= printColumnWidths[i];
			}
		}
		protected void printFileHeaders() {
			for (int i=0; i<fileHeaders.size(); i++) {
				FileColumnHeader header = (FileColumnHeader) fileHeaders.get(i);
				print(i, header.getTitle(), header.getAlignment());
			}
			println(new String());
		}
		protected void print(int column, String text, int alignment) {
			int x0 = printColumnPosition[column];
			int x1 = (column+1 < printColumnPosition.length) ?
					printColumnPosition[column+1] : clientArea.width;
			int w = printColumnWidths[column];
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
			TextBundle textBundle = UiBundle.getInstance();
			String text = textBundle.format("PageNumberText", Integer.toString(page)); //$NON-NLS-1$
			Point point = gc.stringExtent(text);
			gc.drawString(text, 
				clientArea.x + (clientArea.width - point.x)/2, 
				clientArea.y + clientArea.height + dpiY - point.y);
			page++;
		}
		protected void printFiles(DirectoryEntry directory, int level) throws DiskException {
			Iterator<FileEntry> iterator = directory.getFiles().iterator();
			while (iterator.hasNext()) {
				FileEntry fileEntry = (FileEntry) iterator.next();
				if (!fileEntry.isDeleted() || isShowDeletedFiles()) {
					List<String> columns = fileEntry.getFileColumnData(getCurrentFormat());
					for (int i=0; i<columns.size(); i++) {
						FileColumnHeader header = (FileColumnHeader) fileHeaders.get(i);
						String text = (String)columns.get(i);
						if ("name".equalsIgnoreCase(header.getTitle())) { //$NON-NLS-1$
							for (int l=0; l<level; l++) {
								text = "  " + text; //$NON-NLS-1$
							}
						}
						print(i, text, header.getAlignment());
					}
					println(new String());
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
				int chop = filename.lastIndexOf(".", filename.length()-4); //$NON-NLS-1$
				filename = filename.substring(0, chop+1) + extension + ".gz"; //$NON-NLS-1$
			} else {
				int chop = filename.lastIndexOf("."); //$NON-NLS-1$
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
			box.setText(textBundle.get("ChangeImageOrderErrorTitle")); //$NON-NLS-1$
			box.setMessage(textBundle.format(
				"ChangeImageOrderErrorMessage", errorMessage)); //$NON-NLS-1$
			box.open();
		}
	}

	/**
	 * Construct the popup menu for the export button on the toolbar.
	 */
	protected Menu createChangeImageOrderMenu() {
		Menu menu = new Menu(shell, SWT.NONE);
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent event) {
				Menu theMenu = (Menu) event.getSource();
				MenuItem[] subItems = theMenu.getItems();
				// Nibble Order (*.nib)
				subItems[0].setSelection(getDisk(0).isNibbleOrder());
				// DOS Order (*.dsk)
				subItems[1].setSelection(getDisk(0).isDosOrder());
				// ProDOS Order (*.po)
				subItems[2].setSelection(getDisk(0).isProdosOrder());
			}
		});
			
		MenuItem item = new MenuItem(menu, SWT.RADIO);
		item.setText(textBundle.get("ChangeToNibbleOrderMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (!getDisk(0).isNibbleOrder()) {
					NibbleOrder nibbleOrder = new NibbleOrder(
						new ByteArrayImageLayout(Disk.APPLE_140KB_NIBBLE_DISK));
					nibbleOrder.format();
					changeImageOrder("nib", nibbleOrder); //$NON-NLS-1$
				}
			}
		});

		item = new MenuItem(menu, SWT.RADIO);
		item.setText(textBundle.get("ChangeToDosOrderMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (!getDisk(0).isDosOrder()) {
					changeImageOrder("dsk", new DosOrder( //$NON-NLS-1$
						new ByteArrayImageLayout(Disk.APPLE_140KB_DISK)));
				}
			}
		});

		item = new MenuItem(menu, SWT.RADIO);
		item.setText(textBundle.get("ChangeToProdosOrderMenuItem")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (!getDisk(0).isProdosOrder()) {
					changeImageOrder("po", new ProdosOrder( //$NON-NLS-1$
						new ByteArrayImageLayout(Disk.APPLE_140KB_DISK)));
				}
			}
		});
		
		return menu;
	}
	
	protected void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}
	
	protected void setShowDeletedFiles(boolean showDeletedFiles) {
		this.showDeletedFiles = showDeletedFiles;
	}
	
	protected boolean isShowDeletedFiles() {
		return showDeletedFiles;
	}
	
	protected Menu getChangeImageOrderMenu() {
		if (changeImageOrderMenu == null) {
			changeImageOrderMenu = createChangeImageOrderMenu();
		}
		return changeImageOrderMenu;
	}
	
	protected ToolBar getToolBar() {
		return toolBar;
	}
	
	protected FormattedDisk[] getDisks() {
		return disks;
	}
	
	protected FormattedDisk getDisk(int diskNumber) {
		return disks[diskNumber];
	}
	
	protected int getCurrentFormat() {
		return currentFormat;
	}
	
	protected Tree getDirectoryTree() {
		return directoryTree;
	}
	
	protected AppleWorksWordProcessorFileFilter getAwpFilter() {
		return awpFilter;
	}
	
	protected GutenbergFileFilter getGutenbergFilter() {
		return gutenbergFilter;
	}
	
	protected GraphicsFileFilter getGraphicsFilter() {
		return graphicsFilter;
	}
	
	protected ToolItem getImportToolItem() {
		return importToolItem;
	}
	
	protected ToolItem getExportToolItem() {
		return exportToolItem;
	}
	
	protected ToolItem getCompileToolItem() {
		return compileToolItem;
	}
	
	protected Table getFileTable() {
		return fileTable;
	}
	
	protected ToolItem getDeleteToolItem() {
		return deleteToolItem;
	}
	
	protected ToolItem getViewFileToolItem() {
		return viewFileItem;
	}
	
	protected List<FileEntry> getCurrentFileList() {
		return currentFileList;
	}
	
	protected ToolItem getSaveToolItem() {
		return saveToolItem;
	}
	
	protected ToolItem getShowDeletedFilesToolItem() {
		return showDeletedFilesToolItem;
	}	
}
