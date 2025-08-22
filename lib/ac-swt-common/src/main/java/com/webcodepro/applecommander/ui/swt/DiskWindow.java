/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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

import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.webcodepro.applecommander.storage.DiskCorruptException;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Displays disk information on the screen.
 * <p>
 * Date created: Oct 12, 2002 3:28:41 PM
 * @author Rob Greene
 */
public class DiskWindow {
	private Shell parentShell;
	private ImageManager imageManager;
	
	private Shell shell;
	private FormattedDisk[] disks;
	
	private DiskInfoTab diskInfoTab;
	private DiskMapTab[] diskMapTabs;

	private TextBundle textBundle = UiBundle.getInstance();

	/**
	 * Construct the disk window.
	 */
	public DiskWindow(Shell parentShell, FormattedDisk[] disks, ImageManager imageManager) {
		this.parentShell = shell;
		this.disks = disks;
		this.imageManager = imageManager;
	}
	
	/**
	 * Setup the Disk window and display (open) it.
	 */
	public void open() {
		shell = new Shell(parentShell, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());
		shell.setImage(imageManager.get(ImageManager.ICON_DISK));
		shell.setMinimumSize(400, 300);
		setStandardWindowTitle();
		shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					dispose(event);
				}
			});
		shell.addListener(SWT.Close, (event) -> {
			if (disks[0].hasChanged()) {
				int button = SwtUtil.showYesNoDialog(shell, "Are you sure?",
						"The disk has been modified. Are you certain you want to quit?");
				event.doit = button == SWT.YES;
			}
		});
			
		CTabFolder tabFolder = new CTabFolder(shell, SWT.BOTTOM);
		new DiskExplorerTab(tabFolder, disks, imageManager, this);
		diskMapTabs = new DiskMapTab[disks.length];
		for (int i=0; i<disks.length; i++) {
			if (disks[i].supportsDiskMap()) {
				diskMapTabs[i] = new DiskMapTab(tabFolder, disks[i]);
			}
		}
		diskInfoTab = new DiskInfoTab(tabFolder, disks);
		tabFolder.setSelection(tabFolder.getItems()[0]);
		
		shell.open();
	}
	
	/**
	 * Warns user about a Disk Corrupt problem
	 * 
	 * @param e
	 */
	protected void handle(final DiskCorruptException e) {
		Shell finalShell = shell;
		MessageBox box = new MessageBox(finalShell, SWT.ICON_ERROR | SWT.OK);
		box.setText(textBundle.get("SwtAppleCommander." + e.kind + ".Title")); //$NON-NLS-1$
		box.setMessage(
			  textBundle.format("SwtAppleCommander.DiskCorruptException.Message", //$NON-NLS-1$
					  e.imagepath
			  		));
		box.open();
	}
	
	/**
	 * Warns user about a Generic Disk Error problem
	 * 
	 * @param e
	 */
	protected void handle(final DiskException e) {
		if (e instanceof DiskCorruptException) {
			this.handle((DiskCorruptException) e);
			return;
		}
		final MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK | SWT.MODELESS);
		box.setText(textBundle.get("SwtAppleCommander.DiskException.Title")); //$NON-NLS-1$
		box.setMessage(
				  textBundle.format("SwtAppleCommander.DiskException.Message", //$NON-NLS-1$
						  e.imagepath, e.toString()));
		box.open();
	}
	
	/**
	 * Warns user about an Application Error problem
	 * 
	 * @param e
	 */
	public void handle(final Exception e) {
		final TextBundle textBundle = UiBundle.getInstance();
		Shell finalShell = shell;
		MessageBox box = new MessageBox(finalShell, SWT.ICON_ERROR | SWT.OK);
		box.setText(textBundle.get("SwtAppleCommander.UnexpectedErrorTitle")); //$NON-NLS-1$
		box.setMessage(
			  textBundle.get("SwtAppleCommander.UnexpectedErrorMessage" //$NON-NLS-1$
			  ));
		box.open();
		e.printStackTrace();
	}
	
	/**
	 * Set the standard AppleCommander disk window title.
	 * This is referenced in DiskWindow as well as DiskExplorerTab.
	 */
	public void setStandardWindowTitle() {
		shell.setText(UiBundle.getInstance().format(
				"DiskWindow.Title", disks[0].getFilename())); //$NON-NLS-1$
	}
	
	/**
	 * Dispose of all shared resources.
	 */
	protected void dispose(DisposeEvent event) {
		for (int i=0; i<diskMapTabs.length; i++) {
			if (diskMapTabs[i] != null) diskMapTabs[i].dispose();
		}
		diskInfoTab.dispose();

		disks = null;
		diskMapTabs = null;
		diskInfoTab = null;
		System.gc();
	}
	
}
