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
package org.applecommander.transfer;

import org.applecommander.util.DataBuffer;
import org.applecommander.util.FileMagic;

import java.util.Date;
import java.util.Optional;

/// Capture all relevant ProDOS attributes since they are the most common set of attributes.
/// Note that the data fork and resource fork are optional and may or may not be included.
public record ProdosAttributes(String name, int access, int fileType, int auxType, long size, Date creation, Date modified,
                               Optional<DataBuffer> dataFork, Optional<DataBuffer> resourceFork) {
    public static final int TXT = 0x04;
    public static final int BIN = 0x06;
    public static final int INT = 0xfa;
    public static final int BAS = 0xfc;
    public static final int REL = 0xfe;

    public String fileTypeText() {
        Optional<FileMagic.FileTypeSummary> summary = FileMagic.findProdosFileType(fileType, auxType);
        if (summary.isPresent()) {
            return summary.get().abbreviation();
        }
        return String.format("$%02X", fileType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private int access;
        private int fileType;
        private int auxType;
        private long size;
        private Date creation;
        private Date modified;
        private DataBuffer dataFork;
        private DataBuffer resourceFork;

        public Builder name(String name) {
            this.name = name;
            return this;
        }
        public Builder locked(boolean locked) {
            return locked ? locked() : unlocked();
        }
        public Builder locked() {
            return access(0x21);
        }
        public Builder unlocked() {
            return access(0xe3);
        }
        public Builder access(int access) {
            this.access = access;
            return this;
        }
        public Builder fileType(int fileType) {
            this.fileType = fileType;
            return this;
        }
        public Builder TXT() {
            return fileType(TXT);
        }
        public Builder BIN(int address) {
            return fileType(BIN).auxType(address);
        }
        public Builder INT() {
            return fileType(INT);
        }
        public Builder BAS() {
            return fileType(BAS);
        }
        public Builder REL() {
            return fileType(REL);
        }
        public Builder auxType(int auxType) {
            this.auxType = auxType;
            return this;
        }
        public Builder size(long size) {
            this.size = size;
            return this;
        }
        public Builder creation(Date creation) {
            this.creation = creation;
            return this;
        }
        public Builder modification(Date modified) {
            this.modified = modified;
            return this;
        }
        public Builder dataFork(byte[] dataFork) {
            return dataFork(DataBuffer.wrap(dataFork));
        }
        public Builder dataFork(DataBuffer dataFork) {
            this.dataFork = dataFork;
            return this;
        }
        public Builder resourceFork(byte[] resourceFork) {
            return resourceFork(DataBuffer.wrap(resourceFork));
        }
        public Builder resourceFork(DataBuffer resourceFork) {
            this.resourceFork = resourceFork;
            return this;
        }
        public ProdosAttributes get() {
            return new ProdosAttributes(name, access, fileType, auxType, size, creation, modified,
                    Optional.ofNullable(dataFork), Optional.ofNullable(resourceFork));
        }
    }
}
