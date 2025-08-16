package com.webcodepro.applecommander.storage;

import com.webcodepro.applecommander.storage.physical.*;
import org.applecommander.hint.Hint;
import org.applecommander.image.WozImage;
import org.applecommander.source.Source;

import java.util.ArrayList;
import java.util.List;

public interface DiskFactory {
    void inspect(Context ctx);

    class Context {
        public final Source source;
        public final List<ImageOrder> orders = new ArrayList<>();
        public final List<FormattedDisk> disks = new ArrayList<>();

        public Context(Source source) {
            this.source = source;

            /* Does it have the WOZ1 or WOZ2 header? */
            int signature = source.readBytes(0, 4).readInt();
            if (WozImage.WOZ1_MAGIC == signature || WozImage.WOZ2_MAGIC == signature) {
                orders.add(new WozOrder(source));
            } else if (source.is(Hint.NIBBLE_SECTOR_ORDER) || source.isApproxEQ(Disk.APPLE_140KB_NIBBLE_DISK)) {
                orders.add(new NibbleOrder(source));
            } else if (source.is(Hint.PRODOS_BLOCK_ORDER) || source.getSize() > Disk.APPLE_400KB_DISK || source.extensionLike("po")) {
                orders.add(new ProdosOrder(source));
            } else if (source.is(Hint.DOS_SECTOR_ORDER) || source.extensionLike("do")) {
                orders.add(new DosOrder(source));
            } else {
                // Could be either - most likely the nebulous "dsk" extension
                orders.add(new DosOrder(source));
                orders.add(new ProdosOrder(source));
            }
        }
    }
}
