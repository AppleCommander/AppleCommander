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
package com.webcodepro.applecommander.ui.swt.wizard.exportfile;

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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;

/**
 * Set locations and file names for the export.
 * <p>
 * Date created: Nov 8, 2002 11:18:47 PM
 * @author Rob Greene
 */
public class ExportFileDestinationPane extends WizardPane {
	private Composite parent;
	private Object layoutData;
	private Composite control;
	private ExportWizard wizard;
	private Text directoryText;
	/**
	 * Constructor for ExportFileDestinationPane.
	 */
	public ExportFileDestinationPane(Composite parent, ExportWizard exportWizard, Object layoutData) {
		super();
		this.parent = parent;
		this.wizard = exportWizard;
		this.layoutData = layoutData;
	}
	/**
	 * This is the last pane in the wizard, so a null is returned to indicate no
	 * more pages.
	 * @see com.webcodepro.applecommander.gui.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return null;
	}
	/**
	 * Create and display the wizard pane.
	 * @see com.webcodepro.applecommander.gui.WizardPane#open()
	 */
	public void open() {
		control = new Composite(parent, SWT.NULL);
		control.setLayoutData(layoutData);
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
		label.setText("Please indicate the destination for the exported files:");

		directoryText = new Text(control, SWT.WRAP | SWT.BORDER);
		if (wizard.getDirectory() != null) directoryText.setText(wizard.getDirectory());
		directoryText.setLayoutData(new RowData(parent.getSize().x - 30, -1));
		directoryText.setBackground(new Color(control.getDisplay(), 255,255,255));
		directoryText.setFocus();
		directoryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				Text text = (Text) event.getSource();
				wizard.setDirectory(text.getText());
			}
		});
		
		Button button = new Button(control, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(control.getShell());
				directoryDialog.setFilterPath(directoryText.getText());
				directoryDialog.setMessage(
					"Please choose the directory to which exported files will be written");
				String directory = directoryDialog.open();
				if (directory != null) {
					directoryText.setText(directory);
				}
			}
		});
	}
	/**
	 * Dispose of any resources.
	 * @see com.webcodepro.applecommander.gui.WizardPane#dispose()
	 */
	public void dispose() {
		directoryText.dispose();
		control.dispose();
		control = null;
	}
}
