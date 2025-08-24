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
        return fromSource(source, 0);
    }

    public Optional<Source> fromSource(Source source, int requestedBlockSize) {
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
                    byte[] imageData = unpackSHKFile(source.getName(), source, requestedBlockSize);
                    Source shkSource = DataBufferSource.create(imageData, source.getName() + ".po")
                            .hints(Hint.PRODOS_BLOCK_ORDER, Hint.ORIGIN_SHRINKIT)
                            .information(Information.builder("Original name").value(source.getName()),
                                         Information.builder("Original type").value(bxy ? "Binary II" : "Shrinkit"))
                            .changed(true)
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
