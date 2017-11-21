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

import org.eclipse.swt.graphics.Image;

import com.webcodepro.applecommander.storage.filters.HexDumpFileFilter;
import com.webcodepro.applecommander.ui.swt.FileViewerWindow;

/**
 * Provides a view of a hex dump of the program as seen when loaded from the disk.
 * 
 * @author Rob Greene
 */
public class HexFilterAdapter extends TextFilterAdapter {
	public HexFilterAdapter(FileViewerWindow window, String text, String toolTipText, Image image) {
		super(window, text, toolTipText, image);
	}

	protected String createTextContent() {
		return new String(new HexDumpFileFilter().filter(getFileEntry()));
	}
}
