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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Allow programmatic control of what is in the results of the file entry.
 * Useful for translating from raw data.
 * It can also be used to layer in results and overrides.
 */
public class OverrideFileEntryReader implements FileEntryReader {
    private static Logger LOG = Logger.getLogger(OverrideFileEntryReader.class.getName());
    
    private Optional<FileEntryReader> parent = Optional.empty();
    private Optional<String> filename = Optional.empty();
    private Optional<String> prodosFiletype = Optional.empty();
    private Optional<Boolean> locked = Optional.empty();
    private Optional<byte[]> fileData = Optional.empty();
    private Optional<byte[]> resourceData = Optional.empty();
    private Optional<Integer> binaryAddress = Optional.empty();
    private Optional<Integer> binaryLength = Optional.empty();
    private Optional<Integer> auxiliaryType = Optional.empty();
    private Optional<Date> creationDate = Optional.empty();
    private Optional<Date> lastModificationDate = Optional.empty();

    @Override
    public Optional<String> getFilename() {
        return filename.or(() -> parent.map(FileEntryReader::getFilename).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<String> getProdosFiletype() {
        return prodosFiletype.or(() -> parent.map(FileEntryReader::getProdosFiletype).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<Boolean> isLocked() {
        return locked.or(() -> parent.map(FileEntryReader::isLocked).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<byte[]> getFileData() {
        return fileData.or(() -> parent.map(FileEntryReader::getFileData).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<byte[]> getResourceData() {
        return resourceData.or(() -> parent.map(FileEntryReader::getResourceData).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<Integer> getBinaryAddress() {
        return binaryAddress.or(() -> parent.map(FileEntryReader::getBinaryAddress).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<Integer> getBinaryLength() {
        return binaryLength.or(() -> parent.map(FileEntryReader::getBinaryLength).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<Integer> getAuxiliaryType() {
        return auxiliaryType.or(() -> parent.map(FileEntryReader::getBinaryLength).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<Date> getCreationDate() {
        return creationDate.or(() -> parent.map(FileEntryReader::getCreationDate).filter(Optional::isPresent).map(Optional::get));
    }
    @Override
    public Optional<Date> getLastModificationDate() {
        return lastModificationDate.or(() -> parent.map(FileEntryReader::getLastModificationDate).filter(Optional::isPresent).map(Optional::get));
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private OverrideFileEntryReader fileEntryReader = new OverrideFileEntryReader();
        
        public FileEntryReader build(FileEntryReader parent) {
            Objects.requireNonNull(parent);
            fileEntryReader.parent = Optional.of(parent);
            return fileEntryReader;
        }
        public List<FileEntryReader> buildList(FileEntryReader parent) {
            return Arrays.asList(build(parent));
        }
        public FileEntryReader build() {
            return fileEntryReader;
        }
        public List<FileEntryReader> buildList() {
            return Arrays.asList(build());
        }
        
        public Builder filename(String filename) {
            Objects.requireNonNull(filename);
            LOG.finest(() -> String.format("Setting filename to '%s'", filename));
            fileEntryReader.filename = Optional.of(filename);
            return this;
        }
        public Builder filename(Optional<String> filename) {
            Objects.requireNonNull(filename);
            filename.ifPresent(name -> {
                LOG.finest(() -> String.format("Setting filename to '%s'", name));
            });
            fileEntryReader.filename = filename;
            return this;
        }
        public Builder prodosFiletype(String filetype) {
            Objects.requireNonNull(filetype);
            LOG.finest(() -> String.format("Setting file type to '%s'", filetype));
            fileEntryReader.prodosFiletype = Optional.of(filetype);
            return this;
        }
        public Builder prodosFiletype(Optional<String> filetype) {
            Objects.requireNonNull(filetype);
            filetype.ifPresent(type -> {
                LOG.finest(() -> String.format("Setting file type to '%s'", type));
            });
            fileEntryReader.prodosFiletype = filetype;
            return this;
        }
        public Builder locked(boolean locked) {
            LOG.finest(() -> String.format("Setting locked to '%b'", locked));
            fileEntryReader.locked = Optional.of(locked);
            return this;
        }
        public Builder locked(Optional<Boolean> locked) {
            locked.ifPresent(flag -> {
                LOG.finest(() -> String.format("Setting locked to '%b'", flag));
            });
            Objects.requireNonNull(locked);
            fileEntryReader.locked = locked;
            return this;
        }
        public Builder fileData(byte[] fileData) {
            Objects.requireNonNull(fileData);
            LOG.finest(() -> String.format("Setting data fork to %d bytes.", fileData.length));
            fileEntryReader.fileData = Optional.of(fileData);
            return this;
        }
        public Builder fileData(Optional<byte[]> fileData) {
            Objects.requireNonNull(fileData);
            fileData.ifPresent(data -> {
                LOG.finest(() -> String.format("Setting data fork to %d bytes.", data.length));
            });
            fileEntryReader.fileData = fileData;
            return this;
        }
        public Builder resourceData(byte[] resourceData) {
            Objects.requireNonNull(resourceData);
            LOG.finest(() -> String.format("Setting data fork to %d bytes.", resourceData.length));
            fileEntryReader.resourceData = Optional.of(resourceData);
            return this;
        }
        public Builder resourceData(Optional<byte[]> resourceData) {
            Objects.requireNonNull(resourceData);
            resourceData.ifPresent(data -> {
                LOG.finest(() -> String.format("Setting resource fork to %d bytes.", data.length));
            });
            LOG.finest(() -> String.format("Setting file data to %d bytes.", resourceData.orElse(new byte[0]).length));
            fileEntryReader.resourceData = resourceData;
            return this;
        }
        public Builder binaryAddress(int binaryAddress) {
            fileEntryReader.binaryAddress = Optional.of(binaryAddress);
            LOG.finest(() -> String.format("Setting address to $%04xs.", binaryAddress));
            return this;
        }
        public Builder binaryAddress(Optional<Integer> binaryAddress) {
            Objects.requireNonNull(binaryAddress);
            binaryAddress.ifPresent(addr -> {
                LOG.finest(() -> String.format("Setting address to $%04xs.", addr));
            });
            fileEntryReader.binaryAddress = binaryAddress;
            return this;
        }
        public Builder binaryLength(int binaryLength) {
            LOG.finest(() -> String.format("Setting binary length to %04x.", binaryLength));
            fileEntryReader.binaryLength = Optional.of(binaryLength);
            return this;
        }
        public Builder binaryLength(Optional<Integer> binaryLength) {
            Objects.requireNonNull(binaryLength);
            binaryLength.ifPresent(length -> {
                LOG.finest(() -> String.format("Setting binary length to %04x.", length));
            });
            fileEntryReader.binaryLength = binaryLength;
            return this;
        }
        public Builder auxiliaryType(int auxiliaryType) {
            LOG.finest(() -> String.format("Setting aux type to $%04x.", auxiliaryType));
            fileEntryReader.auxiliaryType = Optional.of(auxiliaryType);
            return this;
        }
        public Builder auxiliaryType(Optional<Integer> auxiliaryType) {
            Objects.requireNonNull(auxiliaryType);
            auxiliaryType.ifPresent(type -> {
                LOG.finest(() -> String.format("Setting aux type to $%04x.", type));
            });
            fileEntryReader.auxiliaryType = auxiliaryType;
            return this;
        }
        public Builder creationDate(Date creationDate) {
            Objects.requireNonNull(creationDate);
            LOG.finest(() -> String.format("Setting creation date to %s.", creationDate));
            fileEntryReader.creationDate = Optional.of(creationDate);
            return this;
        }
        public Builder creationDate(Optional<Date> creationDate) {
            Objects.requireNonNull(creationDate);
            creationDate.ifPresent(date -> {
                LOG.finest(() -> String.format("Setting creation date to %s.", date));
            });
            fileEntryReader.creationDate = creationDate;
            return this;
        }
        public Builder lastModificationDate(Date lastModificationDate) {
            Objects.requireNonNull(lastModificationDate);
            LOG.finest(() -> String.format("Setting last modification date to %s.", lastModificationDate));
            fileEntryReader.lastModificationDate = Optional.of(lastModificationDate);
            return this;
        }
        public Builder lastModificationDate(Optional<Date> lastModificationDate) {
            Objects.requireNonNull(lastModificationDate);
            lastModificationDate.ifPresent(date -> {
                LOG.finest(() -> String.format("Setting last modification date to %s.", date));
            });
            fileEntryReader.lastModificationDate = lastModificationDate;
            return this;
        }
    }
}
