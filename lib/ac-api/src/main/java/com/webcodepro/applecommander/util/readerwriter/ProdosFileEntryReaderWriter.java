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
package com.webcodepro.applecommander.util.readerwriter;

import java.util.Date;
import java.util.Optional;

import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;

public class ProdosFileEntryReaderWriter implements FileEntryReader, FileEntryWriter {
    private ProdosFileEntry fileEntry;
    
    public ProdosFileEntryReaderWriter(ProdosFileEntry fileEntry) {
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
        return Optional.ofNullable(fileEntry.getFiletype());
    }
    @Override
    public void setProdosFiletype(String filetype) {
        fileEntry.setFiletype(filetype);
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
    public void setFileData(byte[] data, byte[] resource) {
        try {
            // If we have a resource fork in addition to a data fork,
            // then we've got a GSOS storage type $5. 
            fileEntry.setFileData(data, resource);
            fileEntry.setStorageType(0x05);
        } catch (DiskFullException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Optional<Integer> getBinaryAddress() {
        if (fileEntry.needsAddress()) {
            return Optional.of(fileEntry.getAuxiliaryType());
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
        return Optional.of(fileEntry.getSize());
    }
    @Override
    public void setBinaryLength(int length) {
        // Nothing to do
    }
    
    @Override
    public Optional<Integer> getAuxiliaryType() {
        return Optional.of(fileEntry.getAuxiliaryType());
    }
    @Override
    public void setAuxiliaryType(int auxType) {
        fileEntry.setAuxiliaryType(auxType);
    }
    
    @Override
    public Optional<Date> getCreationDate() {
        return Optional.ofNullable(fileEntry.getCreationDate());
    }
    @Override
    public void setCreationDate(Date date) {
        fileEntry.setCreationDate(date);
    }
    
    @Override
    public Optional<Date> getLastModificationDate() {
        return Optional.ofNullable(fileEntry.getLastModificationDate());
    }
    @Override
    public void setLastModificationDate(Date date) {
        fileEntry.setLastModificationDate(date);
    }
}
