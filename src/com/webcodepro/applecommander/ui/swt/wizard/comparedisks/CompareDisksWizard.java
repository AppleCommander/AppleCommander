/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2004 by Robert Greene
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
package com.webcodepro.applecommander.ui.swt.wizard.comparedisks;

import org.eclipse.swt.widgets.Shell;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.wizard.Wizard;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;

/**
 * Compare disks wizard.
 * <p>
 * @author Rob Greene
 */
public class CompareDisksWizard extends Wizard {
	private String diskname1;
	private String diskname2;
	/**
	 * Constructor for ExportWizard.
	 */
	public CompareDisksWizard(Shell parent, ImageManager imageManager) {
		super(parent, imageManager.get(ImageManager.LOGO_COMPARE_IMAGE_WIZARD), 
				UiBundle.getInstance().get("CompareDisksTitle")); //$NON-NLS-1$
	}
	/**
	 * Create the initial display used in the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.Wizard#createInitialWizardPane()
	 */
	public WizardPane createInitialWizardPane() {
		return new CompareDisksStartPane(getContentPane(), this, null);
	}
	public String getDiskname1() {
		return diskname1;
	}
	public String getDiskname2() {
		return diskname2;
	}
	public void setDiskname1(String string) {
		diskname1 = string;
	}
	public void setDiskname2(String string) {
		diskname2 = string;
	}

}
