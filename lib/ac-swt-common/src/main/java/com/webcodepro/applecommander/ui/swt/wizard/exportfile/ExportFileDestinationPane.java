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
package com.webcodepro.applecommander.ui.swt.wizard.exportfile;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Set locations and file names for the export.
 * <p>
 * Date created: Nov 8, 2002 11:18:47 PM
 * @author Rob Greene
 */
public class ExportFileDestinationPane extends WizardPane<ExportWizard.Pages> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final Composite parent;
	private Composite control;
	private final ExportWizard wizard;
	private Text directoryText;
	/**
	 * Constructor for ExportFileDestinationPane.
	 */
	public ExportFileDestinationPane(Composite parent, ExportWizard exportWizard) {
		super();
		this.parent = parent;
		this.wizard = exportWizard;
	}
	/**
	 * This is the last pane in the wizard, so a null is returned to indicate no
	 * more pages.
	 */
	public ExportWizard.Pages getNextPane() {
		return null;
	}
	/**
	 * Called when the page is activated.
	 */
	public void activate() {
		wizard.enableNextButton(false);
		wizard.enableFinishButton(true);
		directoryText.setFocus();
		if (wizard.getDirectory() != null) directoryText.setText(wizard.getDirectory());
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
		label.setText(textBundle.get("ExportFilePrompt"));

		directoryText = new Text(control, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		GridData directoryGridData = new GridData(GridData.FILL_HORIZONTAL);
		directoryGridData.heightHint = (int)directoryText.getFont().getFontData()[0].height * 4;
		directoryText.setLayoutData(directoryGridData);
		directoryText.addModifyListener(event -> {
            Text text = (Text) event.getSource();
            getWizard().setDirectory(text.getText());
        });
		
		Button button = new Button(control, SWT.PUSH);
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		button.setText(textBundle.get("BrowseButton")); 
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
				directoryDialog.setFilterPath(getDirectoryText().getText());
				directoryDialog.setMessage(
					UiBundle.getInstance().get("ExportFileDirectoryPrompt")); 
				String directory = directoryDialog.open();
				if (directory != null) {
					getDirectoryText().setText(directory);
				}
			}
		});
		return control;
	}
	/**
	 * Dispose of any resources.
	 */
	public void dispose() {
		directoryText.dispose();
		control.dispose();
		control = null;
	}

	protected ExportWizard getWizard() {
		return wizard;
	}
	
	protected Shell getShell() {
		return control.getShell();
	}
	
	protected Text getDirectoryText() {
		return directoryText;
	}
}
