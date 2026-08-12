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
package com.webcodepro.applecommander.ui.swt.wizard.comparedisks;

import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Provides the wizard pane which gets the disks to compare.
 * <p>
 * @author Rob Greene
 */
public class CompareDisksStartPane extends WizardPane<CompareDisksWizard.Pages> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final Composite parent;
	private Composite control;
	private final CompareDisksWizard wizard;
	private Text diskname1Text;
	private Text diskname2Text;
	private Combo comparisonStrategyCombo;
	private Text limitText;
	/**
	 * Constructor for CompareDisksStartPane.
	 */
	public CompareDisksStartPane(Composite parent, CompareDisksWizard wizard) {
		super();
		this.parent = parent;
		this.wizard = wizard;
	}
	/**
	 * Open up and configure the wizard pane.
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
		label.setText(textBundle.get("CompareDisksStartPane.Description"));
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL));

		label = new Label(control, SWT.WRAP);
		label.setText("");	// just a vertical spacer

		label = new Label(control, SWT.WRAP);
		label.setText(getDiskLabel(1));
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL));

		diskname1Text = new Text(control, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		float fontHeight = diskname1Text.getFont().getFontData()[0].height;
		GridData disknameGridData = new GridData(GridData.FILL_BOTH);
		disknameGridData.heightHint = (int)fontHeight * 4;
		diskname1Text.setLayoutData(disknameGridData);
		diskname1Text.addModifyListener(event -> {
            Text text = (Text) event.getSource();
            getWizard().setDiskname1(text.getText());
        });
		
		Button button = new Button(control, SWT.PUSH);
		button.setText(textBundle.get("BrowseButton"));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getControl().getShell());
				fileDialog.setFilterPath(getDiskname1Text().getText());
				fileDialog.setText(getDiskLabel(1));
				String filename = fileDialog.open();
				if (filename != null) {
					getDiskname1Text().setText(filename);
				}
			}
		});
	
		label = new Label(control, SWT.WRAP);
		label.setText(getDiskLabel(2));

		diskname2Text = new Text(control, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		diskname2Text.setLayoutData(disknameGridData);
		diskname2Text.addModifyListener(event -> {
            Text text = (Text) event.getSource();
            getWizard().setDiskname2(text.getText());
        });
		
		button = new Button(control, SWT.PUSH);
		button.setText(textBundle.get("BrowseButton"));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getControl().getShell());
				fileDialog.setFilterPath(getDiskname2Text().getText());
				fileDialog.setText(getDiskLabel(2));
				String filename = fileDialog.open();
				if (filename != null) {
					getDiskname2Text().setText(filename);
				}
			}
		});
		
		label = new Label(control, SWT.WRAP);
		label.setText("Select comparison type:");
		
		comparisonStrategyCombo = new Combo(control, SWT.BORDER | SWT.READ_ONLY);
		comparisonStrategyCombo.setItems("Compare by native geometry",
		               "Compare by track/sector geometry",
		               "Compare by block geometry",
		               "Compare by filename");
		comparisonStrategyCombo.select(getWizard().getComparisonStrategy());
		comparisonStrategyCombo.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(SelectionEvent e) {
		        getWizard().setComparisonStrategy(comparisonStrategyCombo.getSelectionIndex());
		    }
		});
        
        label = new Label(control, SWT.WRAP);
        label.setText("Set limit on messages displayed:");
        
        limitText = new Text(control, SWT.WRAP | SWT.BORDER);
        limitText.setText(Integer.toString(wizard.getMessageLimit()));
		GridData limitGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		limitGridData.widthHint = 100;
        limitText.setLayoutData(limitGridData);
        limitText.addModifyListener(this::limitTextModifyListener);
        
        parent.pack();
		return control;
	}
	/**
	 * Get the next pane. A null return indicates the end of the wizard.
	 */
	public CompareDisksWizard.Pages getNextPane() {
		return CompareDisksWizard.Pages.RESULT_PANE;
	}
	/**
	 * Called when the page is activated.
	 */
	public void activate() {
		wizard.enableNextButton(true);
		wizard.enableFinishButton(false);
		diskname1Text.setFocus();

		if (wizard.getDiskname1() != null) diskname1Text.setText(wizard.getDiskname1());
		if (wizard.getDiskname2() != null) diskname2Text.setText(wizard.getDiskname2());
	}
	protected Composite getControl() {
		return control;
	}
	protected Text getDiskname1Text() {
		return diskname1Text;
	}
	protected Text getDiskname2Text() {
		return diskname2Text;
	}
	protected CompareDisksWizard getWizard() {
		return wizard;
	}
	protected String getDiskLabel(int diskNumber) {
		return textBundle.format("CompareDisksStartPane.DiskNLabel",
				diskNumber);
	}
	
	protected void limitTextModifyListener(ModifyEvent event) {
	    try {
	        getWizard().setMessageLimit(Integer.parseInt(limitText.getText()));
	    } catch (NumberFormatException e) {
	        limitText.setText(Integer.toString(getWizard().getMessageLimit()));
	    }
	}
}
