/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2004 by Robert Greene
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
package com.webcodepro.applecommander.ui.swt.util.contentadapter;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextPrintOptions;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.printing.Printer;

import com.webcodepro.applecommander.ui.swt.util.SwtUtil;

/**
 * Content-specific adapter for StyledText.
 * 
 * @author Rob Greene
 */
public class StyledTextAdapter implements ContentTypeAdapter {
	private StyledText styledText;
	private String printJobName;
	
	public StyledTextAdapter(StyledText styledText, String printJobName) {
		this.styledText = styledText;
		this.printJobName = printJobName;
	}
	
	public void print() {
		final Printer printer = SwtUtil.showPrintDialog(styledText);
		if (printer == null) return;	// Print was cancelled
		StyledTextPrintOptions options = new StyledTextPrintOptions();
		options.jobName = printJobName;
		options.printLineBackground = true;
		options.printTextBackground = true;
		options.printTextFontStyle = true;
		options.printTextForeground = true;
		options.footer = "\t<page>"; //$NON-NLS-1$ (for StyledText widget!)
		options.header = "\t" + printJobName; //$NON-NLS-1$
		 
		final Runnable runnable = styledText.print(printer, options);
		new Thread(new Runnable() {
			public void run() {
				runnable.run();
				printer.dispose();
			}
		}).start();
	}
	
	public void selectAll() {
		styledText.selectAll();
	}
	
	public void copy() {
		// If there is no selection, copy everything
		if (styledText.getSelectionCount() == 0) {
			Point selection = styledText.getSelection();
			styledText.selectAll();
			styledText.copy();
			styledText.setSelection(selection);
		} else {	// copy current selection
			styledText.copy();
		}
	}
}