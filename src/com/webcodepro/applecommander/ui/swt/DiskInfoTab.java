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

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;

/**
 * Build the Disk Info tab for the Disk Window.
 * <p>
 * Date created: Nov 17, 2002 9:15:29 PM
 * @author: Rob Greene
 */
public class DiskInfoTab {
	/**
	 * Create the DISK INFO tab.
	 */
	public DiskInfoTab(CTabFolder tabFolder, FormattedDisk disk) {
		CTabItem ctabitem = new CTabItem(tabFolder, SWT.NULL);
		if (disk.getLogicalDiskNumber() > 0) {
			ctabitem.setText("Disk Info #" + disk.getLogicalDiskNumber());
		} else {
			ctabitem.setText("Disk Info");
		}
		
		Table table = new Table(tabFolder, SWT.FULL_SELECTION);
		ctabitem.setControl(table);
		table.setHeaderVisible(true);
		TableColumn column = new TableColumn(table, SWT.LEFT);
		column.setResizable(true);
		column.setText("Label");
		column.setWidth(200);
		column = new TableColumn(table, SWT.LEFT);
		column.setResizable(true);
		column.setText("Value");
		column.setWidth(400);
		
		Iterator iterator = disk.getDiskInformation().iterator();
		TableItem item = null;
		while (iterator.hasNext()) {
			DiskInformation diskinfo = (DiskInformation) iterator.next();
			item = new TableItem(table, SWT.NULL);
			item.setText(new String[] { diskinfo.getLabel(), diskinfo.getValue() });
		}
		
	}
	/**
	 * Dispose of resources.
	 */
	public void dispose() {
	}
}
