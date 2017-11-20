/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2008 by David Schmidt
 * david__schmidt at users.sourceforge.net
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
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.webcodepro.applecommander.ui.swt.FileViewerWindow;
import com.webcodepro.applecommander.ui.swt.util.contentadapter.StyledTextAdapter;
import com.webcodepro.applecommander.util.BusinessBASICToken;
import com.webcodepro.applecommander.util.BusinessBASICTokenizer;

/**
 * Provides a view of a syntax-colored Apple /// Business BASIC program listing.
 * 
 * @author David Schmidt
 */
public class BusinessBASICFilterAdapter extends FilterAdapter {
	private StyledText styledText;
	
	public BusinessBASICFilterAdapter(FileViewerWindow window, String text, String toolTipText, Image image) {
		super(window, text, toolTipText, image);
	}

	public void display() {
		if (styledText == null) {
			createStyledText();
		}
		Point size = styledText.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		getComposite().setContent(styledText);
		getComposite().setExpandHorizontal(true);
		getComposite().setExpandVertical(true);
		getComposite().setMinWidth(size.x);
		getComposite().setMinHeight(size.y);
		getComposite().getContent().addListener(SWT.KeyUp, getToolbarCommandHandler());
			
		setContentTypeAdapter(new StyledTextAdapter(styledText, getFileEntry().getFilename()));
	}
	
	public void dispose() {
		styledText.dispose();
	}


	protected void createStyledText() {
		styledText = new StyledText(getComposite(), SWT.NONE);
		styledText.setForeground(getBlackColor());
		styledText.setFont(getCourierFont());
		styledText.setEditable(false);

		BusinessBASICTokenizer tokenizer = new BusinessBASICTokenizer(getFileEntry());
		boolean firstLine = true;
		boolean firstData = true;
		int nestLevels = 0;
		while (tokenizer.hasMoreTokens()) {
			BusinessBASICToken token = tokenizer.getNextToken();
			if (token == null) {
				continue;	// should be end of program...
			} else if (token.isLineNumber()) {
				if (firstLine) {
					firstLine = false;
				} else {
					styledText.append("\n"); //$NON-NLS-1$
				}
				firstData = true;
				styledText.append(Integer.toString(token.getLineNumber()));
				styledText.append("   "); //$NON-NLS-1$
				if (nestLevels > 0) {
					for (int i = 0; i < nestLevels; i++)
						styledText.append("  "); //$NON-NLS-1$
					}
			} else if (token.isCommandSeparator() || token.isExpressionSeparator()) {
				styledText.append(token.getStringValue());
				firstData = true;
			} else if (token.isEndOfCommand()) {
				styledText.append("\n"); //$NON-NLS-1$
				firstData = false;
			} else if (token.isString()) {
				if (!firstData)
					styledText.append(" "); //$NON-NLS-1$
				int caretOffset = styledText.getCharCount();
				styledText.append(token.getStringValue().trim());
				StyleRange styleRange = new StyleRange();
				styleRange.start = caretOffset;
				styleRange.length = token.getStringValue().trim().length();
				styleRange.foreground = getGreenColor();
				styledText.setStyleRange(styleRange);
				firstData = false;
			} else if (token.isToken()) {
				if (!firstData)
					styledText.append(" "); //$NON-NLS-1$
				int caretOffset = styledText.getCharCount();
				styledText.append(token.getTokenString());
				StyleRange styleRange = new StyleRange();
				styleRange.start = caretOffset;
				styleRange.length = token.getTokenString().length();
				//styleRange.fontStyle = SWT.BOLD;
				styleRange.foreground = getBlueColor();
				styledText.setStyleRange(styleRange);
				firstData = false;
				if (token.isIndenter()) {
					nestLevels ++; }
				else if (token.isOutdenter()) {
					nestLevels --; }
			}
		}
	}
}