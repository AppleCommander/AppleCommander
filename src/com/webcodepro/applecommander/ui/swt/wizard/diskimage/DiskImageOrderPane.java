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

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;

/**
 * Allow the user to choose the order of the disk image, as well as
 * compression.
 * <br>
 * Created on Dec 16, 2002.
 * @author Rob Greene
 */
public class DiskImageOrderPane extends WizardPane {
	private DiskImageWizard wizard;
	private Composite control;
	private Composite parent;
	/**
	 * Constructor for DiskImageNamePane.
	 */
	public DiskImageOrderPane(Composite parent, DiskImageWizard wizard) {
		super();
		this.parent = parent;
		this.wizard = wizard;
	}
	/**
	 * Get the next visible pane.
	 * @see com.webcodepro.applecommander.ui.swt.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return null;
	}
	/**
	 * Create the wizard pane.
	 * @see com.webcodepro.applecommander.ui.swt.WizardPane#open()
	 */
	public void open() {
		control = new Composite(parent, SWT.NULL);
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
		Label label = new Label(control, SWT.WRAP);
		if (wizard.isHardDisk()) {
			label.setText("You have chosen a hard disk volume.  The only\n"
				+ "order allowed is ProDOS.");
		} else {
			label.setText("Please choose the order to use in this disk image:");
		}
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite buttonSubpanel = new Composite(control, SWT.NULL);
		buttonSubpanel.setLayout(subpanelLayout);
		createRadioButton(buttonSubpanel, "DOS ordered", 
			DiskImageWizard.ORDER_DOS,
			"Indicates that image data should be stored by track and sector.");
		createRadioButton(buttonSubpanel, "ProDOS ordered", 
			DiskImageWizard.ORDER_PRODOS,
			"Indicates that image data should be stored by block.");
		if (wizard.getSize() == Disk.APPLE_140KB_DISK) {
			createRadioButton(buttonSubpanel, "Nibble ordered",
				DiskImageWizard.ORDER_NIBBLE,
				"Indicates that this is a disk stored as a nibble image.  This is "
				+ "an image that consists of disk bytes.  It is only available for "
				+ "140KB 5.25\" disks.");
		}
		
		label = new Label(control, SWT.WRAP);
		if (wizard.isHardDisk()) {
			label.setText("Compression is not available for hard disk images.");
		} else {
			label.setText("Indicate if this disk image should be GZIP compressed:");
		}
		final Button button = new Button(control, SWT.CHECK);
		button.setText("GZip compression");
		button.setToolTipText("Compresses the disk image (*.gz).");
		button.setSelection(wizard.isCompressed());
		button.setEnabled(!wizard.isHardDisk());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wizard.setCompressed(!wizard.isCompressed());
			}
		});
	}
	/**
	 * Create a radio button for the disk image size list.
	 */
	protected void createRadioButton(Composite composite, String label, 
		final int order, String helpText) {
			
		Button button = new Button(composite, SWT.RADIO);
		button.setText(label);
		button.setToolTipText(helpText);
		button.setSelection(wizard.getOrder() == order);
		button.setEnabled(!wizard.isHardDisk());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wizard.setOrder(order);
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
