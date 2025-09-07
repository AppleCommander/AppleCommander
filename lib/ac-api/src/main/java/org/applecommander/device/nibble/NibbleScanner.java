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

import org.applecommander.util.DataBuffer;
import static org.applecommander.device.nibble.NibbleUtil.*;

import java.util.*;

/**
 * Scan a nibble disk and try to identify the formatting. Note that this is really only good for
 * protected DOS disks. Also note that the DOS catalog track has likely been moved, so the DOS
 * filesystem needs to also support it.
 * <p/>
 * This appears to be a rather successful algorithm:
 * <ol>
 * <li>Locate address prolog bytes by simply looking for the track number and a sensible sector
 *    using 4&4 encoding.</li>
 * <li>From that prolog (per track), start from end of address prolog and scan for sync bytes.</li>
 * <li>Try reading based on the sector count (16 vs 13).</li>
 * </ol>
 */
public class NibbleScanner {
    public static Optional<DiskMarker[]> identify(NibbleTrackReaderWriter trackReaderWriter) {
        final int tracksOnDevice = trackReaderWriter.getTracksOnDevice();

        DiskMarker[] diskMarkers = new DiskMarker[tracksOnDevice];
        int successCount = 0;
        for (int track=0; track<tracksOnDevice; track++) {
            DataBuffer trackData = trackReaderWriter.readTrackData(track);
            // Generate a list of likely address prologs (that meet the expected 4&4 encodings and structure)
            Map<Integer, Set<Integer>> addressPrologs = findAddressPrologs(track, trackData);
            final int foundSectors = addressPrologs.values().stream().map(Collection::size).max(Integer::compareTo).orElseThrow();
            // Assumption: Track is "mostly" normal, so 5&3 or 6&2 along with expected 13- and 16-sector sizing:
            // (except for track 0 since it's possibly a mix of "expected" prolog and protected prolog bytes)
            final int sectorsOnTrack;
            if (foundSectors >= 10 && foundSectors <= 13) {
                sectorsOnTrack = 13;
            }
            else if (foundSectors > 13 && foundSectors <= 16) {
                sectorsOnTrack = 16;
            }
            else {
                // Expecting same sector count -- fill in DiskMarker just for sanity
                diskMarkers[track] = DiskMarker.disk525sector13();
                break;
            }
            // Find the one that matches our expected sectors per track and then capture possible address prolog bytes:
            final int addressProlog = addressPrologs.entrySet().stream()
                    .filter(e -> e.getValue().size() == foundSectors)
                    .map(Map.Entry::getKey)
                    .findFirst().orElseThrow();
            // Scan from an address prolog, look for (self?) sync bytes, and then assume next 3 are data prolog
            NibbleDiskCodec nibbleDiskCodec = foundSectors == 13 ? new Nibble53Disk525Codec() : new Nibble62Disk525Codec();
            int dataProlog = findDataProlog(addressProlog, trackData, nibbleDiskCodec);
            // Save (always, prevent easy NPE's in other code)
            final int addr1 = (addressProlog >> 16) & 0xff;
            final int addr2 = (addressProlog >> 8) & 0xff;
            final int addr3 = addressProlog & 0xff;
            final int data1 = (dataProlog >> 16) & 0xff;
            final int data2 = (dataProlog >> 8) & 0xff;
            final int data3 = dataProlog & 0xff;
            diskMarkers[track] = DiskMarker.build(sectorsOnTrack)
                    .addressProlog(addr1, addr2, addr3).addressEpilog()
                    .dataProlog(data1, data2, data3).dataEpilog()
                    .get();
            // Only tally good ones
            if (dataProlog != 0) {
                successCount++;
            }
        }
        // Somewhat arbitrary on number of tracks we think where successful...
        return successCount > 5 ? Optional.of(diskMarkers) : Optional.empty();
    }

    /**
     * Look for likely address prologs on disk. Returns raw results in case there is more than one
     * possibility and to identify likely sectors per track.
     * <p/>
     * Generate strategy is to look for byte sequence <code>P1 P2 P3 VX VY TX TY SX SY CX CY E1 E2 E3</code>
     * where 1,2,3 = prolog/epilog bytes, X,Y = 4+4 encoded data, and V,T,S,C identify volume, track, sector,
     * and checksum.
     */
    public static Map<Integer,Set<Integer>> findAddressPrologs(int track, DataBuffer trackData) {
        Map<Integer, Set<Integer>> addressPrologs = new HashMap<>();
        final int headerLength = 14;
        final int trackLength = trackData.limit();
        for (int i = 5; i < trackLength + headerLength; i++) {
            int trk = decodeOddEven(trackData, i % trackLength);
            if (trk == track) {
                int sct = decodeOddEven(trackData, (i + 2) % trackLength);
                if (sct < 16) { // likely sector number range
                    DataBuffer header = DataBuffer.create(headerLength);
                    for (int j = 0; j < headerLength; j++) {    // allow wrap to occur
                        header.putByte(j, trackData.getUnsignedByte((i - 5 + j) % trackLength));
                    }
                    int prolog = header.getUnsignedByte(0) << 16 | header.getUnsignedByte(1) << 8
                            | header.getUnsignedByte(2);
                    addressPrologs.merge(prolog, new TreeSet<>(Set.of(sct)), (l, n) -> {
                        l.addAll(n);
                        return l;
                    });
                }
            }
        }
        return addressPrologs;
    }

    /**
     * Scan from an address prolog, look for sync bytes, and then assume next 3 are data prolog.
     * Note that this makes a bunch of assumptions!
     */
    public static int findDataProlog(int addressProlog, DataBuffer trackData, NibbleDiskCodec nibbleDiskCodec) {
        final int trackLength = trackData.limit();
        final int addr1 = (addressProlog >> 16) & 0xff;
        final int addr2 = (addressProlog >> 8) & 0xff;
        final int addr3 = addressProlog & 0xff;
        int pos = 0;
        int[] readTranslateTable = nibbleDiskCodec.readTranslateTable();
        while (pos < trackLength+40) {
            if (trackData.getUnsignedByte(pos % trackLength) == addr1
                    && trackData.getUnsignedByte((pos+1) % trackLength) == addr2
                    && trackData.getUnsignedByte((pos+2) % trackLength) == addr3) {
                pos = pos + 3 + 8 + 2;  // prolog(3) + address field(8) + epilog(2)
                int syncByte = 0;       // the sync byte is not always 0xff... trying to be somewhat lenient
                int syncCount = 0;
                for (int i=pos; i<pos+40; i++) {
                    int b = trackData.getUnsignedByte(i % trackLength);
                    if (b == syncByte) {
                        syncCount++;
                    }
                    else if (syncCount >= 4) {
                        // Run the checksum algorithm; since we're not really decoding, we just include the
                        // checksum byte as well:
                        int checksum = 0;
                        for (int j=0; j<nibbleDiskCodec.encodedSize()+1; j++) {
                            // we need to look past the 3 data prolog bytes
                            int b2 = trackData.getUnsignedByte((i+3+j) % trackLength);
                            checksum ^= readTranslateTable[b2];     // XOR
                        }
                        if (checksum == 0) {
                            // Only keep it if the checksum matched
                            int data1 = trackData.getUnsignedByte(i % trackLength);
                            int data2 = trackData.getUnsignedByte((i + 1) % trackLength);
                            int data3 = trackData.getUnsignedByte((i + 2) % trackLength);
                            return data1 << 16 | data2 << 8 | data3;
                        }
                        else {
                            syncByte = b;
                            syncCount = 1;
                        }
                    }
                    else {
                        syncByte = b;
                        syncCount = 1;
                    }
                }
            }
            pos++;
        }
        return 0;
    }
}
