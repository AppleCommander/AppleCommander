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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.ui.swt.WizardPane;
import com.webcodepro.applecommander.util.AppleUtil;

/**
 * Shows the result of the disk image comparison.
 * <p>
 * @author: Rob Greene
 */
public class CompareDisksResultsPane extends WizardPane {
	private Composite parent;
	private Object layoutData;
	private Composite control;
	private CompareDisksWizard wizard;
	private Text diskname1Text;
	private Text diskname2Text;
	/**
	 * Constructor for ExportFileStartPane.
	 */
	public CompareDisksResultsPane(Composite parent, CompareDisksWizard wizard, Object layoutData) {
		super();
		this.parent = parent;
		this.wizard = wizard;
		this.layoutData = layoutData;
	}
	/**
	 * Open up and configure the wizard pane.
	 */
	public void open() {
		control = new Composite(parent, SWT.NULL);
		control.setLayoutData(layoutData);
		wizard.enableNextButton(false);
		wizard.enableFinishButton(true);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.justify = true;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		layout.spacing = 3;
		control.setLayout(layout);

		String message = compareDisks();
		
		Label label = new Label(control, SWT.WRAP);
		label.setText(message);

		label = new Label(control, SWT.WRAP);
		label.setText("If you wish to compare more disks, click previous and start again.");
	}
	/**
	 * Get the next pane. A null return indicates the end of the wizard.
	 * @see com.webcodepro.applecommander.gui.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return null;
	}
	/**
	 * Dispose of resources.
	 * @see com.webcodepro.applecommander.gui.WizardPane#dispose()
	 */
	public void dispose() {
		control.dispose();
		control = null;
	}
	
	protected String compareDisks() {
		StringBuffer errorMessages = new StringBuffer();		
		FormattedDisk[] disk1 = null;
		try {
			disk1 = new Disk(wizard.getDiskname1()).getFormattedDisks();
		} catch (Throwable t) {
			errorMessages.append("Unable to load disk #1: ");
			errorMessages.append(t.getMessage());
			errorMessages.append("\n");
		}
		FormattedDisk[] disk2 = null;
		try {
			disk2 = new Disk(wizard.getDiskname2()).getFormattedDisks();
		} catch (Throwable t) {
			errorMessages.append("Unable to load disk #2: ");
			errorMessages.append(t.getMessage());
			errorMessages.append("\n");
		}
		if (disk1 != null && disk2 != null) {
			if (disk1.length != disk2.length) {
				errorMessages.append("The two disks are of differing formats - unable to compare.\n");
			} else {
				boolean disk1TSformat = disk1[0].isCpmFormat() || disk1[0].isDosFormat() || disk1[0].isRdosFormat(); 
				boolean disk2TSformat = disk2[0].isCpmFormat() || disk2[0].isDosFormat() || disk2[0].isRdosFormat();
				if (disk1TSformat && disk2TSformat) {
					if (!AppleUtil.disksEqualByTrackAndSector(disk1[0], disk2[0])) {
						errorMessages.append("The two disks do not contain the same data.\n"); 
					}
				} else if (!disk1TSformat && !disk2TSformat) {
					if (!AppleUtil.disksEqualByBlock(disk1[0], disk2[0])) {
						errorMessages.append("The two disks do not contain the same data.\n");
					}
				} else {
					errorMessages.append("The two disks are not the same data format.\n");
				}
			}
		}
		if (errorMessages.length() == 0) {
			return "The disk images match.";
		} else {
			return errorMessages.toString();
		}
	}
}
