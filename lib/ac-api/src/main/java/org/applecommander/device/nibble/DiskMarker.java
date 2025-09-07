/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package org.applecommander.device.nibble;

/**
 * A disk marker is just the prolog and epilog bytes for the address field and data field.
 * It also serves as the keeper of expected sectors and codec.
 */
public record DiskMarker(int sectorsOnTrack, NibbleDiskCodec codec, int[] addressProlog, int[] addressEpilog, int[] dataProlog, int[] dataEpilog) {
        public static DiskMarker disk525sector16() {
            return build(16).addressProlog(0xd5, 0xaa, 0x96).addressEpilog(0xde, 0xaa, 0xeb)
                    .dataProlog(0xd5, 0xaa, 0xad).dataEpilog(0xde, 0xaa, 0xeb).get();
        }
        public static DiskMarker disk525sector13() {
            return build(13).addressProlog(0xd5, 0xaa, 0xb5).addressEpilog(0xde, 0xaa, 0xeb)
                    .dataProlog(0xd5, 0xaa, 0xad).dataEpilog(0xde, 0xaa, 0xeb).get();
        }
        public static Builder build(int sectorsOnTrack) {
            return new Builder(sectorsOnTrack);
        }
        public static class Builder {
            private final int sectorsOnTrack;
            private final NibbleDiskCodec codec;
            private int[] addressProlog;
            private int[] addressEpilog;
            private int[] dataProlog;
            private int[] dataEpilog;
            private Builder(int sectorsOnTrack) {
                this.sectorsOnTrack = sectorsOnTrack;
                this.codec = (sectorsOnTrack == 13) ? new Nibble53Disk525Codec() : new Nibble62Disk525Codec();
            }
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
                return new DiskMarker(sectorsOnTrack, codec, addressProlog, addressEpilog, dataProlog, dataEpilog);
            }
        }
    }