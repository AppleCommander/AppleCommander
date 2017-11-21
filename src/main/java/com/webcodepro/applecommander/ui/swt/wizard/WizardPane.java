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
package com.webcodepro.applecommander.ui.swt.wizard;

/**
 * Represents a pane of a wizard.
 * <p>
 * Date created: Nov 7, 2002 8:40:44 PM
 * @author Rob Greene
 */
public abstract class WizardPane {
	/**
	 * Constructor for WizardPane.
	 */
	public WizardPane() {
		super();
	}
	/**
	 * Get the next WizardPane.
	 */
	public abstract WizardPane getNextPane();
	/**
	 * Create and display the wizard pane.
	 */
	public abstract void open();
	/**
	 * Dispose of any resources.
	 */
	public abstract void dispose();
}
