package com.webcodepro.applecommander.storage;

import org.applecommander.hint.Hint;
import org.applecommander.source.DataBufferSource;
import org.applecommander.source.Source;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.webcodepro.shrinkit.io.ByteConstants.*;
import static com.webcodepro.applecommander.util.ShrinkItUtilities.*;

public class ShrinkitSourceFactory implements Source.Factory {
    @Override
    public Optional<Source> fromObject(Object object) {
        return Optional.empty();
    }

    @Override
    public Optional<Source> fromSource(Source source) {
        final int sampleSize = 128;
        final int requiredBytes = IntStream.of(NUFILE_ID.length, NUFX_ID.length, BXY_ID.length).max().getAsInt();
        if (source.getSize() >= sampleSize) {
            DataBuffer magic = source.readBytes(0, sampleSize);
            boolean nufile = false;
            boolean nufx = false;
            boolean bxy = false;
            for (int i=0; !nufile && !nufx && !bxy && i<sampleSize-requiredBytes; i++) {
                nufile = magic.matches(i, NUFILE_ID);
                nufx = magic.matches(i, NUFX_ID);
                bxy = magic.matches(i, BXY_ID);
            }
            if (nufile || nufx || bxy) {
                try {
                    byte[] imageData = unpackSHKFile(source.getName(), source, 0);
                    Source shkSource = DataBufferSource.create(imageData, source.getName() + ".po")
                            .hints(Hint.PRODOS_BLOCK_ORDER)
                            .information(Information.builder("Original name").value(source.getName()),
                                         Information.builder("Original type").value(bxy ? "Binary II" : "Shrinkit"))
                            .get();
                    return Optional.of(shkSource);
                } catch (IOException ex) {
                    // ignore error
                }
            }
        }
        return Optional.empty();
    }
}
