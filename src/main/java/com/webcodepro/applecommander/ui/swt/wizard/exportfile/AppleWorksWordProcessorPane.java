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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.webcodepro.applecommander.storage.filters.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Choose format for AppleWorks Word Processor export.
 * <p>
 * Date created: Nov 15, 2002 11:31:15 PM
 * @author Rob Greene
 */
public class AppleWorksWordProcessorPane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
	private Composite parent;
	private Object layoutData;
	private Composite control;
	private ExportWizard wizard;
	/**
	 * Constructor for AppleWorksWordProcessorPane.
	 */
	public AppleWorksWordProcessorPane(Composite parent, ExportWizard exportWizard, Object layoutData) {
		super();
		this.parent = parent;
		this.wizard = exportWizard;
		this.layoutData = layoutData;
	}
	/**
	 * Get the next WizardPane.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return new ExportFileDestinationPane(parent, wizard, layoutData);
	}
	/**
	 * Create and display the wizard pane.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#open()
	 */
	public void open() {
		wizard.enableFinishButton(false);
		wizard.enableNextButton(true);
		control = new Composite(parent, SWT.NULL);
		control.setLayoutData(layoutData);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.justify = true;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		layout.spacing = 3;
		control.setLayout(layout);
		Label label = new Label(control, SWT.WRAP);
		label.setText(textBundle.get("AppleWorksWordProcessorFormatPrompt")); //$NON-NLS-1$
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Button button = new Button(control, SWT.RADIO);
		button.setText(textBundle.get("AppleWorksWordProcessorFormatAsText")); //$NON-NLS-1$
		button.setSelection(getFilter().isTextRendering());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getFilter().selectTextRendering();
			}
		});
		button = new Button(control, SWT.RADIO);
		button.setText(textBundle.get("AppleWorksWordProcessorFormatAsHtml")); //$NON-NLS-1$
		button.setSelection(getFilter().isHtmlRendering());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getFilter().selectHtmlRendering();
			}
		});
		button = new Button(control, SWT.RADIO);
		button.setText(textBundle.get("AppleWorksWordProcessorFormatAsRtf")); //$NON-NLS-1$
		button.setSelection(getFilter().isRtfRendering());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getFilter().selectRtfRendering();
			}
		});
	}
	/**
	 * Dispose of any resources.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#dispose()
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
