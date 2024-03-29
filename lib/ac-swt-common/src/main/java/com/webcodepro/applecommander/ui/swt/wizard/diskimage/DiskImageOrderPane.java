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
 * Allow the user to choose the order of the disk image, as well as
 * compression.
 * <br>
 * Created on Dec 16, 2002.
 * @author Rob Greene
 */
public class DiskImageOrderPane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
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
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return null;
	}
	/**
	 * Create the wizard pane.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#open()
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
			label.setText(textBundle.get("DiskImageOrderProdosOnly")); //$NON-NLS-1$
		} else {
			label.setText(textBundle.get("DiskImageOrderPrompt")); //$NON-NLS-1$
		}
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite buttonSubpanel = new Composite(control, SWT.NULL);
		buttonSubpanel.setLayout(subpanelLayout);
		createRadioButton(buttonSubpanel, textBundle.get("DiskImageOrderDosLabel"),  //$NON-NLS-1$
			DiskImageWizard.ORDER_DOS,
			textBundle.get("DiskImageOrderDosText")); //$NON-NLS-1$
		createRadioButton(buttonSubpanel, textBundle.get("DiskImageOrderProdosLabel"),  //$NON-NLS-1$
			DiskImageWizard.ORDER_PRODOS,
			textBundle.get("DiskImageOrderProdosText")); //$NON-NLS-1$
		if (wizard.getSize() == Disk.APPLE_140KB_DISK) {
			createRadioButton(buttonSubpanel, textBundle.get("DiskImageOrderNibbleLabel"), //$NON-NLS-1$
				DiskImageWizard.ORDER_NIBBLE,
				textBundle.get("DiskImageOrderNibbleText")); //$NON-NLS-1$
		}
		
		label = new Label(control, SWT.WRAP);
		if (wizard.isHardDisk()) {
			label.setText(textBundle.get("DiskImageOrderNoCompression")); //$NON-NLS-1$
		} else {
			label.setText(textBundle.get("DiskImageOrderCompressionPrompt")); //$NON-NLS-1$
		}
		final Button button = new Button(control, SWT.CHECK);
		button.setText(textBundle.get("DiskImageOrderGzipCheckbox")); //$NON-NLS-1$
		button.setToolTipText(textBundle.get("DiskImageOrderGzipTooltip")); //$NON-NLS-1$
		button.setSelection(wizard.isCompressed());
		button.setEnabled(!wizard.isHardDisk());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setCompressed(!getWizard().isCompressed());
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
				getWizard().setOrder(order);
			}
		});
	}
	/**
	 * Dispose of all resources.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#dispose()
	 */
	public void dispose() {
		control.dispose();
	}
	
	protected DiskImageWizard getWizard() {
		return wizard;
	}
}
