/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2004 by Robert Greene
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Provides the wizard pane which gets the disks to compare.
 * <p>
 * @author Rob Greene
 */
public class CompareDisksStartPane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
	private Composite parent;
	private Object layoutData;
	private Composite control;
	private CompareDisksWizard wizard;
	private Text diskname1Text;
	private Text diskname2Text;
	/**
	 * Constructor for CompareDisksStartPane.
	 */
	public CompareDisksStartPane(Composite parent, CompareDisksWizard wizard, Object layoutData) {
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
		label.setText(textBundle.get("CompareDisksStartPane.Description")); //$NON-NLS-1$

		label = new Label(control, SWT.WRAP);
		label.setText(getDiskLabel(1));

		diskname1Text = new Text(control, SWT.WRAP | SWT.BORDER);
		if (wizard.getDiskname1() != null) diskname1Text.setText(wizard.getDiskname1());
		diskname1Text.setLayoutData(new RowData(300, -1));
		diskname1Text.setBackground(new Color(control.getDisplay(), 255,255,255));
		diskname1Text.setFocus();
		diskname1Text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				Text text = (Text) event.getSource();
				getWizard().setDiskname1(text.getText());
			}
		});
		
		Button button = new Button(control, SWT.PUSH);
		button.setText(textBundle.get("BrowseButton")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getControl().getShell());
				fileDialog.setFilterPath(getDiskname1Text().getText());
				fileDialog.setText(getDiskLabel(1));
				String filename = fileDialog.open();
				if (filename != null) {
					getDiskname1Text().setText(filename);
				}
			}
		});
	
		label = new Label(control, SWT.WRAP);
		label.setText(getDiskLabel(2));

		diskname2Text = new Text(control, SWT.WRAP | SWT.BORDER);
		if (wizard.getDiskname2() != null) diskname2Text.setText(wizard.getDiskname2());
		diskname2Text.setLayoutData(new RowData(300, -1));
		diskname2Text.setBackground(new Color(control.getDisplay(), 255,255,255));
		diskname2Text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				Text text = (Text) event.getSource();
				getWizard().setDiskname2(text.getText());
			}
		});
		
		button = new Button(control, SWT.PUSH);
		button.setText(textBundle.get("BrowseButton")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getControl().getShell());
				fileDialog.setFilterPath(getDiskname2Text().getText());
				fileDialog.setText(getDiskLabel(2));
				String filename = fileDialog.open();
				if (filename != null) {
					getDiskname2Text().setText(filename);
				}
			}
		});
	}
	/**
	 * Get the next pane. A null return indicates the end of the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return new CompareDisksResultsPane(parent, wizard, layoutData);
	}
	/**
	 * Dispose of resources.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#dispose()
	 */
	public void dispose() {
		control.dispose();
		control = null;
	}
	protected Composite getControl() {
		return control;
	}
	protected Text getDiskname1Text() {
		return diskname1Text;
	}
	protected Text getDiskname2Text() {
		return diskname2Text;
	}
	protected CompareDisksWizard getWizard() {
		return wizard;
	}
	protected String getDiskLabel(int diskNumber) {
		return textBundle.format("CompareDisksStartPane.DiskNLabel", //$NON-NLS-1$
				new Object[] { new Integer(diskNumber) });
	}
}
