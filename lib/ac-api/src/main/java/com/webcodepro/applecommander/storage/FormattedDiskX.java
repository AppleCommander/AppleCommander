/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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

import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.WozOrder;
import com.webcodepro.applecommander.util.TextBundle;
import org.applecommander.util.DataBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a transitional component while the various file systems get refitted with devices.
 * It contains the ImageOrder related functions that will be replaced.
 */
public abstract class FormattedDiskX extends FormattedDisk {
    private final TextBundle textBundle = StorageBundle.getInstance();

    private ImageOrder imageOrder = null;

    public FormattedDiskX(String filename, ImageOrder imageOrder) {
        super(filename, imageOrder.getSource());
        this.imageOrder = imageOrder;
    }

    /**
     * Get disk information.  This is intended to be pretty generic -
     * each disk format can build this as appropriate.  Each subclass should
     * override this method and add its own detail.
     */
    public List<DiskInformation> getDiskInformation() {
        List<DiskInformation> list = new ArrayList<>(super.getDiskInformation());
        list.add(new DiskInformation(textBundle.get("FormattedDisk.PhysicalSizeInBytes"), getPhysicalSize()));
        list.add(new DiskInformation(textBundle.get("FormattedDisk.PhysicalSizeInKb"), getPhysicalSize() / 1024));
        list.add(new DiskInformation(textBundle.get("FormattedDisk.ArchiveOrder"), getOrderName()));
        return list;
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
     * Resize the disk image to be its full size.  Only invoke this
     * method if a size does not match exception is thrown.
     */
    public void resizeDiskImage() {
        resizeDiskImage(getFreeSpace() + getUsedSpace());
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
     * Write the AppleCommander boot code to track 0 sector 0 of
     * the disk.  This will work for a floppy, but may cause problems
     * for other devices.
     */
    protected void writeBootCode() {
        InputStream inputStream = getClass().
                getResourceAsStream("/com/webcodepro/applecommander/storage/AppleCommander-boot.dump"); //$NON-NLS-1$
        if (inputStream != null) {
            byte[] bootCode = new byte[DiskConstants.SECTOR_SIZE];
            try {
                inputStream.read(bootCode, 0, bootCode.length);
                writeSector(0, 0, bootCode);
            } catch (IOException ignored) {
                // Ignored
            }
        }
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

    /**
     * Change the physical ordering of the disk.  This must be implemented by all
     * subclasses.  See AppleUtil for common utility methods.  (It is assumed that a
     * disk needs to be copied in the appropriate order - i.e., by track and sector for
     * a DOS type disk or by blocks in a ProDOS type disk.)
     */
    public abstract void changeImageOrder(ImageOrder imageOrder);
}
