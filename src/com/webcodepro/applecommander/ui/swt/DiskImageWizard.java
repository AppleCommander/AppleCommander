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

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DosFormatDisk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.OzDosFormatDisk;
import com.webcodepro.applecommander.storage.PascalFormatDisk;
import com.webcodepro.applecommander.storage.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.RdosFormatDisk;
import com.webcodepro.applecommander.storage.UniDosFormatDisk;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

/**
 * Disk Image Wizard.
 * <br>
 * Created on Dec 15, 2002.
 * @author Rob Greene
 */
public class DiskImageWizard extends Wizard {
	public static final int FORMAT_DOS33 = 1;
	public static final int FORMAT_UNIDOS = 2;
	public static final int FORMAT_PRODOS = 3;
	public static final int FORMAT_PASCAL = 4;
	public static final int FORMAT_RDOS = 5;
	public static final int FORMAT_OZDOS = 6;
	public static final int ORDER_DOS = 1;
	public static final int ORDER_PRODOS = 2;
	private int format = FORMAT_DOS33;
	private int size = FormattedDisk.APPLE_140KB_DISK;
	private String fileName = "";
	private String volumeName = "";
	private int order = ORDER_PRODOS;
	private boolean compressed = false;
	/**
	 * Constructor for ExportWizard.
	 */
	public DiskImageWizard(Shell parent, Image logo) {
		super(parent, logo, "Disk Image Wizard");
	}
	/**
	 * Create the initial display used in the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.Wizard#createInitialWizardPane()
	 */
	public WizardPane createInitialWizardPane() {
		return new DiskImageFormatPane(getContentPane(), this);
	}
	/**
	 * Get the FormattedDisk as specified.
	 */
	public FormattedDisk[] getFormattedDisks() {
		StringBuffer name = new StringBuffer(fileName);
		if (isHardDisk()) {
			name.append(".hdv");
		} else if (order == ORDER_DOS) {
			name.append(".dsk");
		} else {
			name.append(".po");
		}
		if (isCompressed()) {
			name.append(".gz");
		}
		switch (format) {
			case FORMAT_DOS33:
				return DosFormatDisk.create(name.toString());
			case FORMAT_OZDOS:
				return OzDosFormatDisk.create(name.toString());
			case FORMAT_PASCAL:
				return PascalFormatDisk.create(name.toString(), volumeName, size);
			case FORMAT_PRODOS:
				return ProdosFormatDisk.create(name.toString(), volumeName, size);
			case FORMAT_RDOS:
				return RdosFormatDisk.create(name.toString());
			case FORMAT_UNIDOS:
				return UniDosFormatDisk.create(name.toString());
		}
		return null;
	}
	/**
	 * Returns the image format.
	 */
	public int getFormat() {
		return format;
	}
	/**
	 * Sets the image format.
	 */
	public void setFormat(int format) {
		this.format = format;
	}
	/**
	 * Returns the size.
	 */
	public int getSize() {
		return size;
	}
	/**
	 * Sets the size.
	 */
	public void setSize(int size) {
		this.size = size;
	}
	/**
	 * Returns the fileName.
	 */
	public String getFileName() {
		return fileName;
	}
	/**
	 * Returns the volumeName.
	 */
	public String getVolumeName() {
		return volumeName;
	}
	/**
	 * Sets the fileName.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**
	 * Sets the volumeName.
	 */
	public void setVolumeName(String volumeName) {
		this.volumeName = volumeName;
	}
	/**
	 * Indicates if the format is ProDOS.
	 */
	public boolean isFormatProdos() {
		return format == FORMAT_PRODOS;
	}
	/**
	 * Indicates if the format is Pascal.
	 */
	public boolean isFormatPascal() {
		return format == FORMAT_PASCAL;
	}
	/**
	 * Returns the order.
	 */
	public int getOrder() {
		return order;
	}
	/**
	 * Sets the order.
	 */
	public void setOrder(int order) {
		this.order = order;
	}
	/**
	 * Returns the compressed.
	 */
	public boolean isCompressed() {
		return compressed;
	}
	/**
	 * Sets the compressed.
	 */
	public void setCompressed(boolean compressed) {
		this.compressed = compressed;
	}
	/**
	 * Indicates if this image is a hard disk.
	 */
	public boolean isHardDisk() {
		return size > Disk.APPLE_800KB_2IMG_DISK;
	}
}
