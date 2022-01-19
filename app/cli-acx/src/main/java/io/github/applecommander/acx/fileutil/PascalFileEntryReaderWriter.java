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

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.filters.PascalTextFileFilter;
import com.webcodepro.applecommander.storage.os.pascal.PascalFileEntry;

public class PascalFileEntryReaderWriter implements FileEntryReader, FileEntryWriter {
    private static final PascalTextFileFilter TEXT_FILTER = new PascalTextFileFilter();
    private static final Map<String,String> FILE_TYPES;
    static {
        FILE_TYPES = Map.of(
                // Pascal => Prodos
                "xdskfile", "BAD",  // TODO we should skip bad block files
                "CODE", "BIN",      // TODO is there an address?
                "TEXT", "TXT",
                "INFO", "TXT",      // TODO We should skip debugger info
                "DATA", "BIN",
                "GRAF", "BIN",      // TODO compressed graphics image
                "FOTO", "BIN",      // TODO screen image
                "securedir", "BIN", // TODO is this even implemented
                
                // Prodos => Pascal
                "BIN", "DATA",
                "TXT", "TEXT"
            );
    }
    
    private PascalFileEntry fileEntry;
    
    public PascalFileEntryReaderWriter(PascalFileEntry fileEntry) {
        this.fileEntry = fileEntry;
    }
    
    @Override
    public Optional<String> getFilename() {
        return Optional.ofNullable(fileEntry.getFilename());
    }
    @Override
    public void setFilename(String filename) {
        fileEntry.setFilename(filename);
    }
    
    @Override
    public Optional<String> getProdosFiletype() {
        return Optional.ofNullable(FILE_TYPES.get(fileEntry.getFiletype()));
    }
    @Override
    public void setProdosFiletype(String filetype) {
        fileEntry.setFiletype(FILE_TYPES.getOrDefault(filetype, "DATA"));
    }
    
    @Override
    public Optional<Date> getLastModificationDate() {
        return Optional.ofNullable(fileEntry.getModificationDate());
    }
    @Override
    public void setLastModificationDate(Date date) {
        fileEntry.setModificationDate(date);
    }
    
    @Override
    public Optional<byte[]> getFileData() {
        if ("TEXT".equals(fileEntry.getFiletype())) {
            return Optional.ofNullable(TEXT_FILTER.filter(fileEntry));
        }
        else {
            return Optional.ofNullable(fileEntry.getFileData());
        }
    }
    @Override
    public void setFileData(byte[] data) {
        try {
            fileEntry.setFileData(data);
        } catch (DiskFullException e) {
            throw new RuntimeException(e);
        }
    }
}
