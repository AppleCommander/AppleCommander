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
package com.webcodepro.applecommander.ui.swt.wizard.compilefile;

import org.eclipse.swt.widgets.Shell;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.wizard.Wizard;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;

/**
 * Compile wizard.
 * <p>
 * Date created: Nov 7, 2002 9:22:35 PM
 * @author Rob Greene
 */
public class CompileWizard extends Wizard {
	private FormattedDisk disk;
	private String directory;
	/**
	 * Constructor for ExportWizard.
	 */
	public CompileWizard(Shell parent, ImageManager imageManager, FormattedDisk disk) {
		super(parent, imageManager.get(ImageManager.LOGO_COMPILE_WIZARD), 
				UiBundle.getInstance().get("CompileWizardTitle")); //$NON-NLS-1$
		this.disk = disk;
	}
	/**
	 * Get the disk that is being worked on.
	 */
	public FormattedDisk getDisk() {
		return disk;
	}
	/**
	 * Create the initial display used in the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.Wizard#createInitialWizardPane()
	 */
	public WizardPane createInitialWizardPane() {
		return new CompileFileStartPane(getContentPane(), this, null);
	}

	/**
	 * @return the directory
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * @param string the directory
	 */
	public void setDirectory(String string) {
		directory = string;
	}

}
