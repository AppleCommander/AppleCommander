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

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.ui.ImportSpecification;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

/**
 * The Disk Import Wizard.
 * <br>
 * Created on Jan 16, 2003.
 * @author Rob Greene
 */
public class ImportWizard extends Wizard {
	private FormattedDisk disk;
	private List importSpecifications;
	/**
	 * Constructor for ImportWizard.
	 */
	public ImportWizard(Shell parent, ImageManager imageManager, FormattedDisk disk) {
		super(parent, imageManager.getImportWizardLogo(), "Import Wizard");
		this.disk = disk;
	}
	/**
	 * Create the initial display used in the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.Wizard#createInitialWizardPane()
	 */
	public WizardPane createInitialWizardPane() {
		return new ImportSelectFilesWizardPane(getContentPane(), this);
	}
	/**
	 * Add an import specification.
	 */
	public void addImportSpecification(ImportSpecification importSpecification) {
		getImportSpecifications().add(importSpecification);
	}
	/**
	 * Remove an import specification.
	 */
	public void removeImportSpecification(ImportSpecification importSpecification) {
		getImportSpecifications().remove(importSpecification);
	}
	/**
	 * Get the list of ImportSpecifications.
	 */
	public List getImportSpecifications() {
		if (importSpecifications == null) {
			importSpecifications = new ArrayList();
		}
		return importSpecifications;
	}
	/**
	 * Get the FormattedDisk the wizard is working with.
	 */
	public FormattedDisk getDisk() {
		return disk;
	}
}
