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
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;

import com.webcodepro.applecommander.storage.FormattedDisk;

/**
 * Displays disk information on the screen.
 * <p>
 * Date created: Oct 12, 2002 3:28:41 PM
 * @author: Rob Greene
 */
public class DiskWindow {
	private Shell parentShell;
	private ImageManager imageManager;
	
	private Shell shell;
	private FormattedDisk disk;
	
	private DiskInfoTab diskInfoTab;
	private DiskMapTab diskMapTab;
	private DiskExplorerTab diskExplorerTab;

	/**
	 * Construct the disk window.
	 */
	public DiskWindow(Shell parentShell, FormattedDisk disk, ImageManager imageManager) {
		this.parentShell = shell;
		this.disk = disk;
		this.imageManager = imageManager;
	}
	
	/**
	 * Setup the Disk window and display (open) it.
	 */
	public void open() {
		shell = new Shell(parentShell, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());
		shell.setImage(imageManager.getDiskIcon());
		shell.setText("AppleCommander - " + disk.getFilename());
		shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					dispose(event);
				}
			});
			
		CTabFolder tabFolder = new CTabFolder(shell, SWT.BOTTOM);
		diskExplorerTab = new DiskExplorerTab(tabFolder, disk, imageManager);
		diskMapTab = new DiskMapTab(tabFolder, disk);
		diskInfoTab = new DiskInfoTab(tabFolder, disk);
		tabFolder.setSelection(tabFolder.getItems()[0]);
		
		shell.open();
	}
	
	/**
	 * Dispose of all shared resources.
	 */
	private void dispose(DisposeEvent event) {
		diskMapTab.dispose();
		diskInfoTab.dispose();

		disk = null;
		diskMapTab = null;
		diskInfoTab = null;
		System.gc();
	}
	
}
