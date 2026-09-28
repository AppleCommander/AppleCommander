/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.filestore;

import com.webcodepro.applecommander.storage.DiskConstants;
import org.applecommander.device.nibble.NibbleTrackReaderWriter;
import org.applecommander.hint.Hint;
import org.applecommander.image.NibbleImage;
import org.applecommander.image.WozImage;
import org.applecommander.source.Source;

import java.util.ArrayList;
import java.util.List;

/**
 * The FileStoreFactory inspects a given Source to see if it matches any known file store(s).
 * Invoke via {@link FileStores#inspect(Source)} which will return a Context. The Context _can be empty_.
 * Note that most logic is deferred to the "old" AppleCommander APIs for the time being.
 */
public interface FileStoreFactory {
    void inspect(Context ctx);

    class Context {
        public final Source source;
        public final NibbleTrackReaderWriter nibbleTrackReaderWriter;
        public final List<FileStore> fileStores = new ArrayList<>();

        public Context(Source source) {
            this.source = source;

            /* Does it have the WOZ1 or WOZ2 header? */
            int signature = source.readBytes(0, 4).readInt();
            if (WozImage.WOZ1_MAGIC == signature || WozImage.WOZ2_MAGIC == signature) {
                nibbleTrackReaderWriter = new WozImage(source);
            } else if (source.is(Hint.NIBBLE_SECTOR_ORDER) || source.isApproxEQ(DiskConstants.APPLE_140KB_NIBBLE_DISK)) {
                nibbleTrackReaderWriter = new NibbleImage(source);
            } else {
                nibbleTrackReaderWriter = null;
            }
        }
    }
}