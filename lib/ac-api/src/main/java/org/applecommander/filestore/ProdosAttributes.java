package org.applecommander.filestore;

import java.util.Date;

public record ProdosAttributes(String name, int access, int fileType, int auxType, long size, Date creation, Date modified) {
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

        public Builder name(String name) {
            this.name = name;
            return this;
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
        public ProdosAttributes build() {
            return new ProdosAttributes(name, access, fileType, auxType, size, creation, modified);
        }
    }
}
