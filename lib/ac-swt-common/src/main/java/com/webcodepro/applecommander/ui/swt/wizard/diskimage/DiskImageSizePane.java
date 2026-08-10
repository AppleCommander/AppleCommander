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
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Allow the user to choose the size of the disk image, as appropriate.
 * <br>
 * Created on Dec 15, 2002.
 * @author Rob Greene
 */
public class DiskImageSizePane extends WizardPane<DiskImageWizard.Pages> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final DiskImageWizard wizard;
	private Composite control;
	private final Composite parent;
	// These are dynamic based on format being ProDOS:
	private Button size5MbButton;
	private Button size10MbButton;
	private Button size20MbButton;
	private Button size32MbButton;
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
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public DiskImageWizard.Pages getNextPane() {
		return DiskImageWizard.Pages.NAME;
	}
	/**
	 * Called when the page is activated.
	 */
	public void activate() {
		wizard.enableNextButton(true);
		wizard.enableFinishButton(false);

		boolean prodosFormat = wizard.getFormat() == DiskImageWizard.FORMAT_PRODOS;
		size5MbButton.setVisible(prodosFormat);
		size10MbButton.setVisible(prodosFormat);
		size20MbButton.setVisible(prodosFormat);
		size32MbButton.setVisible(prodosFormat);
	}
	/**
	 * Create the wizard pane.
	 */
	public Control create() {
		control = new Composite(parent, SWT.NULL);
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
			textBundle.get("DiskImageSizePrompt"));
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite buttonSubpanel = new Composite(control, SWT.NULL);
		buttonSubpanel.setLayout(subpanelLayout);
		createRadioButton(buttonSubpanel, textBundle.get("DiskImageSize140Kb"),
			DiskConstants.APPLE_140KB_DISK,
			textBundle.get("DiskImageSize140KbText"));
		createRadioButton(buttonSubpanel, textBundle.get("DiskImageSize800Kb"),
			DiskConstants.APPLE_800KB_DISK,
			textBundle.get("DiskImageSize800KbText"));
		size5MbButton = createRadioButton(buttonSubpanel, textBundle.get("DiskImageSize5Mb"),
			DiskConstants.APPLE_5MB_HARDDISK,
			textBundle.get("DiskImageSize5MbText"));
		size10MbButton = createRadioButton(buttonSubpanel, textBundle.get("DiskImageSize10Mb"),
			DiskConstants.APPLE_10MB_HARDDISK,
			textBundle.get("DiskImageSize10MbText"));
		size20MbButton = createRadioButton(buttonSubpanel, textBundle.get("DiskImageSize20Mb"),
			DiskConstants.APPLE_20MB_HARDDISK,
			textBundle.get("DiskImageSize20MbText"));
		size32MbButton = createRadioButton(buttonSubpanel, textBundle.get("DiskImageSize32Mb"),
			DiskConstants.APPLE_32MB_HARDDISK,
			textBundle.get("DiskImageSize32MbText"));
		return control;
	}
	/**
	 * Create a radio button for the disk image size list.
	 */
	protected Button createRadioButton(Composite composite, String label, final int size, String helpText) {
		Button button = new Button(composite, SWT.RADIO);
		button.setText(label);
		button.setToolTipText(helpText);
		button.setSelection(wizard.getSize() == size);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setSize(size);
			}
		});
		return button;
	}
	/**
	 * Dispose of all resources.
	 */
	public void dispose() {
		control.dispose();
	}
	
	protected DiskImageWizard getWizard() {
		return wizard;
	}
}
