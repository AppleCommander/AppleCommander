package io.github.applecommander.acx;

import java.util.function.Function;
import java.util.logging.Logger;

import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.physical.ByteArrayImageLayout;
import com.webcodepro.applecommander.storage.physical.DosOrder;
import com.webcodepro.applecommander.storage.physical.ImageOrder;
import com.webcodepro.applecommander.storage.physical.NibbleOrder;
import com.webcodepro.applecommander.storage.physical.ProdosOrder;

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
        ByteArrayImageLayout layout = new ByteArrayImageLayout(new byte[size]);
        return new DosOrder(layout);
    }
    /**
     * Nibblized disks are always 140K disks (or ~230K on disk). 
     */
    static ImageOrder create140kNibbleImageOrder(int size) {
        if (size != Disk.APPLE_140KB_NIBBLE_DISK && size != Disk.APPLE_140KB_DISK) {
            LOG.warning("Setting image size to 140KB");
        }
        ByteArrayImageLayout layout = new ByteArrayImageLayout(new byte[Disk.APPLE_140KB_NIBBLE_DISK]);
        return new NibbleOrder(layout);
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
        ByteArrayImageLayout layout = new ByteArrayImageLayout(size);
        return new ProdosOrder(layout);
    }

}
