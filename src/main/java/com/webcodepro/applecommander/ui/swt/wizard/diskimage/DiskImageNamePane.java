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
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Allow the user to choose the names of the disk image, as well as the
 * volume name, if appropriate.
 * <br>
 * Created on Dec 16, 2002.
 * @author Rob Greene, John B. Matthews
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
	 * Listen for Verify events on the Text widgets.
	 * Require upper case for optional volume name.
	 * Preserve names when navigating among panes.
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
		final Text fileName = new Text(control, SWT.BORDER);
		fileName.setFocus();
		RowData rowData = new RowData();
		rowData.width = parent.getClientArea().width - 50;
		fileName.setLayoutData(rowData);
		fileName.setText(wizard.getFileName());
		setButtonStatus();
		fileName.addListener(SWT.Verify, new Listener() {
			public void handleEvent(Event e) {
				String s = edit(fileName.getText(), e);
				wizard.setFileName(s);
				setButtonStatus();
			}
		});
		if (wizard.isFormatProdos() || wizard.isFormatPascal()) {
			int maxLength = wizard.isFormatProdos() ? 15 : 7;
			label = new Label(control, SWT.WRAP);
			Object[] objects = new Object[2];
			objects[0] = wizard.isFormatProdos()
					? textBundle.get("Prodos")  //$NON-NLS-1$
					: textBundle.get("Pascal"); //$NON-NLS-1$
			objects[1] = new Integer(maxLength);
			label.setText(textBundle.format(
					"DiskImageNameLengthText", objects)); //$NON-NLS-1$
			final Text volumeName = new Text(control, SWT.BORDER);
			volumeName.setText(wizard.getVolumeName());
			volumeName.setTextLimit(maxLength);
			volumeName.addListener(SWT.Verify, new Listener() {
				public void handleEvent(Event e) {
					e.text = e.text.toUpperCase();
					String s = edit(volumeName.getText(), e);
					wizard.setVolumeName(s);
					setButtonStatus();
				}
			});
		}
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
	protected void setButtonStatus() {
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
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#dispose()
	 */
	public void dispose() {
		control.dispose();
	}
	
	protected DiskImageWizard getWizard() {
		return wizard;
	}
}
