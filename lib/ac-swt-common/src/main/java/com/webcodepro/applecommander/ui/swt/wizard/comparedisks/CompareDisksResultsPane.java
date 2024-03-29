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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.compare.ComparisonResult;
import com.webcodepro.applecommander.storage.compare.DiskDiff;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Shows the result of the disk image comparison.
 * <p>
 * @author Rob Greene
 */
public class CompareDisksResultsPane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
	private Composite parent;
	private Object layoutData;
	private Composite control;
	private CompareDisksWizard wizard;
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
		label.setText(textBundle.get("CompareDisksResultsPane.RestartText")); //$NON-NLS-1$
		
		parent.pack();
	}
	/**
	 * Get the next pane. A null return indicates the end of the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return null;
	}
	/**
	 * Dispose of resources.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#dispose()
	 */
	public void dispose() {
		control.dispose();
		control = null;
	}
	
	protected String compareDisks() {
		List<String> errorMessages = new ArrayList<>();	
		Disk disk1 = null;
		try {
			disk1 = new Disk(wizard.getDiskname1());
		} catch (Throwable t) {
			errorMessages.add(textBundle.
				format("CompareDisksResultsPane.UnableToLoadDiskN", //$NON-NLS-1$
					1, t.getLocalizedMessage()));
		}
		Disk disk2 = null;
		try {
			disk2 = new Disk(wizard.getDiskname2());
		} catch (Throwable t) {
			errorMessages.add(textBundle.
				format("CompareDisksResultsPane.UnableToLoadDiskN", //$NON-NLS-1$
					2, t.getLocalizedMessage()));
		}
		if (disk1 != null && disk2 != null) {
		    DiskDiff.Builder builder = DiskDiff.create(disk1, disk2);
		    switch (wizard.getComparisonStrategy()) {
		    case 0:
		        builder.selectCompareByNativeGeometry();
		        break;
		    case 1:
		        builder.selectCompareByTrackSectorGeometry();
		        break;
		    case 2:
		        builder.selectCompareByBlockGeometry();
		        break;
		    case 3:
		        builder.selectCompareByFileName();
		        break;
	        default:
	            throw new RuntimeException("missing a comparison strategy");
		    }
		    ComparisonResult result = builder.compare();
		    errorMessages.addAll(result.getLimitedMessages(wizard.getMessageLimit()));
		}
		if (errorMessages.size() == 0) {
			return textBundle.get("CompareDisksResultsPane.DisksMatch"); //$NON-NLS-1$
		}
		return String.join("\n", errorMessages);
	}
}
