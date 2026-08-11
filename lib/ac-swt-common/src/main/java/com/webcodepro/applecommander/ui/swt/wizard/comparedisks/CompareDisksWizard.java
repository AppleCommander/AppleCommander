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
package com.webcodepro.applecommander.ui.swt.wizard.comparedisks;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.wizard.Wizard;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import org.eclipse.swt.widgets.Shell;

import java.util.Map;

/**
 * Compare disks wizard.
 * <p>
 * @author Rob Greene
 */
public class CompareDisksWizard extends Wizard<CompareDisksWizard.Pages> {
	private String diskname1;
	private String diskname2;
	private int comparisonStrategy = 0;
	private int messageLimit = 10;
	/**
	 * Constructor for ExportWizard.
	 */
	public CompareDisksWizard(Shell parent, ImageManager imageManager) {
		super(parent, imageManager.get(ImageManager.LOGO_COMPARE_IMAGE_WIZARD), 
				UiBundle.getInstance().get("CompareDisksTitle"));
	}
	/**
	 * Create the panes used in the wizard.
	 */
	public Map<Pages,WizardPane<Pages>> createWizardPanes() {
		return Map.of(
			Pages.START_PANE, new CompareDisksStartPane(getContentPane(), this),
			Pages.RESULT_PANE, new CompareDisksResultsPane(getContentPane(), this)
		);
	}
	/**
	 * Indicates the first pane of the wizard.
	 */
	public Pages getFirstWizardPane() {
		return Pages.START_PANE;
	}

	public String getDiskname1() {
		return diskname1;
	}
	public String getDiskname2() {
		return diskname2;
	}
	public int getComparisonStrategy() {
        return comparisonStrategy;
    }
	public int getMessageLimit() {
        return messageLimit;
    }
	public void setDiskname1(String string) {
		diskname1 = string;
	}
	public void setDiskname2(String string) {
		diskname2 = string;
	}
	public void setComparisonStrategy(int comparisonStrategy) {
        this.comparisonStrategy = comparisonStrategy;
    }
	public void setMessageLimit(int messageLimit) {
        this.messageLimit = messageLimit;
    }

	public enum Pages {
		START_PANE,
		RESULT_PANE
	}
}
