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

import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.FormattedDisk;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

/**
 * File export wizard.
 * <p>
 * Date created: Nov 7, 2002 9:22:35 PM
 * @author: Rob Greene
 */
public class ExportWizard extends Wizard {
	private FormattedDisk disk;
	private FileFilter fileFilter;
	private String directory;
	/**
	 * Constructor for ExportWizard.
	 */
	public ExportWizard(Shell parent, ImageManager imageManager, FormattedDisk disk) {
		super(parent, imageManager.getExportWizardLogo(), "Export Wizard");
		this.disk = disk;
	}
	/**
	 * Get the FileFilter.
	 */
	public FileFilter getFileFilter() {
		return fileFilter;
	}
	/**
	 * Set the FileFilter.
	 */
	public void setFileFilter(FileFilter fileFilter) {
		this.fileFilter = fileFilter;
	}
	/**
	 * Get the disk that is being worked on.
	 */
	public FormattedDisk getDisk() {
		return disk;
	}
	/**
	 * Returns the directory.
	 * @return String
	 */
	public String getDirectory() {
		return directory;
	}
	/**
	 * Sets the directory.
	 * @param directory The directory to set
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}
	/**
	 * Create the initial display used in the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.Wizard#createInitialWizardPane()
	 */
	public WizardPane createInitialWizardPane() {
		return new ExportFileStartPane(getContentPane(), this, null);
	}

}
