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
package com.webcodepro.applecommander.ui.swt.wizard.diskimage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;

/**
 * Allow the user to choose the size of the disk image, as appropriate.
 * <br>
 * Created on Dec 15, 2002.
 * @author Rob Greene
 */
public class DiskImageSizePane extends WizardPane {
	private DiskImageWizard wizard;
	private Composite control;
	private Composite parent;
	/**
	 * Constructor for DiskImageSizePane.
	 */
	public DiskImageSizePane(Composite parent, DiskImageWizard wizard) {
		super();
		this.parent = parent;
		this.wizard = wizard;
	}
	/**
	 * Get the next visible pane.
	 * @see com.webcodepro.applecommander.ui.swt.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return new DiskImageNamePane(parent, wizard);
	}
	/**
	 * Create the wizard pane.
	 * @see com.webcodepro.applecommander.ui.swt.WizardPane#open()
	 */
	public void open() {
		control = new Composite(parent, SWT.NULL);
		wizard.enableNextButton(true);
		wizard.enableFinishButton(false);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.justify = true;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		layout.spacing = 3;
		control.setLayout(layout);
		Label label = new Label(control, SWT.WRAP);
		label.setText(
			"Please choose the disk image size:");
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite buttonSubpanel = new Composite(control, SWT.NULL);
		buttonSubpanel.setLayout(subpanelLayout);
		createRadioButton(buttonSubpanel, "140KB", FormattedDisk.APPLE_140KB_DISK,
			"The Disk II 5.25\" floppy drive.");
		createRadioButton(buttonSubpanel, "800KB", FormattedDisk.APPLE_800KB_2IMG_DISK,
			"The Apple UniDisk 3.5\" drive.");
		if (wizard.getFormat() == DiskImageWizard.FORMAT_PRODOS) {
			createRadioButton(buttonSubpanel, "5MB", FormattedDisk.APPLE_5MB_HARDDISK,
				"A 5MB hard disk.");
			createRadioButton(buttonSubpanel, "10MB", FormattedDisk.APPLE_10MB_HARDDISK,
				"A 10MB hard disk.");
			createRadioButton(buttonSubpanel, "20MB", FormattedDisk.APPLE_20MB_HARDDISK,
				"A 20MB hard disk.");
			createRadioButton(buttonSubpanel, "32MB", FormattedDisk.APPLE_32MB_HARDDISK,
				"A 32MB hard disk.");
		}
	}
	/**
	 * Create a radio button for the disk image size list.
	 */
	protected void createRadioButton(Composite composite, String label, 
		final int size, String helpText) {
			
		Button button = new Button(composite, SWT.RADIO);
		button.setText(label);
		button.setToolTipText(helpText);
		button.setSelection(wizard.getSize() == size);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wizard.setSize(size);
			}
		});
	}
	/**
	 * Dispose of all resources.
	 * @see com.webcodepro.applecommander.ui.swt.WizardPane#dispose()
	 */
	public void dispose() {
		control.dispose();
	}
}
