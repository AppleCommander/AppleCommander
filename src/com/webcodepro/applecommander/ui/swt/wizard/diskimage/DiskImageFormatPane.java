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
 * Allow the user to choose the which operating system to format the 
 * disk with.
 * <br>
 * Created on Dec 15, 2002.
 * @author Rob Greene
 */
public class DiskImageFormatPane extends WizardPane {
	private DiskImageWizard wizard;
	private Composite control;
	private Composite parent;
	/**
	 * Constructor for DiskImageFormatPane.
	 */
	public DiskImageFormatPane(Composite parent, DiskImageWizard wizard) {
		super();
		this.parent = parent;
		this.wizard = wizard;
	}
	/**
	 * Get the next WizardPane.
	 * Note that the order and size are set, or defaults are
	 * chosen.
	 */
	public WizardPane getNextPane() {
		switch (wizard.getFormat()) {
			case DiskImageWizard.FORMAT_DOS33:
			case DiskImageWizard.FORMAT_RDOS:
			case DiskImageWizard.FORMAT_CPM:
				wizard.setOrder(DiskImageWizard.ORDER_DOS);
				wizard.setSize(FormattedDisk.APPLE_140KB_DISK);
				return new DiskImageNamePane(parent, wizard);
			case DiskImageWizard.FORMAT_UNIDOS:
				wizard.setOrder(DiskImageWizard.ORDER_DOS);
				wizard.setSize(FormattedDisk.APPLE_800KB_2IMG_DISK);
				return new DiskImageNamePane(parent, wizard);
			case DiskImageWizard.FORMAT_OZDOS:
				wizard.setOrder(DiskImageWizard.ORDER_PRODOS);
				wizard.setSize(FormattedDisk.APPLE_800KB_2IMG_DISK);
				return new DiskImageNamePane(parent, wizard);
			case DiskImageWizard.FORMAT_PASCAL:
			case DiskImageWizard.FORMAT_PRODOS:
				wizard.setOrder(DiskImageWizard.ORDER_PRODOS);
				return new DiskImageSizePane(parent, wizard);
		}
		return null;
	}
	/**
	 * Create and display the wizard pane.
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
			"Please choose the operating system with which to format\nthe disk image:");
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite buttonSubpanel = new Composite(control, SWT.NULL);
		buttonSubpanel.setLayout(subpanelLayout);
		createRadioButton(buttonSubpanel, "DOS 3.3", DiskImageWizard.FORMAT_DOS33,
			"This is Apple's DOS 3.3 format.  The disk will automatically be\n"
			+ "sized at 140K.");
		createRadioButton(buttonSubpanel, "UniDOS", DiskImageWizard.FORMAT_UNIDOS,
			"UniDOS was created to allow DOS 3.3 to operate with 800K disk\n"
			+ "drives.  The disk will default to 800K.");
		createRadioButton(buttonSubpanel, "OzDOS", DiskImageWizard.FORMAT_OZDOS,
			"OzDOS was created to allow DOS 3.3 to operate with 800K disk\n"
			+ "drives.  The disk will default to 800K.");
		createRadioButton(buttonSubpanel, "ProDOS", DiskImageWizard.FORMAT_PRODOS,
			"ProDOS was (is?) Apple's professional DOS for the Apple ][ series\n"
			+ "of computers. ProDOS allows subdirectories and can use devices\n"
			+ "up to 32MB in size. You will be presented with image sizing\n"
			+ "options from the 140K disk to a 32MB hard disk.");
		createRadioButton(buttonSubpanel, "Pascal", DiskImageWizard.FORMAT_PASCAL,
			"Apple Pascal formatted disks are part of the Pascal environment.\n"
			+ "Early implementations of Pascal only allowed 140K volumes, but\n"
			+ "later versions allowed 800K volumes (and possibly more). You\n"
			+ "will be presented with options for 140K or 800K.");
		createRadioButton(buttonSubpanel, "RDOS 2.1", DiskImageWizard.FORMAT_RDOS,
			"RDOS was created by (or for) SSI to protected their games. The\n"
			+ "original format appears to be a 13 sector disk. Most disk images\n"
			+ "that I've seen have been mapped onto a 16 sector disk (leaving 3\n"
			+ "sectors of each track unused.  The only image size RDOS supports\n"
			+ "is 140K.");
		createRadioButton(buttonSubpanel, "CP/M", DiskImageWizard.FORMAT_CPM,
			"CP/M for the Apple computer.");
		control.pack();
	}
	/**
	 * Create a radio button for the disk image format list.
	 */
	protected void createRadioButton(Composite composite, String label, 
		final int format, String helpText) {
			
		Button button = new Button(composite, SWT.RADIO);
		button.setText(label);
		button.setSelection(wizard.getFormat() == format);
		button.setToolTipText(helpText);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wizard.setFormat(format);
			}
		});
	}
	/**
	 * Dispose of any resources.
	 */
	public void dispose() {
		control.dispose();
	}
}
