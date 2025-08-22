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
package io.github.applecommander.filters;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;

/**
 * A custom FileFilter to dump "raw" data from the disk.
 * This filter uses the filename as given on the Disk with
 * no additional extensions.
 * 
 * @author rob
 */
public class RawFileFilter implements FileFilter {

    @Override
    public byte[] filter(FileEntry fileEntry) {
        return fileEntry.getFileData();
    }

    @Override
    public String getSuggestedFileName(FileEntry fileEntry) {
        return fileEntry.getFilename();
    }

}
