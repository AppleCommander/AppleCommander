package org.applecommander.util;

import org.applecommander.codec.Nibble53Disk525Codec;
import org.applecommander.codec.Nibble62Disk525Codec;
import org.applecommander.codec.NibbleDiskCodec;
import org.applecommander.device.DiskMarker;
import org.applecommander.device.NibbleTrackReaderWriter;
import org.applecommander.device.TrackSectorNibbleDevice;
import org.applecommander.image.NibbleImage;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;

import java.nio.file.Path;
import java.util.*;

/**
 * Note - This appears to be a rather successful algorithm.
 * 1. Locate address prolog bytes by simply looking for the track number and a sensible sector
 *    using 4&4 encoding.
 * 2. From that prolog (per track), start from end of address prolog and scan for sync bytes.
 * 3. Try reading based on the sector count (16 vs 13).
 * Only good for protected DOS disks. Note that the catalog is likely moved, leaving the locating
 * that for later.
 */
public class NibbleScanner2 {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please include filenames as arguments.");
            System.exit(0);
        }

        for (String filename : args) {
            if (!filename.toLowerCase().contains(".nib")) {
                System.err.printf("Must be a *.nib image: %s\n", filename);
                continue;
            }
            Optional<Source> optionalSource = Sources.create(Path.of(filename));
            if (optionalSource.isEmpty()) {
                System.err.printf("Unable to load file: %s\n", filename);
                continue;
            }
            NibbleTrackReaderWriter trackReaderWriter = new NibbleImage(optionalSource.get());
            int expectedOffset = 0;
            for (int track=0; track<trackReaderWriter.getTracksOnDevice(); track++) {
                DataBuffer trackData = trackReaderWriter.readTrackData(track);
                // Look for P1 P2 P3 VX VY TX TY SX SY CX CY E1 E2 E3
                // (1,2,3 = prolog/epilog bytes; X,Y = 4+4 encoded data)
                System.out.printf("Track #%d (offset = %d ($%06x)\n", track, expectedOffset, expectedOffset);
                expectedOffset += trackData.limit();
                Map<Integer, Set<Integer>> addressPrologs = new HashMap<>();
                final int headerLength = 14;
                for (int i = 5; i < trackData.limit() + headerLength; i++) {  // can't handle negatives, over-wrap it then
                    int trk = decodeOddEven(trackData, i);
                    if (trk == track) {
                        int sct = decodeOddEven(trackData, i + 2);
                        if (sct < 16) { // likely sector number range
                            DataBuffer header = DataBuffer.create(headerLength);
                            final int trackLength = trackData.limit();
                            for (int j = 0; j < headerLength; j++) {    // allow wrap to occur
                                header.putByte(j, trackData.getUnsignedByte((i - 5 + j) % trackLength));
                            }
                            //System.out.printf("\tPossible header (T%02d,S%02d): %s\n", trk, sct, dump(header));
                            int prolog = header.getUnsignedByte(0) << 16 | header.getUnsignedByte(1) << 8 | header.getUnsignedByte(2);
                            addressPrologs.merge(prolog, new TreeSet<>(Set.of(sct)), (l, n) -> {
                                l.addAll(n);
                                return l;
                            });
                        }
                    }
                }
                final int foundSectors = addressPrologs.values().stream().map(Collection::size).max(Integer::compareTo).orElseThrow();
//                addressPrologs.forEach((p, s) -> {
//                    System.out.printf("\tProlog %06x - sectors = %s %s\n", p, s,
//                            s.size() == foundSectors ? "*** MATCH ***" : "");
//                });
                // Find the one that matches our expected 13 sectors per track and then capture possible address prolog bytes
                // And see how many match 13
                int addressProlog = addressPrologs.entrySet().stream()
                        .filter(e -> e.getValue().size() == foundSectors)
                        .map(Map.Entry::getKey)
                        .findFirst().orElseThrow();
                // Scan from an address prolog, look for (self?) sync bytes, and then assume next 3 are data prolog
                int pos = 0;
                final int trackLength = trackData.limit();
                final int addr1 = (addressProlog >> 16) & 0xff;
                final int addr2 = (addressProlog >> 8) & 0xff;
                final int addr3 = addressProlog & 0xff;
                DiskMarker diskMarker = null;
                while (pos < trackLength+40) {
                    if (trackData.getUnsignedByte(pos % trackLength) == addr1
                            && trackData.getUnsignedByte((pos+1) % trackLength) == addr2
                            && trackData.getUnsignedByte((pos+2) % trackLength) == addr3) {
                        int v = decodeOddEven(trackData, pos+3);
                        int t = decodeOddEven(trackData, pos+5);
                        int s = decodeOddEven(trackData, pos+7);
                        System.out.printf("\tT%02d,S%02d,V%03d - offset %d ($%04x)\n", t, s, v, pos, pos);
                        System.out.printf("\t\t--> address prolog: %02x %02x %02x\n", addr1, addr2, addr3);
                        pos = pos + 8;
                        int syncByte = 0;
                        int syncCount = 0;
                        for (int i=pos; i<pos+40; i++) {
                            int b = trackData.getUnsignedByte(i % trackLength);
                            if (b == syncByte) {
                                syncCount++;
                            }
                            else if (syncCount >= 4) {
                                int data1 = trackData.getUnsignedByte(i % trackLength);
                                int data2 = trackData.getUnsignedByte((i+1) % trackLength);
                                int data3 = trackData.getUnsignedByte((i+2) % trackLength);
                                System.out.printf("\t\t--> data prolog: %02x %02x %02x\n", data1, data2, data3);
                                diskMarker = DiskMarker.build()
                                        .addressProlog(addr1, addr2, addr3).addressEpilog()
                                        .dataProlog(data1, data2, data3).dataEpilog()
                                        .get();
                                break;
                            }
                            else {
                                syncByte = b;
                                syncCount = 1;
                            }
                        }
                        if (diskMarker != null) {
                            break;
                        }
                        System.out.println("\t\t--> data prolog: UNKNOWN");
                    }
                    pos++;
                }
                if (diskMarker != null) {
                    int sectorsPerTrack = 16;
                    NibbleDiskCodec nibbleDiskCodec = new Nibble62Disk525Codec();
                    if (foundSectors <= 13) {
                        sectorsPerTrack = 13;
                        nibbleDiskCodec = new Nibble53Disk525Codec();
                    }
                    TrackSectorNibbleDevice device = new TrackSectorNibbleDevice(trackReaderWriter, diskMarker,
                            nibbleDiskCodec, sectorsPerTrack);
                    for (int sector=0; sector < sectorsPerTrack; sector++) {
                        try {
                            DataBuffer buf = device.readSector(track, sector);
                            if (buf != null) dumpSector(track, sector, buf);
                        } catch (RuntimeException e) {
                            // just ignoring read errors
                            System.out.printf("\tTrack #%d, Sector %d -> %s\n", track, sector, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public static String dump(DataBuffer data) {
        StringBuilder sb = new StringBuilder();
        while (data.hasRemaining()) {
            int b = data.readUnsignedByte();
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }

    public static void dumpSector(int track, int sector, DataBuffer buf) {
        System.out.printf("\tTrack #%d, Sector %d ->\n", track, sector);
        for (int addr = 0; addr < 256; addr += 16) {
            System.out.printf("\t\t%03x- ", addr);
            StringBuilder str = new StringBuilder(16);
            for (int i=0; i<16; i++) {
                int b = buf.getUnsignedByte(addr+i);
                System.out.printf("%02x ", b);
                char ch = (char)(b & 0x7f);
                if (Character.isISOControl(ch)) {
                    ch = '.';
                }
                str.append(ch);
            }
            System.out.printf(" |%s|\n", str);
        }
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
    public static int decodeOddEven(DataBuffer rawData, int offset) {
        int b1 = rawData.getUnsignedByte(offset % rawData.limit());
        int b2 = rawData.getUnsignedByte((offset+1) % rawData.limit());
        return (b1 << 1 | 0x01) & b2;
    }

    /**
     * Encode odd-even bytes to be stored on disk.  See decodeOddEven
     * for the format.
     * @see #decodeOddEven(DataBuffer, int)
     */
    public static void encodeOddEven(DataBuffer data, int offset, int value) {
        int b1 = (value >> 1) | 0xaa;
        int b2 = value | 0xaa;
        data.putBytes(offset, b1, b2);
    }
}
