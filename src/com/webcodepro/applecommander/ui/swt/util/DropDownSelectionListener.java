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
package com.webcodepro.applecommander.ui.swt.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Listens to widgetSelected() events on SWT.DROP_DOWN type ToolItems
 * and opens/closes a menu when appropriate.
 * Code taken and modified from SWT examples to be more generic.
 * <p>
 * Date created: Nov 2, 2002 8:25:11 PM
 * @author Rob Greene
 */
public class DropDownSelectionListener extends SelectionAdapter {
	private Menu menu = null;
	private boolean visible = false;
	
	/**
	 * Construct the DropDownSelectionListener with the specific menu to be used.
	 */
	public DropDownSelectionListener(Menu menu) {
		this.menu = menu;
		
		MenuItem[] menuItems = menu.getItems();
		for (int i=0; i<menuItems.length; i++) {
			/*
			 * Add a menu selection listener so that the menu is hidden
			 * when the user selects an item from the drop down menu.
			 */
			menuItems[i].addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setMenuVisible(false);
				}
			});
		}
	}

	/**
	 * Handle selection events.
	 */	
	public void widgetSelected(SelectionEvent event) {
		/**
		 * A selection event will be fired when a drop down tool
		 * item is selected in the main area and in the drop
		 * down arrow.  Examine the event detail to determine
		 * where the widget was selected.
		 */		
		if (event.detail == SWT.ARROW) {
			/*
			 * The drop down arrow was selected.
			 */
			if (visible) {
				// Hide the menu to give the Arrow the appearance of being a toggle button.
				setMenuVisible(false);
			} else {	
				// Position the menu below and vertically aligned with the the drop down tool button.
				final ToolItem toolItem = (ToolItem) event.widget;
				final ToolBar  toolBar = toolItem.getParent();
				
				Rectangle toolItemBounds = toolItem.getBounds();
				Point point = toolBar.toDisplay(new Point(toolItemBounds.x, toolItemBounds.y));
				menu.setLocation(point.x, point.y + toolItemBounds.height);
				setMenuVisible(true);
			}
		} else {
			/*
			 * Main area of drop down tool item selected.
			 * An application would invoke the code to perform the action for the tool item.
			 */
		}
	}
	
	/**
	 * Set menu visibility and track state.
	 */
	protected void setMenuVisible(boolean visible) {
		menu.setVisible(visible);
		this.visible = visible;
	}
}
