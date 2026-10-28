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
import org.applecommander.filestore.FileStoreOrganization;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

import java.util.List;

public class ZipFileStoreFactory implements FileStoreFactory {
    public static final int ZIP_SIGNATURE = 0x504b0304;     // "PK.."
    public static final List<FileStoreOrganization> VALID_ORGANIZATIONS = List.of(FileStoreOrganization.ARCHIVE);

    @Override
    public String getName() {
        return "Zip Archive (*.zip)";
    }

    @Override
    public void inspect(Context ctx) {
        DataBuffer signature = ctx.source.readBytes(0, 4);
        if (signature.getIntBE(0) == ZIP_SIGNATURE) {
            ctx.fileStores.add(new ZipFileStore(ctx.source));
        }
    }

    @Override
    public List<FileStoreOrganization> getFileStoreOrganizations() {
        return VALID_ORGANIZATIONS;
    }

    @Override
    public ZipFileStore create(FileStoreOrganization fileStoreOrganization) {
        Source source = fileStoreOrganization.createSource("zip");
        return new ZipFileStore(source);
    }

    // TODO: - DELETE ME -
    public static ZipFileStore create(Source source) {
        return new ZipFileStore(source);
    }
}
