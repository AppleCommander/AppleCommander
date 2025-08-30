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
package com.webcodepro.applecommander.storage;

import com.webcodepro.applecommander.storage.physical.*;
import org.applecommander.codec.Nibble53Disk525Codec;
import org.applecommander.codec.Nibble62Disk525Codec;
import org.applecommander.device.*;
import org.applecommander.hint.Hint;
import org.applecommander.image.NibbleImage;
import org.applecommander.image.WozImage;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;

public interface DiskFactory {
    void inspect(Context ctx);

    class Context {
        public final Source source;
        public final List<ImageOrder> orders = new ArrayList<>();
        public final List<FormattedDisk> disks = new ArrayList<>();
        // Note: These are only set if we *KNOW* what they are. With the exception of DSK images,
        //       where both will be set.
        public final BlockDevice blockDevice;
        public final TrackSectorDevice sectorDevice;

        public Context(Source source) {
            this.source = source;

            /* Does it have the WOZ1 or WOZ2 header? */
            int signature = source.readBytes(0, 4).readInt();
            if (WozImage.WOZ1_MAGIC == signature || WozImage.WOZ2_MAGIC == signature) {
                orders.add(new WozOrder(source));
                blockDevice = null;
                sectorDevice = identifySectorsPerTrack(new WozImage(source));
            } else if (source.is(Hint.NIBBLE_SECTOR_ORDER) || source.isApproxEQ(DiskConstants.APPLE_140KB_NIBBLE_DISK)) {
                orders.add(new NibbleOrder(source));
                blockDevice = null;
                sectorDevice = identifySectorsPerTrack(new NibbleImage(source));
            } else if (source.is(Hint.PRODOS_BLOCK_ORDER) || source.getSize() > DiskConstants.APPLE_400KB_DISK || source.extensionLike("po")) {
                orders.add(new ProdosOrder(source));
                blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
                sectorDevice = null;
            } else if (source.is(Hint.DOS_SECTOR_ORDER) || source.extensionLike("do")) {
                orders.add(new DosOrder(source));
                blockDevice = null;
                sectorDevice = new DosOrderedTrackSectorDevice(source);
            } else {
                // Could be either - most likely the nebulous "dsk" extension
                orders.add(new DosOrder(source));
                orders.add(new ProdosOrder(source));
                blockDevice = new ProdosOrderedBlockDevice(source, BlockDevice.STANDARD_BLOCK_SIZE);
                sectorDevice = new DosOrderedTrackSectorDevice(source);
            }
        }

        /**
         * Brute force attempt to identify 13 or 16 sector tracks. Note we only test track 0.
         * Also note, we can do much better -- but all the nibble stuff will need to be reconfigured
         * to allow different prologs/epilogs per track*. This likely can enable reading early software
         * protection schemes that just fiddled with those bytes. DOS likely got moved around, so that
         * would be coupled with more flexibility in DOS. See the "experimenting/identifying-nibble-prolog-bytes"
         * for some experimental work.
         * <p/>
         * Note: the variance in prolog/epilog can be super detailed, but it is unlikely a DOS clone
         * has different prolog/epilog bytes per sector. Per track may be a bit over-the-top. Except, that
         * it appears Ultima I may have used it. :-)
         */
        private TrackSectorDevice identifySectorsPerTrack(NibbleTrackReaderWriter trackReaderWriter) {
            try {
                // Try 16-sector disks first:
                TrackSectorDevice device = new TrackSectorNibbleDevice(trackReaderWriter,
                        DiskMarker.disk525sector16(), new Nibble62Disk525Codec(), 16);
                DataBuffer sectorData = device.readSector(0, 0);
                if (sectorData.limit() == TrackSectorDevice.SECTOR_SIZE) {
                    return device;
                }
            } catch (Throwable t) {
                // ignored
            }
            try {
                // Next try 13-sector disks:
                TrackSectorDevice device = new TrackSectorNibbleDevice(trackReaderWriter,
                        DiskMarker.disk525sector13(), new Nibble53Disk525Codec(), 13);
                DataBuffer sectorData = device.readSector(0, 0);
                if (sectorData.limit() == TrackSectorDevice.SECTOR_SIZE) {
                    return device;
                }
            } catch (Throwable t) {
                // ignored
            }
            // Failure
            return null;
        }
    }
}
