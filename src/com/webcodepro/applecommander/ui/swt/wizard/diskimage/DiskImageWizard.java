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
package com.webcodepro.applecommander.ui.swt.wizard.diskimage;

import org.eclipse.swt.widgets.Shell;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.cpm.CpmFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.OzDosFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.UniDosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.NibbleOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import com.webcodepro.applecommander.ui.UiBundle;
import com.webcodepro.applecommander.ui.swt.util.ImageManager;
import com.webcodepro.applecommander.ui.swt.wizard.Wizard;
import com.webcodepro.applecommander.ui.swt.wizard.WizardPane;

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
	public static final int FORMAT_CPM = 7;
	public static final int ORDER_DOS = 1;
	public static final int ORDER_PRODOS = 2;
	public static final int ORDER_NIBBLE = 3;
	private int format = FORMAT_DOS33;
	private int size = Disk.APPLE_140KB_DISK;
	private String fileName = new String();
	private String volumeName = new String();
	private int order = ORDER_PRODOS;
	private boolean compressed = false;
	/**
	 * Constructor for ExportWizard.
	 */
	public DiskImageWizard(Shell parent, ImageManager imageManager) {
		super(parent, imageManager.get(ImageManager.LOGO_DISK_IMAGE_WIZARD), 
				UiBundle.getInstance().get("DiskImageWizardTitle")); //$NON-NLS-1$
	}
	/**
	 * Create the initial display used in the wizard.
	 * @see com.webcodepro.applecommander.ui.swt.wizard.Wizard#createInitialWizardPane()
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
			name.append(".hdv"); //$NON-NLS-1$
		} else if (order == ORDER_DOS) {
			name.append(".dsk"); //$NON-NLS-1$
		} else {
			name.append(".po"); //$NON-NLS-1$
		}
		if (isCompressed()) {
			name.append(".gz"); //$NON-NLS-1$
		}
		ByteArrayImageLayout imageLayout = new ByteArrayImageLayout(getSize());
		ImageOrder imageOrder = null;
		switch (getOrder()) {
			case ORDER_DOS:
				imageOrder = new DosOrder(imageLayout);
				break;
			case ORDER_NIBBLE:
				imageOrder = new NibbleOrder(imageLayout);
				break;
			case ORDER_PRODOS:
				imageOrder = new ProdosOrder(imageLayout);
				break;
		}
		switch (format) {
			case FORMAT_DOS33:
				return DosFormatDisk.create(name.toString(), imageOrder);
			case FORMAT_OZDOS:
				return OzDosFormatDisk.create(name.toString(), imageOrder);
			case FORMAT_PASCAL:
				return PascalFormatDisk.create(name.toString(), volumeName, imageOrder);
			case FORMAT_PRODOS:
				return ProdosFormatDisk.create(name.toString(), volumeName, imageOrder);
			case FORMAT_RDOS:
				return RdosFormatDisk.create(name.toString(), imageOrder);
			case FORMAT_UNIDOS:
				return UniDosFormatDisk.create(name.toString(), imageOrder);
			case FORMAT_CPM:
				return CpmFormatDisk.create(name.toString(), imageOrder);
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
