package org.applecommander.util;

import org.applecommander.codec.Nibble53Disk525Codec;
import org.applecommander.codec.Nibble62Disk525Codec;
import org.applecommander.device.NibbleTrackReaderWriter;
import org.applecommander.image.NibbleImage;
import org.applecommander.source.Source;
import org.applecommander.source.Sources;

import java.nio.file.Path;
import java.util.*;

public class NibbleScanner3 {
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
                Set<Integer> nonStandardBytes53 = new TreeSet<>();
                Set<Integer> nonStandardBytes62 = new TreeSet<>();
                while (trackData.hasRemaining()) {
                    int b = trackData.readUnsignedByte();
                    if (Nibble53Disk525Codec.readTranslateTable53[b] == 0 && b != 0xab) {   // skip byte 0 manually
                        nonStandardBytes53.add(b);
                    }
                    if (Nibble62Disk525Codec.readTranslateTable62[b] == 0 && b != 0x96) {   // skip byte 0 manually
                        nonStandardBytes62.add(b);
                    }
                }
                System.out.printf("Track #%02d\n", track);
                System.out.print("\tNon-standard bytes (5&3): ");
                nonStandardBytes53.forEach(b -> System.out.printf("%02x ", b));
                System.out.println();
                System.out.print("\tNon-standard bytes (6&2): ");
                nonStandardBytes62.forEach(b -> System.out.printf("%02x ", b));
                System.out.println();
            }
        }
    }
}
