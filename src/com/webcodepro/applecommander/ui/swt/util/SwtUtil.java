/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2003 by Robert Greene
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
package com.webcodepro.applecommander.ui.swt.util;

import org.eclipse.swt.widgets.Shell;

/**
 * SWT-related utility code.
 * <p>
 * Date Created: Mar 5, 2003
 * @author Rob Greene
 */
public class SwtUtil {
	/**
	 * Center the child shell within the parent shell window.
	 */
	public static void center(Shell parent, Shell child) {
		int x = parent.getLocation().x + 
			(parent.getSize().x - child.getSize().x) / 2;
		int y = parent.getLocation().y +
			(parent.getSize().y - child.getSize().y) / 2;
		child.setLocation(x,y);
	}
}
