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

import com.webcodepro.applecommander.storage.DiskFactory;
import com.webcodepro.applecommander.storage.Disks;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.compare.ComparisonResult;
import com.webcodepro.applecommander.storage.compare.DiskDiff;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the result of the disk image comparison.
 * <p>
 * @author Rob Greene
 */
public class CompareDisksResultsPane extends WizardPane<CompareDisksWizard.Pages> {
	private static final TextBundle textBundle = UiBundle.getInstance();
	private final Composite parent;
    private final CompareDisksWizard wizard;
	private Text resultText;
	/**
	 * Constructor for ExportFileStartPane.
	 */
	public CompareDisksResultsPane(Composite parent, CompareDisksWizard wizard) {
		super();
		this.parent = parent;
		this.wizard = wizard;
	}
	/**
	 * Open up and configure the wizard pane.
	 */
	public Control create() {
        Composite control = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		control.setLayout(layout);

		Label label = new Label(control, SWT.WRAP);
		label.setText("Results:\n");

		resultText = new Text(control, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		resultText.setEditable(false);
		resultText.setLayoutData(new GridData(GridData.FILL_BOTH));

		label = new Label(control, SWT.WRAP);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label.setText(textBundle.get("CompareDisksResultsPane.RestartText"));
		
		parent.pack();
		return control;
	}
	/**
	 * Get the next pane. A null return indicates the end of the wizard.
	 */
	public CompareDisksWizard.Pages getNextPane() {
		return null;
	}
	/**
	 * Called when the page is activated.
	 */
	public void activate() {
		wizard.enableNextButton(false);
		wizard.enableFinishButton(true);

		String message = compareDisks();
		resultText.setText(message);
	}
	protected String compareDisks() {
		List<String> errorMessages = new ArrayList<>();	
		List<FormattedDisk> disk1 = null;
		try {
            Source source = Sources.create(wizard.getDiskname1()).orElseThrow();
            DiskFactory.Context ctx = Disks.inspect(source);
            disk1 = ctx.disks;
		} catch (Throwable t) {
			errorMessages.add(textBundle.
				format("CompareDisksResultsPane.UnableToLoadDiskN",
					1, t.getLocalizedMessage()));
		}
        List<FormattedDisk> disk2 = null;
		try {
            Source source = Sources.create(wizard.getDiskname2()).orElseThrow();
            DiskFactory.Context ctx = Disks.inspect(source);
			disk2 = ctx.disks;
		} catch (Throwable t) {
			errorMessages.add(textBundle.
				format("CompareDisksResultsPane.UnableToLoadDiskN",
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
		if (errorMessages.isEmpty()) {
			return textBundle.get("CompareDisksResultsPane.DisksMatch");
		}
		return String.join("\n", errorMessages);
	}
}
