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

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.Disk.FilenameFilter;
import com.webcodepro.applecommander.ui.AppleCommander;
import com.webcodepro.applecommander.ui.UserPreferences;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Main class for the SwtAppleCommander interface.
 * <p>
 * Date created: Oct 7, 2002 9:43:37 PM
 * @author: Rob Greene
 */
public class SwtAppleCommander {
	private Display display;
	private Shell shell;
	private ToolBar toolBar;
	private UserPreferences userPreferences = UserPreferences.getInstance();
	private static ImageManager imageManager;

	/**
	 * Launch SwtAppleCommander.
	 */
	public static void main(String[] args) {
		Display display = new Display();
		imageManager = new ImageManager(display);
		SwtAppleCommander application = new SwtAppleCommander();
		Shell shell = application.open(display);
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
		
		UserPreferences.getInstance().save();
	}

	/**
	 * Constructor for SwtAppleCommander.
	 */
	public SwtAppleCommander() {
		super();
	}
	
	/**
	 * Opens the main program.
	 */
	private Shell open(Display display) {		
		this.display = display;
		display.setAppName("AppleCommander");
		shell = new Shell(display, SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.TITLE);
		shell.setText("AppleCommander");
		shell.setImage(imageManager.getDiskIcon());
		shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					dispose(event);
				}
			});

		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 5;
		gridLayout.marginWidth = 5;
		shell.setLayout(gridLayout);

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		createToolBar(shell, gridData);
		
		gridData = new GridData();
		Image logoImage = imageManager.getLogoImage();
		gridData.widthHint = logoImage.getImageData().width;
		gridData.heightHint = logoImage.getImageData().height;
		ImageCanvas imageCanvas = new ImageCanvas(shell, SWT.BORDER, logoImage, gridData);
		
		shell.pack();
		shell.open();
		return shell;
	}

	/**
	 * Dispose of all shared resources.
	 */
	private void dispose(DisposeEvent event) {
		toolBar.dispose();
		imageManager.dispose();
	}

	/**
	 * Exits the main program.
	 */
	private void exit() {
		shell.close();
	}
	
	/**
	 * Open a file.
	 */
	private void openFile() {
		FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
		FilenameFilter[] fileFilters = Disk.getFilenameFilters();
		String[] names = new String[fileFilters.length];
		String[] extensions = new String[fileFilters.length];
		for (int i=0; i<fileFilters.length; i++) {
			names[i] = fileFilters[i].getNames();
			extensions[i] = fileFilters[i].getExtensions();
		}
		fileDialog.setFilterNames(names);
		fileDialog.setFilterExtensions(extensions);
		fileDialog.setFilterPath(userPreferences.getDiskImageDirectory());
		String fullpath = fileDialog.open();
		
		if (fullpath != null) {
			userPreferences.setDiskImageDirectory(fileDialog.getFilterPath());
			try {
				Disk disk = new Disk(fullpath);
				FormattedDisk[] formattedDisks = disk.getFormattedDisks();
				if (formattedDisks != null) {
					DiskWindow window = new DiskWindow(shell, formattedDisks, imageManager);
					window.open();
				} else {
					Shell finalShell = shell;
					MessageBox box = new MessageBox(finalShell, SWT.ICON_ERROR | SWT.OK);
					box.setText("Unrecognized Disk Format");
					box.setMessage(
						  "Unable to load '" + fullpath + "'.\n\n"
					    + "AppleCommander did not recognize the format\n"
						+ "of that disk.  Either this is a new format\n"
						+ "or a protected disk.\n\n"
						+ "Sorry!");
					box.open();
				}
			} catch (IOException ignored) {
			}
		}
	}

	/**
	 * Create a disk image.
	 */
	private void createDiskImage() {
		DiskImageWizard wizard = new DiskImageWizard(shell,
			imageManager.getDiskImageWizardLogo());
		wizard.open();
		if (wizard.isWizardCompleted()) {
			FormattedDisk[] disks = wizard.getFormattedDisks();
			DiskWindow window = new DiskWindow(shell, disks, imageManager);
			window.open();
		}
	}

	/**
	 * Creates the toolbar.
	 */
	private void createToolBar(Shell shell, Object layoutData) {
		toolBar = new ToolBar(shell, SWT.FLAT);
		if (layoutData != null) toolBar.setLayoutData(layoutData);

		ToolItem item = new ToolItem(toolBar, SWT.PUSH);
		item.setImage(imageManager.getOpenDiskIcon());
		item.setText("Open...");
		item.setToolTipText("Open a disk image");
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				openFile();
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		item = new ToolItem(toolBar, SWT.PUSH);
		item.setImage(imageManager.getNewDiskIcon());
		item.setText("Create...");
		item.setToolTipText("Create a disk image");
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				createDiskImage();
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		item = new ToolItem(toolBar, SWT.PUSH);
		item.setImage(imageManager.getAboutIcon());
		item.setText("About");
		item.setToolTipText("About AppleCommander");
		final Shell finalShell = shell;
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				MessageBox box = new MessageBox(finalShell, SWT.ICON_INFORMATION | SWT.OK);
				box.setText("About AppleCommander");
				box.setMessage(
					  "AppleCommander\n"
					+ "Version " + AppleCommander.VERSION + "\n"
					+ "Copyright (c) 2002\n\n"
				    + "AppleCommander was created for the express\n"
					+ "purpose of assisting those-who-remember.\n\n"
					+ "I wish you many hours of vintage pleasure!\n"
					+ "-Rob");
				box.open();
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		toolBar.pack();
	}
}
