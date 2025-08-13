package org.applecommander.util;

import org.applecommander.codec.Nibble53Disk525Codec;
import org.applecommander.device.DiskMarker;
import org.applecommander.device.NibbleTrackReaderWriter;
import org.applecommander.device.TrackSectorNibbleDevice;
import org.applecommander.image.NibbleImage;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NibbleScanner {
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
            for (int track=0; track<trackReaderWriter.getTracksOnDevice(); track++) {
                DataBuffer trackData = trackReaderWriter.readTrackData(track);
                int prologBytes = 0;
                Map<Integer,Integer> counts = new HashMap<>();
                while (trackData.hasRemaining()) {
                    int b = trackData.readUnsignedByte();
                    prologBytes &= 0xffff;
                    prologBytes <<= 8;
                    prologBytes |= b;
                    if ((prologBytes & 0x00800000) == 0) {
                        // Still loading
                        continue;
                    }
                    counts.merge(prologBytes, 1, Integer::sum);
                }
                System.out.printf("Track #%d possible prolog bytes:\n", track);
                counts.forEach((prolog,count) -> {
                    if (count == 13 || count == 16) {
                        System.out.printf("\t%06x: %d\n", prolog, count);
                    }
                });
                DiskMarker diskMarker = switch(track) {
                    // data prolog:
                    // 1: unknown
                    // 2: 0xaa (constant?)
                    // 3: odd = 0xfd, even = 0xed?
                    case 0 -> DiskMarker.disk525sector13();
                    case 1 -> DiskMarker.build()
                                .addressProlog(0xdf, 0xaa, 0xb7).addressEpilog()
                                .dataProlog(0xdf, 0xaa, 0xfd).dataEpilog()
                                .get();
                    case 2 -> DiskMarker.build()
                            .addressProlog(0xdd, 0xaa, 0xf5).addressEpilog()
                            .dataProlog(0xd7, 0xaa, 0xfd).dataEpilog()
                            .get();
                    case 3 -> DiskMarker.build()
                            .addressProlog(0xdd, 0xaa, 0xf7).addressEpilog()
                            .dataProlog(0xdf, 0xaa, 0xfd).dataEpilog()
                            .get();
                    case 4 -> DiskMarker.build()
                            .addressProlog(0xdf, 0xaa, 0xf5).addressEpilog()
                            .dataProlog(0xdf, 0xaa, 0xbd).dataEpilog()
                            .get();
                    case 5 -> DiskMarker.build()
                            .addressProlog(0xdd, 0xaa, 0xf5).addressEpilog()
                            .dataProlog(0xdd, 0xaa, 0xfd).dataEpilog()
                            .get();
                    case 6 -> DiskMarker.build()
                            .addressProlog(0xdf, 0xaa, 0xf7).addressEpilog()
                            .dataProlog(0xdd, 0xaa, 0xed).dataEpilog()
                            .get();
                    case 16 -> DiskMarker.build()
                            .addressProlog(0xdd, 0xaa, 0xf5).addressEpilog()
                            .dataProlog(0xdf, 0xaa, 0xed).dataEpilog()
                            .get();
                    case 17 -> DiskMarker.build()
                                .addressProlog(0xdf, 0xaa, 0xb7).addressEpilog()
                                .dataProlog(0xd7, 0xaa, 0xfd).dataEpilog()
                                .get();
                    case 18 -> DiskMarker.build()
                            .addressProlog(0xdf, 0xaa, 0xf5).addressEpilog()
                            .dataProlog(0xdf, 0xaa, 0xed).dataEpilog()
                            .get();
                    default -> null;
                };
                if (track == 7) {
                    dumpSector(track, -1, trackData);
                }
                if (diskMarker != null) {
                    TrackSectorNibbleDevice device = new TrackSectorNibbleDevice(trackReaderWriter, diskMarker, new Nibble53Disk525Codec(), 13);
                    for (int sector=0; sector < 13; sector++) {
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

    public static void dumpSector(int track, int sector, DataBuffer buf) {
        System.out.printf("\tTrack #%d, Sector %d ->\n", track, sector);
        int addr = 0;
        while (addr < buf.limit()) {
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
            addr+= 16;
        }
    }
}
