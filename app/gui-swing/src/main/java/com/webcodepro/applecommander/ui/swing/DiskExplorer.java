/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2008-2022 by Robert Greene
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
package com.webcodepro.applecommander.ui.swing;

import java.awt.Color;

import javax.swing.JPanel;

public class DiskExplorer extends JPanel {

	public DiskExplorer() {
		this.setBackground(randomColor());
	}

	private Color randomColor() {
		Color colors[] = {Color.black, Color.blue, Color.cyan,
			Color.gray, Color.darkGray, Color.green,
			Color.lightGray, Color.magenta, Color.orange,
			Color.pink,Color.red, Color.white, Color.yellow};
		return colors[(int)(Math.random() * colors.length)];
	}
	/**
	 * serialVersionUID, to keep Eclipse happy
	 */
	private static final long serialVersionUID = 4981722122357764174L;

}
