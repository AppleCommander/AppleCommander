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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Allow the user to choose the names of the disk image, as well as the
 * volume name, if appropriate.
 * <br>
 * Created on Dec 16, 2002.
 * @author Rob Greene
 */
public class DiskImageNamePane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
	private DiskImageWizard wizard;
	private Composite control;
	private Composite parent;
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
	public WizardPane getNextPane() {
		return new DiskImageOrderPane(parent, wizard);
	}
	/**
	 * Create the wizard pane.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#open()
	 */
	public void open() {
		control = new Composite(parent, SWT.NULL);
		wizard.enableNextButton(false);
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
			textBundle.get("DiskImageNamePrompt")); //$NON-NLS-1$
		final Text filename = new Text(control, SWT.BORDER);
		filename.setFocus();
		RowData rowData = new RowData();
		rowData.width = parent.getClientArea().width - 50;
		filename.setLayoutData(rowData);
		filename.setText(wizard.getFileName());
		filename.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				getWizard().setFileName(filename.getText());
				setButtonStatus();
			}
		});
		if (wizard.isFormatProdos() || wizard.isFormatPascal()) {
			int maxLength = wizard.isFormatProdos() ? 15 : 7;
			label = new Label(control, SWT.WRAP);
			Object[] objects = new Object[2];
			objects[0] = wizard.isFormatProdos() ? textBundle.get("Prodos")  //$NON-NLS-1$
					: textBundle.get("Pascal"); //$NON-NLS-1$
			objects[1] = new Integer(maxLength);
			label.setText(textBundle.format(
					"DiskImageNameLengthText", objects)); //$NON-NLS-1$
			final Text volumename = new Text(control, SWT.BORDER);
			volumename.setText(wizard.getVolumeName());
			volumename.setTextLimit(maxLength);
			volumename.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent event) {
					getWizard().setVolumeName(volumename.getText().toUpperCase());
					setButtonStatus();
				}
			});
		}
	}
	/**
	 * Enable the Next button when data has been entered into all fields.
	 */
	protected void setButtonStatus() {
		String volumeName = wizard.getVolumeName();
		String fileName = wizard.getFileName();
		if (wizard.isFormatProdos() || wizard.isFormatPascal()) {
			wizard.enableNextButton(
				fileName != null && fileName.length() > 0
				&& volumeName != null && volumeName.length() > 0);
		} else {
			wizard.enableNextButton(fileName != null && fileName.length() > 0);
		}
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
