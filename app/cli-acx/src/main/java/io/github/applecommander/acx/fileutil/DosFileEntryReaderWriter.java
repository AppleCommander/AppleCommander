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

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.os.dos33.DosFileEntry;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.util.AppleUtil;

public class DosFileEntryReaderWriter implements FileEntryReader, FileEntryWriter {
    private static final Map<String,String> FILE_TYPES;
    static {
        FILE_TYPES = Map.of(
                "T", "TXT",
                "I", "INT",
                "A", "BAS",
                "B", "BIN",
                "S", "$F1",
                "R", "REL",
                "a", "$F2",
                "b", "$F3"
                );
    }
    
    private DosFileEntry fileEntry;
    
    public DosFileEntryReaderWriter(DosFileEntry fileEntry) {
        if (fileEntry.isDeleted()) {
            throw new RuntimeException("Unable to copy deleted files.");
        }
        this.fileEntry = fileEntry;
    }
    
    @Override
    public Optional<String> getFilename() {
        return Optional.of(fileEntry.getFilename());
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
        String dosFileType = FILE_TYPES.entrySet()
                                       .stream()
                                       .filter(e -> e.getValue().equals(filetype))
                                       .map(Map.Entry::getKey)
                                       .findFirst()
                                       .orElse("B");
        fileEntry.setFiletype(dosFileType);
    }
    
    @Override
    public Optional<Boolean> isLocked() {
        return Optional.of(fileEntry.isLocked());
    }
    @Override
    public void setLocked(boolean flag) {
        fileEntry.setLocked(flag);
    }
    
    @Override
    public Optional<byte[]> getFileData() {
        return Optional.ofNullable(fileEntry.getFileData());
    }
    @Override
    public void setFileData(byte[] data) {
        try {
            fileEntry.setFileData(data);
        } catch (DiskFullException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Optional<Integer> getBinaryAddress() {
        if (fileEntry.isBinaryFile()) {
            byte[] rawdata = getRawFileData();
            return Optional.of(AppleUtil.getWordValue(rawdata, 0));
        }
        return Optional.empty();
    }
    @Override
    public void setBinaryAddress(int address) {
        if (fileEntry.needsAddress()) {
            fileEntry.setAddress(address);
        }
    }
    
    @Override
    public Optional<Integer> getBinaryLength() {
        if (fileEntry.isBinaryFile() || fileEntry.isApplesoftBasicFile() || fileEntry.isIntegerBasicFile()) {
            // DosFileEntry pulls the address
            return Optional.of(fileEntry.getSize());
        }
        return Optional.empty();
    }
    @Override
    public void setBinaryLength(int length) {
        // Nothing to do
    }
    
    private byte[] getRawFileData() {
        DosFormatDisk disk = (DosFormatDisk) fileEntry.getFormattedDisk();
        return disk.getFileData(fileEntry);
    }
}
