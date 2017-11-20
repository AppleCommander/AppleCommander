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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.FormattedDisk.DiskInformation;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.util.TextBundle;

/**
 * Build the Disk Info tab for the Disk Window.
 * <p>
 * Date created: Nov 17, 2002 9:15:29 PM
 * @author Rob Greene
 */
public class DiskInfoTab {
	private TextBundle textBundle = UiBundle.getInstance();
	private Table infoTable;
	private Composite composite;
	private FormattedDisk[] formattedDisks;
	/**
	 * Create the DISK INFO tab.
	 */
	public DiskInfoTab(CTabFolder tabFolder, FormattedDisk[] disks) {
		this.formattedDisks = disks;
		
		CTabItem ctabitem = new CTabItem(tabFolder, SWT.NULL);
		ctabitem.setText(textBundle.get("DiskInfoTab.Title")); //$NON-NLS-1$
		
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				getInfoTable().removeAll();
				buildDiskInfoTable(getFormattedDisk(0));	// FIXME!
			}
		});
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(
			tabFolder, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		ctabitem.setControl(scrolledComposite);
		
		composite = new Composite(scrolledComposite, SWT.NONE);
		createDiskInfoTable();
		if (disks.length > 1) {
			RowLayout layout = new RowLayout(SWT.VERTICAL);
			layout.wrap = false;
			composite.setLayout(layout);
			for (int i=0; i<disks.length; i++) {
				Label label = new Label(composite, SWT.NULL);
				label.setText(disks[i].getDiskName());
				buildDiskInfoTable(disks[i]);
			}
		} else {
			composite.setLayout(new FillLayout());
			buildDiskInfoTable(disks[0]);
		}
		composite.pack();
		scrolledComposite.setContent(composite);
		scrolledComposite.setMinSize(
			composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	/**
	 * Create the table describing the given disk.
	 */
	public void createDiskInfoTable() {
		infoTable = new Table(composite, SWT.FULL_SELECTION);
		infoTable.setHeaderVisible(true);
		TableColumn column = new TableColumn(infoTable, SWT.LEFT);
		column.setResizable(true);
		column.setText(textBundle.get("DiskInfoTab.LabelHeader")); //$NON-NLS-1$
		column.setWidth(200);
		column = new TableColumn(infoTable, SWT.LEFT);
		column.setResizable(true);
		column.setText(textBundle.get("DiskInfoTab.ValueHeader")); //$NON-NLS-1$
		column.setWidth(400);
	}
	/**
	 * Build the table describing the given disk.
	 */
	public void buildDiskInfoTable(FormattedDisk disk) {
		Iterator iterator = disk.getDiskInformation().iterator();
		TableItem item = null;
		while (iterator.hasNext()) {
			DiskInformation diskinfo = (DiskInformation) iterator.next();
			item = new TableItem(infoTable, SWT.NULL);
			item.setText(new String[] { diskinfo.getLabel(), diskinfo.getValue() });
		}
	}
	/**
	 * Dispose of resources.
	 */
	public void dispose() {
		infoTable.dispose();
		composite.dispose();
	}
	protected Table getInfoTable() {
		return infoTable;
	}
	protected FormattedDisk getFormattedDisk(int diskNumber) {
		return formattedDisks[diskNumber];
	}
}
