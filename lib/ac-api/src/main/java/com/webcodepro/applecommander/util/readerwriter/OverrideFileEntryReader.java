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

/**
 * Allow programmatic control of what is in the results of the file entry.
 * Useful for translating from raw data.
 * It can also be used to layer in results and overrides.
 */
public class OverrideFileEntryReader implements FileEntryReader {
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
        // Special case, the AppleCommander API does not really handle resource forks.
        return resourceData;
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
            fileEntryReader.filename = Optional.of(filename);
            return this;
        }
        public Builder filename(Optional<String> filename) {
            Objects.requireNonNull(filename);
            fileEntryReader.filename = filename;
            return this;
        }
        public Builder prodosFiletype(String filetype) {
            Objects.requireNonNull(filetype);
            fileEntryReader.prodosFiletype = Optional.of(filetype);
            return this;
        }
        public Builder prodosFiletype(Optional<String> filetype) {
            Objects.requireNonNull(filetype);
            fileEntryReader.prodosFiletype = filetype;
            return this;
        }
        public Builder locked(boolean locked) {
            fileEntryReader.locked = Optional.of(locked);
            return this;
        }
        public Builder locked(Optional<Boolean> locked) {
            Objects.requireNonNull(locked);
            fileEntryReader.locked = locked;
            return this;
        }
        public Builder fileData(byte[] fileData) {
            Objects.requireNonNull(fileData);
            fileEntryReader.fileData = Optional.of(fileData);
            return this;
        }
        public Builder fileData(Optional<byte[]> fileData) {
            Objects.requireNonNull(fileData);
            fileEntryReader.fileData = fileData;
            return this;
        }
        public Builder resourceData(byte[] resourceData) {
            Objects.requireNonNull(resourceData);
            fileEntryReader.resourceData = Optional.of(resourceData);
            return this;
        }
        public Builder resourceData(Optional<byte[]> resourceData) {
            Objects.requireNonNull(resourceData);
            fileEntryReader.resourceData = resourceData;
            return this;
        }
        public Builder binaryAddress(int binaryAddress) {
            fileEntryReader.binaryAddress = Optional.of(binaryAddress);
            return this;
        }
        public Builder binaryAddress(Optional<Integer> binaryAddress) {
            Objects.requireNonNull(binaryAddress);
            fileEntryReader.binaryAddress = binaryAddress;
            return this;
        }
        public Builder binaryLength(int binaryLength) {
            fileEntryReader.binaryLength = Optional.of(binaryLength);
            return this;
        }
        public Builder binaryLength(Optional<Integer> binaryLength) {
            Objects.requireNonNull(binaryLength);
            fileEntryReader.binaryLength = binaryLength;
            return this;
        }
        public Builder auxiliaryType(int auxiliaryType) {
            fileEntryReader.auxiliaryType = Optional.of(auxiliaryType);
            return this;
        }
        public Builder auxiliaryType(Optional<Integer> auxiliaryType) {
            Objects.requireNonNull(auxiliaryType);
            fileEntryReader.auxiliaryType = auxiliaryType;
            return this;
        }
        public Builder creationDate(Date creationDate) {
            Objects.requireNonNull(creationDate);
            fileEntryReader.creationDate = Optional.of(creationDate);
            return this;
        }
        public Builder creationDate(Optional<Date> creationDate) {
            Objects.requireNonNull(creationDate);
            fileEntryReader.creationDate = creationDate;
            return this;
        }
        public Builder lastModificationDate(Date lastModificationDate) {
            Objects.requireNonNull(lastModificationDate);
            fileEntryReader.lastModificationDate = Optional.of(lastModificationDate);
            return this;
        }
        public Builder lastModificationDate(Optional<Date> lastModificationDate) {
            Objects.requireNonNull(lastModificationDate);
            fileEntryReader.lastModificationDate = lastModificationDate;
            return this;
        }
    }
}
