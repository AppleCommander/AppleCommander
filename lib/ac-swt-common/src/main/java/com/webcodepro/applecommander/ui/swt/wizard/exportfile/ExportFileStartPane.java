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

import com.webcodepro.applecommander.storage.filters.AppleWorksDataBaseFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksSpreadSheetFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.storage.filters.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.filters.AssemblySourceFileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.BusinessBASICFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.HexDumpFileFilter;
import com.webcodepro.applecommander.storage.filters.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.filters.PascalTextFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Provides the wizard pane which gets the export filter.
 * <p>
 * Date created: Nov 7, 2002 8:43:27 PM
 * @author Rob Greene
 */
public class ExportFileStartPane extends WizardPane {
	private TextBundle textBundle = UiBundle.getInstance();
	private Composite parent;
	private Object layoutData;
	private Composite control;
	private ExportWizard wizard;
	/**
	 * Constructor for ExportFileStartPane.
	 */
	public ExportFileStartPane(Composite parent, ExportWizard exportWizard, Object layoutData) {
		super();
		this.parent = parent;
		this.wizard = exportWizard;
		this.layoutData = layoutData;
	}
	/**
	 * Open up and configure the wizard pane.
	 */
	public void open() {
		control = new Composite(parent, SWT.NULL);
		control.setLayoutData(layoutData);
		wizard.enableNextButton(true);
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
		label.setText(textBundle.get("ExportFileTypePrompt")); //$NON-NLS-1$
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite buttonSubpanel = new Composite(control, SWT.NULL);
		buttonSubpanel.setLayout(subpanelLayout);
		Button button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsRawDiskData")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() == null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(null);
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsBinaryFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof BinaryFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new BinaryFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsHexDump")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof HexDumpFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new HexDumpFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsAsciiTextFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof TextFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new TextFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsFormattedAssemblyTextFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof AssemblySourceFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new AssemblySourceFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsPascalTextFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof PascalTextFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new PascalTextFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsApplesoftBasicFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof ApplesoftFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new ApplesoftFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsIntegerBasicFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof IntegerBasicFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new IntegerBasicFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsBusinessBASICFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof BusinessBASICFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new BusinessBASICFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsAppleworksWordProcessorFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof AppleWorksWordProcessorFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new AppleWorksWordProcessorFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsAppleworksDatabaseFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof AppleWorksDataBaseFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new AppleWorksDataBaseFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsAppleworksSpreadsheetFile")); //$NON-NLS-1$
		button.setSelection(wizard.getFileFilter() instanceof AppleWorksSpreadSheetFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new AppleWorksSpreadSheetFileFilter());
			}
		});
		button = new Button(buttonSubpanel, SWT.RADIO);
		button.setText(textBundle.get("ExportFileAsGraphicsFile")); //$NON-NLS-1$
		button.setEnabled(GraphicsFileFilter.isCodecAvailable());
		button.setSelection(wizard.getFileFilter() instanceof GraphicsFileFilter);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().setFileFilter(new GraphicsFileFilter());
			}
		});
	}
	/**
	 * Get the next pane. A null return indicates the end of the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		if (wizard.getFileFilter() instanceof GraphicsFileFilter) {
			return new ExportGraphicsTypePane(parent, wizard, layoutData);
		} else if (wizard.getFileFilter() instanceof AppleWorksWordProcessorFileFilter) {
			return new AppleWorksWordProcessorPane(parent, wizard, layoutData);
		}
		return new ExportFileDestinationPane(parent, wizard, layoutData);
	}
	/**
	 * Dispose of resources.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.WizardPane#dispose()
	 */
	public void dispose() {
		control.dispose();
		control = null;
	}
	
	protected ExportWizard getWizard() {
		return wizard;
	}
}
