package org.applecommander.device;

import org.applecommander.capability.Capability;
import org.applecommander.codec.NibbleDiskCodec;
import org.applecommander.util.DataBuffer;

public class TrackSectorNibbleDevice implements TrackSectorDevice {
    /**
     * This is the "data" component of the address field: 2 x (Volume, Track, Sector, Checksum).
     */
    private final static int ADDRESS_FIELD_SIZE = 8;
    private final NibbleTrackReaderWriter trackReaderWriter;
    private final DiskMarker diskMarker;
    private final NibbleDiskCodec dataCodec;
    private final Geometry geometry;

    public TrackSectorNibbleDevice(NibbleTrackReaderWriter trackReaderWriter, DiskMarker diskMarker,
                                   NibbleDiskCodec dataCodec, int sectorsPerTrack) {
        this.trackReaderWriter = trackReaderWriter;
        this.diskMarker = diskMarker;
        this.dataCodec = dataCodec;
        this.geometry = new Geometry(trackReaderWriter.getTracksOnDevice(), sectorsPerTrack);
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
     * Note that we also check the epilog but allow it to be wiggly.
     * <p>
     * This method fills fieldData as well as returning the last position referenced
     * in the track buffer.
     */
    public static int locateField(int[] prolog, int[] epilog, DataBuffer trackData, DataBuffer fieldData, int startingOffset) {
        int i = startingOffset;	    // logical position in track buffer (can wrap)
        int position = 0;			// physical position in field buffer
        final int endOfProlog = prolog.length;
        final int endOfData = endOfProlog + fieldData.limit();
        final int endOfEpilog = endOfData + epilog.length;
        while (i < trackData.limit() + fieldData.limit()) {
            int offset = i % trackData.limit();	// physical position in track buffer
            int b = trackData.getUnsignedByte(offset);
            if (position < endOfProlog && b == prolog[position]) {
                position++;
            } else if (position >= endOfProlog && position < endOfData) {
                fieldData.putByte(position - endOfProlog, b);
                position++;
            } else if (position >= endOfData && position < endOfEpilog) {
                assert (b == epilog[position - endOfData]);
                position++;
            } else if (position == endOfEpilog) {
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
            int nextOffset = locateField(diskMarker.addressProlog(), diskMarker.addressEpilog(), trackData, addressField, offset);
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
            offset = locateField(diskMarker.dataProlog(), diskMarker.dataEpilog(), trackData, dataField, offset);
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
        // TODO
    }
}
