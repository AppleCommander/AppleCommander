/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx;

import java.util.function.Function;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.NibbleOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;
import org.applecommander.hint.Hint;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;

public enum OrderType {
    DOS(OrderType::createDosImageOrder), 
    NIBBLE(OrderType::create140kNibbleImageOrder), 
    PRODOS(OrderType::createProdosImageOrder);

    private static Logger LOG = Logger.getLogger(OrderType.class.getName());
    
    private Function<Integer,ImageOrder> createImageOrderFn;

    private OrderType(Function<Integer,ImageOrder> createImageOrderFn) {
        this.createImageOrderFn = createImageOrderFn;
    }
    
    public ImageOrder createImageOrder(int size) {
        return createImageOrderFn.apply(size);
    }

    /** 
     * At this time, the various DOS disks only support 140K or 800K disks and 
     * we have to rely on the SystemType to actually evaluate the correct size.
     */
    static ImageOrder createDosImageOrder(int size) {
        if (size < Disk.APPLE_140KB_DISK) {
            LOG.warning("Setting image size to 140KB.");
            size = Disk.APPLE_140KB_DISK;
        }
        else if (size == Disk.APPLE_140KB_DISK) {
            // Size is valid; don't warn and don't bump to 800K.
        }
        else if (size != Disk.APPLE_800KB_DISK) {
            LOG.warning("Setting image size to 800KB.");
            size = Disk.APPLE_800KB_DISK;
        }
        Source source = DataBufferSource.create(size, "new-disk").hints(Hint.DOS_SECTOR_ORDER).get();
        return new DosOrder(source);
    }
    /**
     * Nibblized disks are always 140K disks (or ~230K on disk). 
     */
    static ImageOrder create140kNibbleImageOrder(int size) {
        if (size != Disk.APPLE_140KB_NIBBLE_DISK && size != Disk.APPLE_140KB_DISK) {
            LOG.warning("Setting image size to 140KB");
        }
        Source source = DataBufferSource.create(Disk.APPLE_140KB_NIBBLE_DISK, "new-disk").get();
        return new NibbleOrder(source);
    }
    /** 
     * Lock ProDOS into 140K, 800K, or anything between 800K and 32M.
     * This means you _could_ setup a 807KB disk if you wanted. 
     */
    static ImageOrder createProdosImageOrder(int size) {
        if (size < Disk.APPLE_140KB_DISK) {
            LOG.warning("Setting image size to 140KB.");
            size = Disk.APPLE_140KB_DISK;
        }
        else if (size == Disk.APPLE_140KB_DISK) {
            // Size is valid; don't warn and don't bump to 800K.
        }
        else if (size < Disk.APPLE_800KB_DISK) {
            LOG.warning("Setting image size to 800KB.");
            size = Disk.APPLE_800KB_DISK;
        }
        else if (size > Disk.APPLE_32MB_HARDDISK) {
            LOG.warning("Setting image size to 32MB.");
            size = Disk.APPLE_32MB_HARDDISK;
        }
        Source source = DataBufferSource.create(size, "new-disk").hints(Hint.PRODOS_BLOCK_ORDER).get();
        return new ProdosOrder(source);
    }
}
