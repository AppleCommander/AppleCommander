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
package com.webcodepro.applecommander.ui.swt;

import com.webcodepro.applecommander.storage.GraphicsFileFilter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Choose graphics options for file export.
 * <p>
 * Date created: Nov 7, 2002 10:25:43 PM
 * @author: Rob Greene
 */
public class ExportGraphicsTypePane extends WizardPane {
	private Composite parent;
	private Object layoutData;
	private Composite control;
	private ExportWizard wizard;
	/**
	 * Constructor for ExportGraphicsTypePane.
	 */
	public ExportGraphicsTypePane(Composite parent, ExportWizard exportWizard, Object layoutData) {
		super();
		this.parent = parent;
		this.wizard = exportWizard;
		this.layoutData = layoutData;
	}
	/**
	 * Determine the next wizard pane and return an instance.
	 * @see com.webcodepro.applecommander.gui.WizardPane#getNextPane()
	 */
	public WizardPane getNextPane() {
		return new ExportFileDestinationPane(parent, wizard, layoutData);
	}
	/**
	 * Open up and configure the wizard pane.
	 * @see com.webcodepro.applecommander.gui.WizardPane#open()
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
		label.setText("Please choose the appropriate graphics mode:");
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite graphicsModeGroup = new Composite(control, SWT.NULL);
		graphicsModeGroup.setLayout(subpanelLayout);
		Button button = new Button(graphicsModeGroup, SWT.RADIO);
		button.setText("Hires black and white (280x192)");
		button.setSelection(getGraphicsFilter().isHiresBlackAndWhiteMode());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_HGR_BLACK_AND_WHITE);
			}
		});
		button = new Button(graphicsModeGroup, SWT.RADIO);
		button.setText("Hires color (280x192)");
		button.setSelection(getGraphicsFilter().isHiresColorMode());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_HGR_COLOR);
			}
		});
		button = new Button(graphicsModeGroup, SWT.RADIO);
		button.setText("Double hires black and white (560x384)");
		button.setSelection(getGraphicsFilter().isDoubleHiresBlackAndWhiteMode());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_DHR_BLACK_AND_WHITE);
			}
		});
		button = new Button(graphicsModeGroup, SWT.RADIO);
		button.setText("Double hires color (560x384)");
		button.setSelection(getGraphicsFilter().isDoubleHiresColorMode());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_DHR_COLOR);
			}
		});
		button = new Button(graphicsModeGroup, SWT.RADIO);
		button.setText("Super hires (320x200 or 640x200)");
		button.setSelection(getGraphicsFilter().isSuperHiresMode());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_SHR);
			}
		});
		label = new Label(control, SWT.WRAP);
		label.setText("Please choose the appropriate file format to save image as:");
		Composite graphicsFormatGroup = new Composite(control, SWT.NULL);
		graphicsFormatGroup.setLayout(subpanelLayout);
		String[] formats = getGraphicsFilter().getFileExtensions();
		for (int i=0; i<formats.length; i++) {
			button = new Button(graphicsFormatGroup, SWT.RADIO);
			button.setText(formats[i]);
			button.setSelection(formats[i].equals(getGraphicsFilter().getExtension()));
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Button source = (Button) e.getSource();
					getGraphicsFilter().setExtension(source.getText());
				}
			});
		}
	}
	/**
	 * Dispose of widgets.
	 * @see com.webcodepro.applecommander.gui.WizardPane#dispose()
	 */
	public void dispose() {
		control.dispose();
		control = null;
	}
	/**
	 * Get the graphics file filter.
	 */
	protected GraphicsFileFilter getGraphicsFilter() {
		return (GraphicsFileFilter) wizard.getFileFilter();
	}
}
