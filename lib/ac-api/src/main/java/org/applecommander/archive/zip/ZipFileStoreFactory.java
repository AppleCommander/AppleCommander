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
package org.applecommander.archive.zip;

import org.applecommander.filestore.FileStoreFactory;
import org.applecommander.util.DataBuffer;

public class ZipFileStoreFactory implements FileStoreFactory {
    private static final int ZIP_SIGNATURE = 0x504b0304;     // "PK.."

    @Override
    public void inspect(Context ctx) {
        DataBuffer signature = ctx.source.readBytes(0, 4);
        if (signature.getIntBE(0) ==  ZIP_SIGNATURE) {
            ctx.fileStores.add(new ZipFileStore(ctx.source));
        }
    }
}
