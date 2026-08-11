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

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Allow the user to choose the names of the disk image, as well as the
 * volume name, if appropriate.
 * <br>
 * Created on Dec 16, 2002.
 * @author Rob Greene, John B. Matthews
 */
public class DiskImageNamePane extends WizardPane<DiskImageWizard.Pages> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final DiskImageWizard wizard;
	private Composite control;
	private final Composite parent;
	private Text fileName;
	// These can be hidden or visible depending on what disk type was chosen:
	private Label volumeLabel;
	private Text volumeText;
	/**
	 * Constructor for DiskImageNamePane.
	 */
	public DiskImageNamePane(Composite parent, DiskImageWizard wizard) {
		super();
		this.parent = parent;
		this.wizard = wizard;
	}
	/**
	 * Get the next visible pane.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public DiskImageWizard.Pages getNextPane() {
		return DiskImageWizard.Pages.ORDER;
	}
	/**
	 * Called when the page is activated.
	 */
	public void activate() {
		wizard.enableFinishButton(false);
		setNextButtonStatus();
		fileName.setFocus();

		boolean needVolumeDetails = wizard.isFormatProdos() || wizard.isFormatPascal();
		volumeLabel.setVisible(needVolumeDetails);
		volumeText.setVisible(needVolumeDetails);
		fileName.setText(wizard.getFileName());

		int maxLength = wizard.isFormatProdos() ? 15 : 7;
		String name = wizard.isFormatProdos()
				? textBundle.get("Prodos")
				: textBundle.get("Pascal");
		volumeLabel.setText(textBundle.format(
				"DiskImageNameLengthText", name, maxLength));
		volumeText.setText(wizard.getVolumeName());
		volumeText.setTextLimit(maxLength);

	}
	/**
	 * Create the wizard pane.
	 * Listen for Verify events on the Text widgets.
	 * Require upper case for optional volume name.
	 * Preserve names when navigating among panes.
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
		label.setText(textBundle.get("DiskImageNamePrompt"));
		fileName = new Text(control, SWT.BORDER);
		fileName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fileName.addListener(SWT.Verify, e -> {
            String s = edit(fileName.getText(), e);
            wizard.setFileName(s);
            setNextButtonStatus();
        });

		volumeLabel = new Label(control, SWT.WRAP);
		volumeLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		volumeText = new Text(control, SWT.BORDER);
		volumeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		volumeText.addListener(SWT.Verify, e -> {
            e.text = e.text.toUpperCase();
            String s = edit(volumeText.getText(), e);
            wizard.setVolumeName(s);
            setNextButtonStatus();
        });

		setNextButtonStatus();
		return control;
	}
	/**
	 * Edit a name in response to a Verify event.
	 * @param name the existing name
	 * @param e the verification event
	 * @return the modified name
	 */
	private String edit(String name, Event e) {
		if (e.character == '\b') {
			return name.substring(0, e.start)
				+ name.substring(e.end);
		} else {
			return name.substring(0, e.start)
				+ e.text + name.substring(e.end);
		}
	}
	/**
	 * Enable the Next button when data has been entered into all fields.
	 */
	protected void setNextButtonStatus() {
		String vName = wizard.getVolumeName();
		String fName = wizard.getFileName();
		if (wizard.isFormatProdos() || wizard.isFormatPascal()) {
			wizard.enableNextButton(
				fName != null && fName.length() > 0
				&& vName != null && vName.length() > 0
				&& vName.charAt(0) >= 'A' && vName.charAt(0) <= 'Z');
		} else {
			wizard.enableNextButton(fName != null && fName.length() > 0);
		}
	}
	/**
	 * Dispose of all resources.
	 */
	public void dispose() {
		control.dispose();
	}
}
