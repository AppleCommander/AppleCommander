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
package org.applecommander.archive.binaryii;

import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.util.DataBuffer;

import java.util.Arrays;

public class BinaryIIFileStoreFactory implements FileStoreFactory, BinaryIIConstants {
    @Override
    public String getName() {
        return "Binary II (*.bny, *.bqy, *.bxy)";
    }

    @Override
    public void inspect(Context ctx) {
        if (ctx.source.getSize() > BLOCK_SIZE) {
            DataBuffer idBytes1 = ctx.source.readBytes(ID_BYTES1_OFFSET, ID_BYTES1.length);
            DataBuffer idBytes2 = ctx.source.readBytes(ID_BYTES2_OFFSET, ID_BYTES2.length);
            if (Arrays.equals(ID_BYTES1, idBytes1.asBytes()) && Arrays.equals(ID_BYTES2, idBytes2.asBytes())) {
                ctx.fileStores.add(new BinaryIIFileStore(ctx.source));
            }
        }
    }
}
