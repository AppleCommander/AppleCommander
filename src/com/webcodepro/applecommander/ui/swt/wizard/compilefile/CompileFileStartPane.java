/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-3 by Robert Greene
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
package com.webcodepro.applecommander.ui.swt.wizard.compilefile;

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

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Provides the wizard pane which gets the export filter.
 * <p>
 * Date created: Nov 7, 2002 8:43:27 PM
 * @author Rob Greene
 */
public class CompileFileStartPane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
	private Composite parent;
	private Object layoutData;
	private Composite control;
	private CompileWizard wizard;
	private Text directoryText;
	/**
	 * Constructor for ExportFileStartPane.
	 */
	public CompileFileStartPane(Composite parent, CompileWizard compileWizard, Object layoutData) {
		super();
		this.parent = parent;
		this.wizard = compileWizard;
		this.layoutData = layoutData;
	}
	/**
	 * Open up and configure the wizard pane.
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
		label.setText(textBundle.get("CompileFileWarning")); //$NON-NLS-1$

		label = new Label(control, SWT.WRAP);
		label.setText(textBundle.get("CompileFileDestinationPrompt")); //$NON-NLS-1$

		directoryText = new Text(control, SWT.WRAP | SWT.BORDER);
		if (wizard.getDirectory() != null) directoryText.setText(wizard.getDirectory());
		directoryText.setLayoutData(new RowData(300, -1));
		directoryText.setBackground(new Color(control.getDisplay(), 255,255,255));
		directoryText.setFocus();
		directoryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				Text text = (Text) event.getSource();
				getWizard().setDirectory(text.getText());
			}
		});
		
		Button button = new Button(control, SWT.PUSH);
		button.setText(textBundle.get("BrowseButton")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(
						getControl().getShell());
				directoryDialog.setFilterPath(getDirectoryText().getText());
				directoryDialog.setMessage(UiBundle.getInstance().
						get("CompileFileDirectoryPrompt")); //$NON-NLS-1$
				String directory = directoryDialog.open();
				if (directory != null) {
					getDirectoryText().setText(directory);
				}
			}
		});
	}
	/**
	 * Get the next pane. A null return indicates the end of the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return null;
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

	protected Text getDirectoryText() {
		return directoryText;
	}

	protected CompileWizard getWizard() {
		return wizard;
	}
}
