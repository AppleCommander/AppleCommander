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
package org.applecommander.archive.binscii;

import org.applecommander.filestore.FileStoreFactory;

public class BinsciiFileStoreFactory implements FileStoreFactory, BinsciiConstants {
    @Override
    public String getName() {
        return "BinSCII (*.bsc)";
    }

    @Override
    public void inspect(Context ctx) {
        // Verify we have the FiLeStArTfIlEsTaRt (not perfect but good enough)
        if (ctx.source.readAllBytes().scan(SENTINAL.getBytes(), 0).isPresent()) {
            ctx.fileStores.add(new BinsciiFileStore(ctx.source));
        }
    }
}
