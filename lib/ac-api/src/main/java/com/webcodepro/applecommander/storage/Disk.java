/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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
package com.webcodepro.applecommander.storage;

import com.webcodepro.applecommander.storage.physical.*;
import com.webcodepro.applecommander.util.TextBundle;
import org.applecommander.image.UniversalDiskImage;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Abstract representation of an Apple2 disk (floppy, 800k, hard disk).
 * <p>
 * Date created: Oct 3, 2002 10:59:47 PM
 * @author Rob Greene
 */
public class Disk {
    public static final int BLOCK_SIZE = 512;
	public static final int SECTOR_SIZE = 256;
	public static final int PRODOS_BLOCKS_ON_140KB_DISK = 280;
	public static final int DOS33_SECTORS_ON_140KB_DISK = 560;
	public static final int APPLE_140KB_DISK = 143360;
	public static final int APPLE_140KB_NIBBLE_DISK = 232960;
    public static final int APPLE_400KB_DISK = 409600;
	public static final int APPLE_800KB_DISK = 819200;
	public static final int APPLE_800KB_2IMG_DISK =
		APPLE_800KB_DISK + UniversalDiskImage.HEADER_SIZE;
	public static final int APPLE_5MB_HARDDISK = 5242880;
	public static final int APPLE_10MB_HARDDISK = 10485760;
	public static final int APPLE_20MB_HARDDISK = 20971520;
	public static final int APPLE_32MB_HARDDISK = 33553920;	// short one block!

    private static final TextBundle textBundle = StorageBundle.getInstance();

	private String filename;
	private boolean newImage = false;
	private Source source;
	private ImageOrder imageOrder = null;

    /**
	 * Construct a Disk with the given byte array.
	 */
	protected Disk(String filename, ImageOrder imageOrder) {
		this.imageOrder = imageOrder;
		this.filename = filename;
		this.newImage = true;
	}

	/**
	 * Save a Disk image to its file.
	 */
	public void save() throws IOException {
		File file = new File(getFilename());
		if (!file.exists()) {
			file.createNewFile();
		}
		OutputStream output = new FileOutputStream(file);
		if (getFilename().toLowerCase().endsWith(".gz")) {
			output = new GZIPOutputStream(output);
		}
		DataBuffer data = getSource().readAllBytes();
		byte[] fileData = new byte[data.limit()];
		data.read(fileData);
		output.write(fileData);
		output.close();
		getSource().clearChanges();
		newImage = false;
	}

	/**
	 * Save a Disk image as a new/different file.
	 */
	public void saveAs(String filename) throws IOException {
		this.filename = filename;
		save();
	}

	/**
	 * Returns the source.
	 */
	public Source getSource() {
		if (imageOrder != null) {
			return imageOrder.getSource();
		}
		return source;
	}

	/**
	 * Returns the filename.
	 * @return String
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * Sets the filename.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Returns the name of the underlying image order.
	 * @return String
	 */
	public String getOrderName() {
		return (imageOrder == null) ? textBundle.get("FormattedDisk.Unknown") : imageOrder.getName(); 
	}

	/**
	 * Identify the size of this disk.
	 */
	public int getPhysicalSize() {
		if (getImageOrder() instanceof WozOrder) {
			// Total hack since WOZ is currently a special case.
			return getImageOrder().getPhysicalSize();
		}
		if (getSource() != null) {
			return getSource().getSize();
		}
		return getImageOrder().getPhysicalSize();
	}
	
	/**
	 * Resize a disk image up to a larger size.  The primary intention is to
	 * "fix" disk images that have been created too small.  The primary culprit
	 * is ApplePC HDV images which dynamically grow.  Since AppleCommander
	 * works with a byte array, the image must grow to its full size.
	 * @param newSize
	 */
	protected void resizeDiskImage(int newSize) {
		if (newSize < getPhysicalSize()) {
			throw new IllegalArgumentException(
				textBundle.get("Disk.ResizeDiskError")); //$NON-NLS-1$
		}
		DataBuffer backingBuffer = imageOrder.getSource().get(DataBuffer.class).orElseThrow();
		backingBuffer.limit(newSize);
	}
	
	/**
	 * Read the block from the disk image.
	 */
	public byte[] readBlock(int block) {
		return imageOrder.readBlock(block);
	}
	
	/**
	 * Write the block to the disk image.
	 */
	public void writeBlock(int block, byte[] data) {
		imageOrder.writeBlock(block, data);
	}

	/**
	 * Retrieve the specified sector.
	 */
	public byte[] readSector(int track, int sector) throws IllegalArgumentException {
		return imageOrder.readSector(track, sector);
	}
	
	/**
	 * Write the specified sector.
	 */
	public void writeSector(int track, int sector, byte[] bytes) 
			throws IllegalArgumentException {
		imageOrder.writeSector(track, sector, bytes);
	}

	/**
	 * Indicates if the disk has changed. Triggered when data is
	 * written and cleared when data is saved.
	 */
	public boolean hasChanged() {
		return getSource().hasChanged();
	}
	
	/**
	 * Indicates if the disk image is new.  This can be used
	 * for Save As processing.
	 */
	public boolean isNewImage() {
		return newImage;
	}
	
	/**
	 * Answer with the physical ordering of the disk.
	 */
	public ImageOrder getImageOrder() {
		return imageOrder;
	}
	
	/**
	 * Set the physical ordering of the disk.
	 */
	protected void setImageOrder(ImageOrder imageOrder) {
		this.imageOrder = imageOrder;
	}
}
