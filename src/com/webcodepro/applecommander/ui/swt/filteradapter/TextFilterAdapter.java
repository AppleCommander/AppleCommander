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
package com.webcodepro.applecommander.ui.swt.filteradapter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.webcodepro.applecommander.ui.swt.FileViewerWindow;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.StyledTextAdapter;

/**
 * Provides a view of a simple text file.
 * 
 * @author Rob Greene
 */
public class TextFilterAdapter extends FilterAdapter {
	private String textContent;
	
	public TextFilterAdapter(FileViewerWindow window, String text, String toolTipText, Image image) {
		super(window, text, toolTipText, image);
	}
	
	public void display() {
		if (textContent == null) {
			textContent = createTextContent();
		}
		getCopyToolItem().setEnabled(true);
		createTextWidget(textContent);
	}
	
	protected String createTextContent() {
		return new String(getFileFilter().filter(getFileEntry()));
	}

	protected void createTextWidget(String textContents) {
		StyledText styledText = new StyledText(getComposite(), SWT.NONE);
		styledText.setText(textContents);
		styledText.setFont(getCourierFont());
		styledText.setEditable(false);
		//styledText.setWordWrap(true);		// seems to throw size out-of-whack
		Point size = styledText.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		getComposite().setContent(styledText);
		getComposite().setExpandHorizontal(true);
		getComposite().setExpandVertical(true);
		getComposite().setMinWidth(size.x);
		getComposite().setMinHeight(size.y);
		getComposite().getContent().addListener(SWT.KeyUp, getToolbarCommandHandler());

		getToolItem().setSelection(true);		
		setContentTypeAdapter(new StyledTextAdapter(styledText, getFileEntry().getFilename()));
	}
}