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
package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.device.nibble.*;
import org.applecommander.hint.Hint;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Optional;

public class TrackSectorNibbleDevice implements TrackSectorDevice {
    /**
     * Create a TrackSectorNibbleDevice. Device is not formatted but does not need to
     * go through an identification routine (which would fail with a blank image).
     */
    public static TrackSectorDevice create(NibbleTrackReaderWriter trackReaderWriter, int sectorsPerTrack) {
        assert trackReaderWriter != null;
        assert sectorsPerTrack == 13 || sectorsPerTrack == 16;
        return switch (sectorsPerTrack) {
            case 13 -> new TrackSectorNibbleDevice(trackReaderWriter,
                DiskMarker.disk525sector13(), new Nibble53Disk525Codec(), sectorsPerTrack);
            case 16 -> new TrackSectorNibbleDevice(trackReaderWriter,
                DiskMarker.disk525sector16(), new Nibble62Disk525Codec(), sectorsPerTrack);
            default -> throw new RuntimeException("unexpected sectors per track: " + sectorsPerTrack);
        };
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
    public static Optional<TrackSectorDevice> identify(NibbleTrackReaderWriter trackReaderWriter) {
        // Try 16-sector disks first:
        int sectorsPerTrack = 16;
        TrackSectorDevice device = new TrackSectorNibbleDevice(trackReaderWriter,
            DiskMarker.disk525sector16(), new Nibble62Disk525Codec(), sectorsPerTrack);
        int count = 0;
        for (int sector = 0; sector < sectorsPerTrack; sector++) {
            try {
                DataBuffer sectorData = device.readSector(0, sector);
                if (sectorData.limit() == TrackSectorDevice.SECTOR_SIZE) {
                    count++;
                }
            } catch (Throwable t) {
                // ignored
            }
        }
        if (count > 0) {
            // Just making certain we read more than sector 0
            return (count > 1) ? Optional.of(device) : Optional.empty();
        }

        // Next try 13-sector disks:
        sectorsPerTrack = 13;
        device = new TrackSectorNibbleDevice(trackReaderWriter,
            DiskMarker.disk525sector13(), new Nibble53Disk525Codec(), sectorsPerTrack);
        count = 0;
        for (int sector = 0; sector < sectorsPerTrack; sector++) {
            try {
                DataBuffer sectorData = device.readSector(0, sector);
                if (sectorData.limit() == TrackSectorDevice.SECTOR_SIZE) {
                    count++;
                }
            } catch (Throwable t) {
                // ignored
            }
        }
        // Just making certain we read more than sector 0
        return (count > 1) ? Optional.of(device) : Optional.empty();
    }

    /**
     * This is the "data" component of the address field: 2 x (Volume, Track, Sector, Checksum).
     */
    private final static int ADDRESS_FIELD_SIZE = 8;
    private final NibbleTrackReaderWriter trackReaderWriter;
    private final DiskMarker diskMarker;
    private final NibbleDiskCodec dataCodec;
    private final Geometry geometry;

    private TrackSectorNibbleDevice(NibbleTrackReaderWriter trackReaderWriter, DiskMarker diskMarker,
                                   NibbleDiskCodec dataCodec, int sectorsPerTrack) {
        this.trackReaderWriter = trackReaderWriter;
        this.diskMarker = diskMarker;
        this.dataCodec = dataCodec;
        this.geometry = new Geometry(trackReaderWriter.getTracksOnDevice(), sectorsPerTrack);
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, trackReaderWriter);
    }

    @Override
    public boolean is(Hint hint) {
        return hint == Hint.NIBBLE_SECTOR_ORDER;
    }

    @Override
    public boolean can(Capability capability) {
        if (capability == Capability.WRITE_SECTOR) {
            return trackReaderWriter.can(Capability.WRITE_TRACK)  && dataCodec.can(Capability.ENCODE);
        }
        return false;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Decode odd-even bytes as stored on disk.  The format will be
     * in two bytes.  They are stored as such:<pre>
     *     XX = 1d1d1d1d (odd data bits)
     *     YY = 1d1d1d1d (even data bits)
     * </pre>
     * XX is then shifted by a bit and ANDed with YY to get the data byte.
     * See page 3-12 in Beneath Apple DOS for more information.
     */
    public int decodeOddEven(DataBuffer rawData, int offset) {
        int b1 = rawData.getUnsignedByte(offset);
        int b2 = rawData.getUnsignedByte(offset+1);
        return (b1 << 1 | 0x01) & b2;
    }

    /**
     * Encode odd-even bytes to be stored on disk.  See decodeOddEven
     * for the format.
     * @see #decodeOddEven(DataBuffer, int)
     */
    public void encodeOddEven(DataBuffer data, int offset, int value) {
        int b1 = (value >> 1) | 0xaa;
        int b2 = value | 0xaa;
        data.putBytes(offset, b1, b2);
    }

    /**
     * Locate a field on the track.  These are identified by a 3 byte unique
     * signature.  Because of the way in which disk bytes are captured, we need
     * to wrap around the track to ensure all sequences of bytes are accounted for.
     * <p>
     * This method fills fieldData as well as returning the last position referenced
     * in the track buffer.
     */
    public static int locateField(int[] prolog, DataBuffer trackData, DataBuffer fieldData, int startingOffset) {
        int i = startingOffset;	    // logical position in track buffer (can wrap)
        int position = 0;			// physical position in field buffer
        final int endOfProlog = prolog.length;
        final int endOfData = endOfProlog + fieldData.limit();
        final int syncBytePadding = 40;
        final int endOfLoop = trackData.limit() + endOfData + syncBytePadding;
        while (i < endOfLoop) {
            int offset = i % trackData.limit();	// physical position in track buffer
            int b = trackData.getUnsignedByte(offset);
            if (position < endOfProlog && b == prolog[position]) {
                position++;
            } else if (position >= endOfProlog && position < endOfData) {
                fieldData.putByte(position - endOfProlog, b);
                position++;
            } else if (position == endOfData) {
                return i % trackData.limit();  // done reading!
            } else {
                position = 0;
            }
            i++;
        }
        return -1;  // not found
    }

    public int findSector(DataBuffer trackData, int track, int sector) {
        int offset = 0;
        DataBuffer addressField = DataBuffer.create(ADDRESS_FIELD_SIZE);
        boolean found = false;
        int attempts = 16;  // TODO do we need to be more specific?
        while (!found && attempts >= 0) {
            int nextOffset = locateField(diskMarker.addressProlog(), trackData, addressField, offset);
            if (nextOffset == -1) break;
            attempts--;
            offset = nextOffset;
            int t = decodeOddEven(addressField, 2);
            int s = decodeOddEven(addressField, 4);
            found = (t == track && s == sector);
        }
        if (!found) {
            var msg = String.format("unable to locate T%02d,S%02d", track, sector);
            throw new RuntimeException(msg);
        }
        return offset;
    }

    @Override
    public DataBuffer readSector(int track, int sector) {
        DataBuffer trackData = trackReaderWriter.readTrackData(track);
        // Locate address field for this track and sector
        int offset = findSector(trackData, track, sector);
        // Read data field that immediately follows the address field and the checksum byte
        if (offset != -1) {
            DataBuffer dataField = DataBuffer.create(dataCodec.encodedSize() + 1);
            offset = locateField(diskMarker.dataProlog(), trackData, dataField, offset);
            if (offset != -1) {
                return dataCodec.decode(dataField);
            }
        }
        // We found the sector but couldn't read the sector, fail horribly
        var msg = String.format("unable to read T%02d,S%02d", track, sector);
        throw new RuntimeException(msg);
    }

    @Override
    public void writeSector(int track, int sector, DataBuffer data) {
        DataBuffer trackData = trackReaderWriter.readTrackData(track);
        // Locate address field for this track and sector
        int offset = findSector(trackData, track, sector);
        // Locate data field that immediately follows the address field and the checksum byte
        if (offset != -1) {
            DataBuffer tmpField = DataBuffer.create(0);
            offset = locateField(diskMarker.dataProlog(), trackData, tmpField, offset);
            if (offset != -1) {
                // TODO we likely need to handle wrapping conditions here somewhere.
                trackData.put(offset, dataCodec.encode(data));
                trackReaderWriter.writeTrackData(track, trackData);
                return;
            }
        }
        // We found the sector but couldn't write the sector, fail horribly
        var msg = String.format("unable to write T%02d,S%02d", track, sector);
        throw new RuntimeException(msg);
    }

    @Override
    public void format() {
        if (getGeometry().sectorsPerTrack() == 13) {
            throw new RuntimeException("formatting 13-sector disks not supported");
        }
        // create initial address and data fields
        DataBuffer addressField = DataBuffer.create(14);
        DataBuffer dataField = DataBuffer.create(349);
        dataField.fill(0x96);
        DataBuffer addressProlog = DataBuffer.wrap(diskMarker.addressProlog());
        DataBuffer addressEpilog = DataBuffer.wrap(diskMarker.addressEpilog());
        DataBuffer dataProlog = DataBuffer.wrap(diskMarker.dataProlog());
        DataBuffer dataEpilog = DataBuffer.wrap(diskMarker.dataEpilog());
        addressField.put(0, addressProlog);
        addressField.put(11, addressEpilog);
        dataField.put(0, dataProlog);
        dataField.put(346, dataEpilog);
        // lay out track with address and data fields
        int addressSync = 43;	// number of sync bytes before address field
        int dataSync = 10;		// number of sync bytes before data field
        int volume = 254;		// disk volume# is always 254
        for (int track=0; track < geometry.tracksOnDisk(); track++) {
            DataBuffer trackData = trackReaderWriter.readTrackData(track);
            trackData.fill(0xff);
            int offset = 0;
            for (int sector=0; sector < geometry.sectorsPerTrack(); sector++) {
                // fill in address field:
                encodeOddEven(addressField, 3, volume);
                encodeOddEven(addressField, 5, track);
                encodeOddEven(addressField, 7, sector);
                encodeOddEven(addressField, 9, volume ^ track ^ sector);
                // write out sector data:
                offset+= addressSync;
                trackData.put(offset, addressField);
                offset+= addressField.limit();
                offset+= dataSync;
                trackData.put(offset, dataField);
                offset+= dataField.limit();
            }
            trackReaderWriter.writeTrackData(track, trackData);
        }
    }
}
