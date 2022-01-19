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
package io.github.applecommander.acx.fileutil;

import java.util.Map;
import java.util.Optional;

import com.webcodepro.applecommander.storage.os.rdos.RdosFileEntry;

public class RdosFileEntryReader implements FileEntryReader {
    private static final Map<String,String> FILE_TYPES;
    static {
        FILE_TYPES = Map.of(
                "T", "TXT",
                "A", "BAS",
                "B", "BIN"
            );
    }
    
    private RdosFileEntry fileEntry;
    
    public RdosFileEntryReader(RdosFileEntry fileEntry) {
        this.fileEntry = fileEntry;
    }
    
    @Override
    public Optional<String> getFilename() {
        return Optional.ofNullable(fileEntry.getFilename());
    }

    @Override
    public Optional<Integer> getBinaryAddress() {
        return Optional.ofNullable(fileEntry.getAddress());
    }

    @Override
    public Optional<String> getProdosFiletype() {
        return Optional.ofNullable(FILE_TYPES.get(fileEntry.getFiletype()));
    }
    
    @Override
    public Optional<byte[]> getFileData() {
        return Optional.ofNullable(fileEntry.getFileData());
    }
}
