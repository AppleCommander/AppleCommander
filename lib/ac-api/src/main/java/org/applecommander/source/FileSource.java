package org.applecommander.source;

import org.applecommander.capability.Capability;
import org.applecommander.util.DataBuffer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSource implements Source {
    private DataBuffer buffer;

    public FileSource(Path path) {
        try {
            this.buffer = DataBuffer.wrap(Files.readAllBytes(path));
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
    public DataBuffer readBytes(int offset, int length) {
        return buffer.slice(offset, length);
    }

    @Override
    public void writeBytes(int offset, DataBuffer data) {
        buffer.put(offset, data);
    }
}
