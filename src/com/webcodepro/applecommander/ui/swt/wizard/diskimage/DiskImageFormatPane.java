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
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Allow the user to choose the which operating system to format the 
 * disk with.
 * <br>
 * Created on Dec 15, 2002.
 * @author Rob Greene
 */
public class DiskImageFormatPane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
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
				wizard.setSize(Disk.APPLE_140KB_DISK);
				return new DiskImageNamePane(parent, wizard);
			case DiskImageWizard.FORMAT_UNIDOS:
				wizard.setOrder(DiskImageWizard.ORDER_DOS);
				wizard.setSize(Disk.APPLE_800KB_2IMG_DISK);
				return new DiskImageNamePane(parent, wizard);
			case DiskImageWizard.FORMAT_OZDOS:
				wizard.setOrder(DiskImageWizard.ORDER_PRODOS);
				wizard.setSize(Disk.APPLE_800KB_2IMG_DISK);
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
			textBundle.get("DiskImageFormatPrompt")); //$NON-NLS-1$
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite buttonSubpanel = new Composite(control, SWT.NULL);
		buttonSubpanel.setLayout(subpanelLayout);
		createRadioButton(buttonSubpanel, textBundle.get("Dos"),  //$NON-NLS-1$
			DiskImageWizard.FORMAT_DOS33,
			textBundle.get("DiskImageFormatDosTooltip")); //$NON-NLS-1$
		createRadioButton(buttonSubpanel, textBundle.get("Unidos"),  //$NON-NLS-1$
			DiskImageWizard.FORMAT_UNIDOS,
			textBundle.get("DiskImageFormatUnidosTooltip")); //$NON-NLS-1$
		createRadioButton(buttonSubpanel, textBundle.get("Ozdos"),  //$NON-NLS-1$
			DiskImageWizard.FORMAT_OZDOS, 
			textBundle.get("DiskImageFormatOzdosTooltip")); //$NON-NLS-1$
		createRadioButton(buttonSubpanel, textBundle.get("Prodos"),  //$NON-NLS-1$
			DiskImageWizard.FORMAT_PRODOS, 
			textBundle.get("DiskImageFormatProdosTooltip")); //$NON-NLS-1$
		createRadioButton(buttonSubpanel, textBundle.get("Pascal"),  //$NON-NLS-1$
			DiskImageWizard.FORMAT_PASCAL, 
			textBundle.get("DiskImageFormatPascalTooltip")); //$NON-NLS-1$
		createRadioButton(buttonSubpanel, textBundle.get("Rdos"),  //$NON-NLS-1$
			DiskImageWizard.FORMAT_RDOS, 
			textBundle.get("DiskImageFormatRdosTooltip")); //$NON-NLS-1$
		createRadioButton(buttonSubpanel, textBundle.get("Cpm"),  //$NON-NLS-1$
			DiskImageWizard.FORMAT_CPM, 
			textBundle.get("DiskImageFormatCpmTooltip")); //$NON-NLS-1$
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
				getWizard().setFormat(format);
			}
		});
	}
	/**
	 * Dispose of any resources.
	 */
	public void dispose() {
		control.dispose();
	}
	
	protected DiskImageWizard getWizard() {
		return wizard;
	}
}
