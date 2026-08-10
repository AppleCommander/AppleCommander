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
 * Allow the user to choose the order of the disk image, as well as
 * compression.
 * <br>
 * Created on Dec 16, 2002.
 * @author Rob Greene
 */
public class DiskImageOrderPane extends WizardPane<DiskImageWizard.Pages> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final DiskImageWizard wizard;
	private Composite control;
	private final Composite parent;
	private Button nibbleOrderButton;
	private Label imageOrderLabel;
	private Label compressionLabel;
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
	 */
	public DiskImageWizard.Pages getNextPane() {
		return null;
	}
	/**
	 * Called when the page is activated.
	 */
	public void activate() {
		wizard.enableNextButton(false);
		wizard.enableFinishButton(true);

		nibbleOrderButton.setEnabled(wizard.getSize() == DiskConstants.APPLE_140KB_DISK);
		if (wizard.isHardDisk()) {
			imageOrderLabel.setText(textBundle.get("DiskImageOrderProdosOnly"));
			compressionLabel.setText(textBundle.get("DiskImageOrderNoCompression"));
		} else {
			imageOrderLabel.setText(textBundle.get("DiskImageOrderPrompt"));
			compressionLabel.setText(textBundle.get("DiskImageOrderCompressionPrompt"));
		}

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
		imageOrderLabel = new Label(control, SWT.WRAP);
		imageOrderLabel.setText(textBundle.get("DiskImageOrderProdosOnly"));	// Trying to pick the "biggest"
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite buttonSubpanel = new Composite(control, SWT.NULL);
		buttonSubpanel.setLayout(subpanelLayout);
		createRadioButton(buttonSubpanel, textBundle.get("DiskImageOrderDosLabel"),
			DiskImageWizard.ORDER_DOS,
			textBundle.get("DiskImageOrderDosText"));
		createRadioButton(buttonSubpanel, textBundle.get("DiskImageOrderProdosLabel"),
			DiskImageWizard.ORDER_PRODOS,
			textBundle.get("DiskImageOrderProdosText"));
		nibbleOrderButton = createRadioButton(buttonSubpanel, textBundle.get("DiskImageOrderNibbleLabel"),
			DiskImageWizard.ORDER_NIBBLE,
			textBundle.get("DiskImageOrderNibbleText"));

		compressionLabel = new Label(control, SWT.WRAP);
		compressionLabel.setText(textBundle.get("DiskImageOrderCompressionPrompt"));	// Hopefully the "biggest"
		final Button button = new Button(control, SWT.CHECK);
		button.setText(textBundle.get("DiskImageOrderGzipCheckbox"));
		button.setToolTipText(textBundle.get("DiskImageOrderGzipTooltip"));
		button.setSelection(wizard.isCompressed());
		button.setEnabled(!wizard.isHardDisk());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setCompressed(!getWizard().isCompressed());
			}
		});
		return control;
	}
	/**
	 * Create a radio button for the disk image size list.
	 */
	protected Button createRadioButton(Composite composite, String label, final int order, String helpText) {
		Button button = new Button(composite, SWT.RADIO);
		button.setText(label);
		button.setToolTipText(helpText);
		button.setSelection(wizard.getOrder() == order);
		button.setEnabled(!wizard.isHardDisk());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setOrder(order);
				if (order == DiskImageWizard.ORDER_NIBBLE) {
					getWizard().setSize(DiskConstants.APPLE_140KB_NIBBLE_DISK);
				}
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
