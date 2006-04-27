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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.Disk.FilenameFilter;
import com.webcodepro.applecommander.ui.AppleCommander;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.UserPreferences;
import com.webcodepro.applecommander.ui.swt.util.ImageCanvas;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.wizard.comparedisks.CompareDisksWizard;
import com.webcodepro.applecommander.ui.swt.wizard.diskimage.DiskImageWizard;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Main class for the SwtAppleCommander interface.
 * <p>
 * Date created: Oct 7, 2002 9:43:37 PM
 * @author Rob Greene
 */
public class SwtAppleCommander implements Listener {
	private Shell shell;
	private ToolBar toolBar;
	private UserPreferences userPreferences = UserPreferences.getInstance();
	private TextBundle textBundle = UiBundle.getInstance();
	private ImageCanvas imageCanvas;
	private static ImageManager imageManager;

	/**
	 * Launch SwtAppleCommander.
	 */
	public static void main(String[] args) {
		new SwtAppleCommander().launch();
	}

	/**
	 * Launch SwtAppleCommander.
	 */
	public void launch() {
		Display display = new Display();
		launch(display);
	}
	
	/**
	 * Launch SwtAppleCommander with a given display.
	 * Primary motivation is getting S-Leak to work!
	 */
	public void launch(Display display) {
		imageManager = new ImageManager(display);
		SwtAppleCommander application = new SwtAppleCommander();
		Shell shell = application.open(display);
		shell.forceActive();
		
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
	protected Shell open(Display display) {		
		Display.setAppName(textBundle.get("SwtAppleCommander.AppleCommander")); //$NON-NLS-1$
		shell = new Shell(display, SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.TITLE);
		shell.setText(textBundle.get("SwtAppleCommander.AppleCommander")); //$NON-NLS-1$
		shell.setImage(imageManager.get(ImageManager.ICON_DISK));
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
		Image logoImage = imageManager.get(ImageManager.LOGO_APPLECOMMANDER);
		gridData.widthHint = logoImage.getImageData().width;
		gridData.heightHint = logoImage.getImageData().height;
		imageCanvas = new ImageCanvas(shell, SWT.BORDER, logoImage, gridData);
		imageCanvas.addListener(SWT.KeyUp, this);
		imageCanvas.setFocus();
		
		shell.pack();
		shell.open();
		return shell;
	}

	/**
	 * Dispose of all shared resources.
	 */
	protected void dispose(DisposeEvent event) {
		imageCanvas.dispose();
		toolBar.dispose();
		imageManager.dispose();
	}
	
	/**
	 * Open a file.
	 */
	protected void openFile() {
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
					showUnrecognizedDiskFormatMessage(fullpath);
				}
			} catch (Exception ignored) {
				ignored.printStackTrace();
				showUnrecognizedDiskFormatMessage(fullpath);
			}
		}
	}

	/**
	 * Displays the unrecognized disk format message.
	 * @param fullpath
	 */
	protected void showUnrecognizedDiskFormatMessage(String fullpath) {
		Shell finalShell = shell;
		MessageBox box = new MessageBox(finalShell, SWT.ICON_ERROR | SWT.OK);
		box.setText(textBundle.get("SwtAppleCommander.UnrecognizedFormatTitle")); //$NON-NLS-1$
		box.setMessage(
			  textBundle.format("SwtAppleCommander.UnrecognizedFormatMessage", //$NON-NLS-1$
			  		fullpath));
		box.open();
	}

	/**
	 * Create a disk image.
	 */
	protected void createDiskImage() {
		DiskImageWizard wizard = new DiskImageWizard(shell, imageManager);
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
		item.setImage(imageManager.get(ImageManager.ICON_OPEN_DISK_IMAGE));
		item.setText(textBundle.get("OpenButton")); //$NON-NLS-1$
		item.setSelection(false);
		item.setToolTipText(textBundle.get("SwtAppleCommander.OpenDiskImageTooltip")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				openFile();
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		item = new ToolItem(toolBar, SWT.PUSH);
		item.setImage(imageManager.get(ImageManager.ICON_NEW_DISK_IMAGE));
		item.setText(textBundle.get("CreateButton")); //$NON-NLS-1$
		item.setToolTipText(textBundle.get("SwtAppleCommander.CreateDiskImageTooltip")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				createDiskImage();
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		item = new ToolItem(toolBar, SWT.PUSH);
		item.setImage(imageManager.get(ImageManager.ICON_COMPARE_DISKS));
		item.setText(textBundle.get("CompareButton")); //$NON-NLS-1$
		item.setToolTipText(textBundle.get("SwtAppleCommander.CompareDiskImageTooltip")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				compareDiskImages();
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		item = new ToolItem(toolBar, SWT.PUSH);
		item.setImage(imageManager.get(ImageManager.ICON_ABOUT_APPLECOMMANDER));
		item.setText(textBundle.get("AboutButton")); //$NON-NLS-1$
		item.setToolTipText(textBundle.get("SwtAppleCommander.AboutTooltip")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				showAboutAppleCommander();
			}
		});
		item = new ToolItem(toolBar, SWT.SEPARATOR);

		toolBar.pack();
	}
	
	public void showAboutAppleCommander() {
		final Shell finalShell = shell;
		MessageBox box = new MessageBox(finalShell, SWT.ICON_INFORMATION | SWT.OK);
		box.setText(textBundle.get("SwtAppleCommander.AboutTitle")); //$NON-NLS-1$
		box.setMessage( 
		  textBundle.format("SwtAppleCommander.AboutMessage", //$NON-NLS-1$
		  new Object[] { AppleCommander.VERSION, textBundle.get("Copyright") })); //$NON-NLS-1$
		box.open();
	}

	public void handleEvent(Event event) {
		if (event.type == SWT.KeyUp && (event.stateMask & SWT.CTRL) != 0) {
			switch (event.character) {
				case 0x01:		// CTRL+A
					showAboutAppleCommander();
					break;
				case 0x03:		// CTRL+C
					createDiskImage();
					break;
				case 0x0f:		// CTRL+O
					openFile();
					break;
				case 0x05:		// CTRL+E
					compareDiskImages();
					break;
			}
		}		
	}

	/**
	 * Start the compare disks wizard.  The result of the comparison is actually
	 * shown in the wizard, so this code is real simple.
	 */	
	public void compareDiskImages() {
		CompareDisksWizard wizard = new CompareDisksWizard(shell, imageManager);
		wizard.open();
	}
}
