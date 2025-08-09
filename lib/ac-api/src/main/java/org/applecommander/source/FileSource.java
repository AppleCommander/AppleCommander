package org.applecommander.source;

import org.applecommander.capability.Capability;
import org.applecommander.util.DataBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public class FileSource implements Source {
    private Path path;
    private DataBuffer buffer;
    private boolean changed;

    public FileSource(Path path) {
        try {
            this.path = path;
            byte[] rawData = Files.readAllBytes(path);
            this.buffer = DataBuffer.wrap(rawData);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    public FileSource(Path path, DataBuffer buffer) {
        this.path = path;
        this.buffer = buffer;
    }
    // TODO should this be separate or is "FileSource" a misnomer?
    public FileSource(DataBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean can(Capability capability) {
        // TODO - we haven't actually identified save yet!
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        if (iface.isInstance(buffer)) {
            return Optional.of(iface.cast(this));
        }
        return Optional.empty();
    }

    @Override
    public int getSize() {
        return buffer.limit();
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
}
