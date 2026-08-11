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
package com.webcodepro.applecommander.ui.swt.wizard;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.util.ImageCanvas;
import com.webcodepro.applecommander.ui.swt.util.SwtUtil;
import com.webcodepro.applecommander.util.Host;
import com.webcodepro.applecommander.util.TextBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * A framework for displaying a wizard-like user interface.
 * @author Rob Greene
 */
public abstract class Wizard<E> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final Shell parent;
	private Shell dialog;
	private final Image logo;
	private final String title;
	private final Map<E,Control> wizardControls = new HashMap<>();
	private final Map<E,WizardPane<E>> wizardPanes = new HashMap<>();
	private final StackLayout stackLayout = new StackLayout();
	private final Deque<E> wizardHistory = new ArrayDeque<>();
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
		int styles = SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE;
		if (Host.isMacosx()) {
			styles |= SWT.SHEET;
		}
		dialog = new Shell(parent, styles);
		dialog.setText(title);
		GridLayout layout = new GridLayout();
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		dialog.setLayout(layout);

		// Wizard logo
		GridData imageLayoutData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_CENTER);
		imageLayoutData.widthHint = logo.getImageData().width;
		imageLayoutData.heightHint = logo.getImageData().height;
		imageCanvas = new ImageCanvas(dialog, SWT.BORDER, logo,imageLayoutData);

		// Content pane
		contentPane = new Composite(dialog, SWT.BORDER);
		contentPane.setLayoutData(new GridData(GridData.FILL_BOTH));
		contentPane.setLayout(stackLayout);

		// Bottom row of buttons
		Composite composite = new Composite(dialog, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END));
		composite.setLayout(new FillLayout(SWT.HORIZONTAL));
		Button button = new Button(composite, SWT.PUSH);
		button.setText(textBundle.get("CancelButton"));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setWizardCompleted(false);
				getDialog().close();
			}
		});
		backButton = new Button(composite, SWT.PUSH);
		backButton.setEnabled(false);
		backButton.setText(textBundle.get("BackButton"));
		backButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				E _ = wizardHistory.pop();
				E previousPane = wizardHistory.peek();
				getBackButton().setEnabled(wizardHistory.size() > 1);
				stackLayout.topControl = wizardControls.get(previousPane);
				contentPane.layout();
				wizardPanes.get(previousPane).activate();
			}
		});
		nextButton = new Button(composite, SWT.PUSH);
		nextButton.setText(textBundle.get("NextButton"));
		nextButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				E currentPane = wizardHistory.peek();
				E nextPane = wizardPanes.get(currentPane).getNextPane();
				wizardHistory.push(nextPane);
				getBackButton().setEnabled(wizardHistory.size() > 1);
				stackLayout.topControl = wizardControls.get(nextPane);
				contentPane.layout();
				wizardPanes.get(nextPane).activate();
			}
		});
		finishButton = new Button(composite, SWT.PUSH);
		finishButton.setEnabled(false);
		finishButton.setText(textBundle.get("FinishButton"));
		finishButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setWizardCompleted(true);
				getDialog().close();
			}
		});

		// Keep the WizardPane for the page-to-page logic
		wizardPanes.putAll(createWizardPanes());
		// Setup all the controls and hold those
		wizardPanes.forEach((key,value) -> {
			wizardControls.put(key, value.create());
		});
		wizardHistory.push(getFirstWizardPane());
		stackLayout.topControl = wizardControls.get(wizardHistory.peek());
		contentPane.layout();
		wizardPanes.get(wizardHistory.peek()).activate();

		dialog.pack();
	}
	/**
	 * Create the panes used in the wizard.
	 */
	public abstract Map<E,WizardPane<E>> createWizardPanes();
	/**
	 * Indicates the first pane of the wizard.
	 */
	public abstract E getFirstWizardPane();
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
		for (Control wizardPane : wizardControls.values()) {
			wizardPane.dispose();
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
	 * @param wizardCompleted The wizardCompleted to set.
	 */
	protected void setWizardCompleted(boolean wizardCompleted) {
		this.wizardCompleted = wizardCompleted;
	}
}
