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
package com.webcodepro.applecommander.ui.swt.wizard;

import java.util.Stack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.util.ImageCanvas;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * A framework for displaying a wizard-like user interface.
 * @author Rob Greene
 */
public abstract class Wizard {
	private TextBundle textBundle = UiBundle.getInstance();
	private Shell parent;
	private Shell dialog;
	private Image logo;
	private String title;
	private Stack wizardPanes = new Stack();
	private boolean wizardCompleted;
	private Button backButton;
	private Button nextButton;
	private Button finishButton;
	private Composite contentPane;
	private ImageCanvas imageCanvas;
	/**
	 * Constructor for Wizard.
	 */
	public Wizard(Shell parent, Image logo, String title) {
		super();
		this.parent = parent;
		this.logo = logo;
		this.title= title;
	}
	/**
	 * Create the dialog.
	 */
	private void createDialog() {
		dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(title);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.justify = true;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		layout.spacing = 3;
		dialog.setLayout(layout);

		// Wizard logo		
		RowData rowData = new RowData();
		rowData.width = logo.getImageData().width;
		rowData.height = logo.getImageData().height;
		imageCanvas = new ImageCanvas(dialog, SWT.BORDER, logo, rowData);

		// Starting pane
		rowData = new RowData();
		rowData.width = logo.getImageData().width;
		contentPane = new Composite(dialog, SWT.BORDER);
		contentPane.setLayoutData(rowData);
		contentPane.setLayout(new FillLayout());

		// Bottom row of buttons
		Composite composite = new Composite(dialog, SWT.NONE);
		composite.setLayoutData(rowData);
		composite.setLayout(new FillLayout(SWT.HORIZONTAL));
		Button button = new Button(composite, SWT.PUSH);
		button.setText(textBundle.get("CancelButton")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setWizardCompleted(false);
				getDialog().close();
			}
		});
		backButton = new Button(composite, SWT.PUSH);
		backButton.setEnabled(false);
		backButton.setText(textBundle.get("BackButton")); //$NON-NLS-1$
		backButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				WizardPane current = (WizardPane) getWizardPanes().pop();
				WizardPane previous = (WizardPane) getWizardPanes().peek();
				getBackButton().setEnabled(getWizardPanes().size() > 1);
				current.dispose();
				previous.open();
				getDialog().pack();
			}
		});
		nextButton = new Button(composite, SWT.PUSH);
		nextButton.setText(textBundle.get("NextButton")); //$NON-NLS-1$
		nextButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				WizardPane current = (WizardPane) getWizardPanes().peek();
				WizardPane next = current.getNextPane();
				getWizardPanes().add(next);
				getBackButton().setEnabled(getWizardPanes().size() > 1);
				current.dispose();
				next.open();
				getDialog().pack();
			}
		});
		finishButton = new Button(composite, SWT.PUSH);
		finishButton.setEnabled(false);
		finishButton.setText(textBundle.get("FinishButton")); //$NON-NLS-1$
		finishButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setWizardCompleted(true);
				getDialog().close();
			}
		});
		
		WizardPane wizardPane = createInitialWizardPane();
		wizardPanes.add(wizardPane);
		wizardPane.open();

		dialog.pack();
	}
	/**
	 * Create the initial display used in the wizard.
	 */
	public abstract WizardPane createInitialWizardPane();
	/**
	 * Open and display the dialog.
	 */
	public void open() {
		createDialog();
		SwtUtil.center(parent, dialog);
		dialog.open();
		Display display = dialog.getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep ();
		}
	}
	/**
	 * Dispose of all panels and resources.
	 */
	public void dispose() {
		while (!wizardPanes.empty()) {
			WizardPane pane = (WizardPane) wizardPanes.pop();
			pane.dispose();
			pane = null;
		}
		imageCanvas.dispose();
		dialog.dispose();
		backButton.dispose();
		nextButton.dispose();
		finishButton.dispose();
		contentPane.dispose();
	}
	/**
	 * Indicates if the wizard was completed.
	 */
	public boolean isWizardCompleted() {
		return wizardCompleted;
	}
	/**
	 * Enable/disable the next button.
	 */
	public void enableNextButton(boolean state) {
		nextButton.setEnabled(state);
		if (!finishButton.isEnabled()) dialog.setDefaultButton(nextButton);
	}
	/**
	 * Enable/disable the finish button.
	 */
	public void enableFinishButton(boolean state) {
		finishButton.setEnabled(state);
		dialog.setDefaultButton(finishButton);
	}
	/**
	 * Get the content pane.
	 */
	protected Composite getContentPane() {
		return contentPane;
	}
	/**
	 * Get the Wizard dialog Shell object.  Used by WizardPanes if a popup
	 * window is needed.
	 */
	public Shell getDialog() {
		return dialog;
	}
	/**
	 * @return Returns the backButton.
	 */
	protected Button getBackButton() {
		return backButton;
	}
	/**
	 * @return Returns the wizardPanes.
	 */
	protected Stack getWizardPanes() {
		return wizardPanes;
	}
	/**
	 * @param wizardCompleted The wizardCompleted to set.
	 */
	protected void setWizardCompleted(boolean wizardCompleted) {
		this.wizardCompleted = wizardCompleted;
	}
}
