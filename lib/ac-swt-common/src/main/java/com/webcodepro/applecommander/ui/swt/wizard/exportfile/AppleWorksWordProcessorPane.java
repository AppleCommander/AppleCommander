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

import com.webcodepro.applecommander.storage.filters.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Choose format for AppleWorks Word Processor export.
 * <p>
 * Date created: Nov 15, 2002 11:31:15 PM
 * @author Rob Greene
 */
public class AppleWorksWordProcessorPane extends WizardPane<ExportWizard.Pages> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final Composite parent;
	private Composite control;
	private final ExportWizard wizard;
	private Button textRenderingButton;
	private Button htmlRenderingButton;
	private Button rtfRenderingButton;
	/**
	 * Constructor for AppleWorksWordProcessorPane.
	 */
	public AppleWorksWordProcessorPane(Composite parent, ExportWizard exportWizard) {
		super();
		this.parent = parent;
		this.wizard = exportWizard;
	}
	/**
	 * Get the next WizardPane.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public ExportWizard.Pages getNextPane() {
		return ExportWizard.Pages.DESTINATION;
	}
	/**
	 * Called when the page is activated.
	 */
	public void activate() {
		wizard.enableFinishButton(false);
		wizard.enableNextButton(true);

		textRenderingButton.setSelection(getFilter().isTextRendering());
		htmlRenderingButton.setSelection(getFilter().isHtmlRendering());
		rtfRenderingButton.setSelection(getFilter().isRtfRendering());
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
		label.setText(textBundle.get("AppleWorksWordProcessorFormatPrompt")); //$NON-NLS-1$
		textRenderingButton = new Button(control, SWT.RADIO);
		textRenderingButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		textRenderingButton.setText(textBundle.get("AppleWorksWordProcessorFormatAsText")); //$NON-NLS-1$
		textRenderingButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getFilter().selectTextRendering();
			}
		});
		htmlRenderingButton = new Button(control, SWT.RADIO);
		htmlRenderingButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		htmlRenderingButton.setText(textBundle.get("AppleWorksWordProcessorFormatAsHtml")); //$NON-NLS-1$
		htmlRenderingButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getFilter().selectHtmlRendering();
			}
		});
		rtfRenderingButton = new Button(control, SWT.RADIO);
		rtfRenderingButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		rtfRenderingButton.setText(textBundle.get("AppleWorksWordProcessorFormatAsRtf")); //$NON-NLS-1$
		rtfRenderingButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getFilter().selectRtfRendering();
			}
		});
		return control;
	}
	/**
	 * Dispose of any resources.
	 */
	public void dispose() {
		control.dispose();
		control = null;
	}
	/**
	 * Get the AppleWorks word processor filter.
	 */
	protected AppleWorksWordProcessorFileFilter getFilter() {
		return (AppleWorksWordProcessorFileFilter) wizard.getFileFilter();
	}
}
