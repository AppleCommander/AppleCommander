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

import org.applecommander.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/// FileStores is the factory mechanism to identify and work with Apple II
/// images. The FileStore interface it intended to support both archive and
/// disk images.
///
/// Example usage:
/// ```
/// Source source = Sources.create("filename.po").orElseThrow();
/// DiskFactory.Context ctx = FileStores.inspect(source);
/// ```
///
/// On return, the Context will contain any discovered FileSources.
/// It also contains the Source used and if a NibbleTrackReaderWriter
/// was found.
public class FileStores {
    private static final List<FileStoreFactory> FACTORIES;
    static {
        FACTORIES = new ArrayList<>();
        for (FileStoreFactory factory : ServiceLoader.load(FileStoreFactory.class)) {
            FACTORIES.add(factory);
        }
    }

    /**
     * Standardized FileStore creation. Uses the ServiceLoader mechanism to identify
     * all potential FileStore factories.
     */
    public static FileStoreFactory.Context inspect(Source source) {
        FileStoreFactory.Context ctx = new FileStoreFactory.Context(source);
        FACTORIES.forEach(factory -> {
            try {
                factory.inspect(ctx);
            } catch (Throwable t) {
                // ignore it
            }
        });
        return ctx;
    }
}
