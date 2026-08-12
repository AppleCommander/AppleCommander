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
package com.webcodepro.applecommander.ui.swt.wizard.diskimage;

import com.webcodepro.applecommander.storage.DiskConstants;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Allow the user to choose the which operating system to format the 
 * disk with.
 * <br>
 * Created on Dec 15, 2002.
 * @author Rob Greene
 */
public class DiskImageFormatPane extends WizardPane<DiskImageWizard.Pages> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final DiskImageWizard wizard;
	private Composite control;
	private final Composite parent;
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
	public DiskImageWizard.Pages getNextPane() {
		switch (wizard.getFormat()) {
			case DiskImageWizard.FORMAT_DOS33:
			case DiskImageWizard.FORMAT_RDOS:
			case DiskImageWizard.FORMAT_CPM:
				wizard.setOrder(DiskImageWizard.ORDER_DOS);
				wizard.setSize(DiskConstants.APPLE_140KB_DISK);
				return DiskImageWizard.Pages.NAME;
            case DiskImageWizard.FORMAT_UNIDOS:
            case DiskImageWizard.FORMAT_OZDOS:
				wizard.setOrder(DiskImageWizard.ORDER_PRODOS);
				wizard.setSize(DiskConstants.APPLE_800KB_DISK);
				return DiskImageWizard.Pages.NAME;
            case DiskImageWizard.FORMAT_PASCAL:
			case DiskImageWizard.FORMAT_PRODOS:
				wizard.setOrder(DiskImageWizard.ORDER_PRODOS);
				return DiskImageWizard.Pages.SIZE;
		}
		return null;
	}
	/**
	 * Called when the page is activated.
	 */
	public void activate() {
		wizard.enableNextButton(true);
		wizard.enableFinishButton(false);
	}
	/**
	 * Create and display the wizard pane.
	 */
	public Control create() {
		control = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		control.setLayout(layout);
		Label label = new Label(control, SWT.WRAP);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label.setText(
			textBundle.get("DiskImageFormatPrompt"));
		createRadioButton(control, textBundle.get("Dos"),
			DiskImageWizard.FORMAT_DOS33,
			textBundle.get("DiskImageFormatDosTooltip"));
		createRadioButton(control, textBundle.get("Unidos"),
			DiskImageWizard.FORMAT_UNIDOS,
			textBundle.get("DiskImageFormatUnidosTooltip"));
		createRadioButton(control, textBundle.get("Ozdos"),
			DiskImageWizard.FORMAT_OZDOS, 
			textBundle.get("DiskImageFormatOzdosTooltip"));
		createRadioButton(control, textBundle.get("Prodos"),
			DiskImageWizard.FORMAT_PRODOS, 
			textBundle.get("DiskImageFormatProdosTooltip"));
		createRadioButton(control, textBundle.get("Pascal"),
			DiskImageWizard.FORMAT_PASCAL, 
			textBundle.get("DiskImageFormatPascalTooltip"));
		createRadioButton(control, textBundle.get("Rdos"),
			DiskImageWizard.FORMAT_RDOS, 
			textBundle.get("DiskImageFormatRdosTooltip"));
		createRadioButton(control, textBundle.get("Cpm"),
			DiskImageWizard.FORMAT_CPM, 
			textBundle.get("DiskImageFormatCpmTooltip"));
		control.pack();
		return control;
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
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
