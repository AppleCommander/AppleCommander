package org.applecommander.device;

public record DiskMarker(int[] addressProlog, int[] addressEpilog, int[] dataProlog, int[] dataEpilog) {
        public static DiskMarker disk525sector16() {
            return build().addressProlog(0xd5, 0xaa, 0x96).addressEpilog(0xde, 0xaa)
                          .dataProlog(0xd5, 0xaa, 0xad).dataEpilog(0xde, 0xaa).get();
        }
        public static DiskMarker disk525sector13() {
            return build().addressProlog(0xd5, 0xaa, 0xb5).addressEpilog(0xde, 0xaa)
                          .dataProlog(0xd5, 0xaa, 0xad).dataEpilog(0xde, 0xaa).get();
        }
        public static Builder build() {
            return new Builder();
        }
        public static class Builder {
            private int[] addressProlog;
            private int[] addressEpilog;
            private int[] dataProlog;
            private int[] dataEpilog;
            public Builder addressProlog(int... addressProlog) {
                this.addressProlog = addressProlog;
                return this;
            }
            public Builder addressEpilog(int... addressEpilog) {
                this.addressEpilog = addressEpilog;
                return this;
            }
            public Builder dataProlog(int... dataProlog) {
                this.dataProlog = dataProlog;
                return this;
            }
            public Builder dataEpilog(int... dataEpilog) {
                this.dataEpilog = dataEpilog;
                return this;
            }
            public DiskMarker get() {
                return new DiskMarker(addressProlog, addressEpilog, dataProlog, dataEpilog);
            }
        }
    }