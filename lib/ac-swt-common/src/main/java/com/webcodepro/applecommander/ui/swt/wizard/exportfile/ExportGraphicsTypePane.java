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

import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;
import com.webcodepro.applecommander.util.TextBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import java.util.HashMap;
import java.util.Map;

/**
 * Choose graphics options for file export.
 * <p>
 * Date created: Nov 7, 2002 10:25:43 PM
 * @author Rob Greene
 */
public class ExportGraphicsTypePane extends WizardPane<ExportWizard.Pages> {
	private final TextBundle textBundle = UiBundle.getInstance();
	private final Composite parent;
	private Composite control;
	private final ExportWizard wizard;
	private Button hiresBlackAndWhiteModeButton;
	private Button hiresColorModeButton;
	private Button doubleHiresBlackAndWhiteModeButton;
	private Button doubleHiresColorModeButton;
	private Button superHires16ModeButton;
	private Button superHires3200ModeButton;
	private Button quickDraw2IconButton;
	private final Map<String,Button> byFileExtensionButtons = new HashMap<>();
	/**
	 * Constructor for ExportGraphicsTypePane.
	 */
	public ExportGraphicsTypePane(Composite parent, ExportWizard exportWizard) {
		super();
		this.parent = parent;
		this.wizard = exportWizard;
	}
	/**
	 * Determine the next wizard pane and return an instance.
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
		// Toggle buttons
		hiresBlackAndWhiteModeButton.setSelection(getGraphicsFilter().isHiresBlackAndWhiteMode());
		hiresColorModeButton.setSelection(getGraphicsFilter().isHiresColorMode());
		doubleHiresBlackAndWhiteModeButton.setSelection(getGraphicsFilter().isDoubleHiresBlackAndWhiteMode());
		doubleHiresColorModeButton.setSelection(getGraphicsFilter().isDoubleHiresColorMode());
		superHires16ModeButton.setSelection(getGraphicsFilter().isSuperHires16Mode());
		superHires3200ModeButton.setSelection(getGraphicsFilter().isSuperHires3200Mode());
		quickDraw2IconButton.setSelection(getGraphicsFilter().isQuickDraw2Icon());
		//
		byFileExtensionButtons.forEach((ext, button) -> {
			button.setSelection(ext.equals(getGraphicsFilter().getExtension()));
		});
	}
	/**
	 * Open up and configure the wizard pane.
	 */
	public Control create() {
		control = new Composite(parent, SWT.NULL);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.justify = true;
		layout.marginBottom = 5;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginTop = 5;
		layout.spacing = 3;
		control.setLayout(layout);
		Label label = new Label(control, SWT.WRAP);
		label.setText(textBundle.get("ExportGraphicsTypePrompt")); //$NON-NLS-1$
		RowLayout subpanelLayout = new RowLayout(SWT.VERTICAL);
		subpanelLayout.justify = true;
		subpanelLayout.spacing = 3;
		Composite graphicsModeGroup = new Composite(control, SWT.NULL);
		graphicsModeGroup.setLayout(subpanelLayout);
		hiresBlackAndWhiteModeButton = new Button(graphicsModeGroup, SWT.RADIO);
		hiresBlackAndWhiteModeButton.setText(textBundle.get("ExportGraphicsTypeHiresBlackAndWhite")); //$NON-NLS-1$
		hiresBlackAndWhiteModeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_HGR_BLACK_AND_WHITE);
			}
		});
		hiresColorModeButton = new Button(graphicsModeGroup, SWT.RADIO);
		hiresColorModeButton.setText(textBundle.get("ExportGraphicsTypeHiresColor")); //$NON-NLS-1$
		hiresColorModeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_HGR_COLOR);
			}
		});
		doubleHiresBlackAndWhiteModeButton = new Button(graphicsModeGroup, SWT.RADIO);
		doubleHiresBlackAndWhiteModeButton.setText(textBundle.get("ExportGraphicsTypeDoubleHiresBlackAndWhite")); //$NON-NLS-1$
		doubleHiresBlackAndWhiteModeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_DHR_BLACK_AND_WHITE);
			}
		});
		doubleHiresColorModeButton = new Button(graphicsModeGroup, SWT.RADIO);
		doubleHiresColorModeButton.setText(textBundle.get("ExportGraphicsTypeDoubleHiresColor")); //$NON-NLS-1$
		doubleHiresColorModeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_DHR_COLOR);
			}
		});
		superHires16ModeButton = new Button(graphicsModeGroup, SWT.RADIO);
		superHires16ModeButton.setText(textBundle.get("ExportGraphicsTypeSuperHiresColor")); //$NON-NLS-1$
		superHires16ModeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_SHR_16);
			}
		});
		superHires3200ModeButton = new Button(graphicsModeGroup, SWT.RADIO);
		superHires3200ModeButton.setText(textBundle.get("ExportGraphicsTypeSuperHires3200Color")); //$NON-NLS-1$
		superHires3200ModeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_SHR_3200);
			}
		});
		quickDraw2IconButton = new Button(graphicsModeGroup, SWT.RADIO);
		quickDraw2IconButton.setText(textBundle.get("ExportGraphicsTypeQuickDraw2Icon")); //$NON-NLS-1$
		quickDraw2IconButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getGraphicsFilter().setMode(GraphicsFileFilter.MODE_QUICKDRAW2_ICON);
			}
		});
		label = new Label(control, SWT.WRAP);
		label.setText(textBundle.get("ExportGraphicsFileFormatPrompt")); //$NON-NLS-1$
		Composite graphicsFormatGroup = new Composite(control, SWT.NULL);
		graphicsFormatGroup.setLayout(subpanelLayout);
		String[] formats = GraphicsFileFilter.getFileExtensions();
		for (int i=0; i<formats.length; i++) {
			Button button = new Button(graphicsFormatGroup, SWT.RADIO);
			button.setText(formats[i]);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Button source = (Button) e.getSource();
					getGraphicsFilter().setExtension(source.getText());
				}
			});
			byFileExtensionButtons.put(formats[i], button);
		}
		return control;
	}
	/**
	 * Dispose of widgets.
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
