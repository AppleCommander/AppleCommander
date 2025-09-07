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
package org.applecommander.source;

import org.applecommander.capability.Capability;
import org.applecommander.hint.Hint;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Creates and manages a Source based upon a file in the filesystem.
 */
public class FileSource implements Source {
    private Path path;
    private String filename;
    private int compressedSize = -1;
    private DataBuffer buffer;
    private boolean changed;

    public FileSource(Path path) {
        try {
            this.path = path;
            this.filename = path.toString();
            byte[] rawData = Files.readAllBytes(path);
            this.buffer = DataBuffer.wrap(rawData);
            if (this.buffer.getUnsignedShort(0) == GZIPInputStream.GZIP_MAGIC) {
                try (
                    GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(rawData));
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ) {
                    inputStream.transferTo(outputStream);
                    this.compressedSize = this.buffer.limit();  // before we rewrite it
                    this.buffer = DataBuffer.wrap(outputStream.toByteArray());
                } catch (Throwable ignored) {
                    // assuming that we somehow mis-interpreted the magic bytes
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public boolean can(Capability capability) {
        // TODO - we haven't actually identified save yet!
        return false;
    }

    @Override
    public boolean is(Hint hint) {
        // We know nothing!
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, buffer);
    }

    @Override
    public int getSize() {
        return buffer.limit();
    }

    @Override
    public String getName() {
        return filename;
    }

    @Override
    public DataBuffer readAllBytes() {
        return buffer;
    }

    @Override
    public DataBuffer readBytes(int offset, int length) {
        return buffer.slice(offset, length);
    }

    @Override
    public void writeBytes(int offset, DataBuffer data) {
        buffer.put(offset, data);
        changed = true;
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    @Override
    public void clearChanges() {
        changed = false;
    }

    @Override
    public List<Information> information() {
        List<Information> list = new ArrayList<>();
        list.add(Information.builder("File Path").value(path.toString()));
        if (compressedSize != -1) {
            list.add(Information.builder("Size (*.gz)").value(compressedSize));
        }
        list.add(Information.builder("Size").value(buffer.limit()));
        return list;
    }

    public static class Factory implements Source.Factory {
        @Override
        public Optional<Source> fromObject(Object object) {
            return switch(object) {
                case Path path -> Optional.of(new FileSource(path));
                case File file -> Optional.of(new FileSource(file.toPath()));
                case String filename -> Optional.of(new FileSource(Path.of(filename)));
                default -> Optional.empty();
            };
        }

        @Override
        public Optional<Source> fromSource(Source source) {
            return Optional.empty();
        }
    }
}
