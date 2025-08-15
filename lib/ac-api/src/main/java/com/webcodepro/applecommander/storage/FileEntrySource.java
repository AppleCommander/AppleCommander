package com.webcodepro.applecommander.storage;

import org.applecommander.capability.Capability;
import org.applecommander.hint.Hint;
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.util.List;
import java.util.Optional;

public class FileEntrySource implements Source {
    private FileEntry fileEntry;
    private DataBuffer buffer;
    private boolean changed;

    public FileEntrySource(FileEntry fileEntry) {
        this.fileEntry = fileEntry;
        this.buffer = DataBuffer.wrap(fileEntry.getFileData());
    }

    @Override
    public boolean can(Capability capability) {
        // TODO?
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, fileEntry, buffer);
    }

    @Override
    public boolean is(Hint hint) {
        // Right now this is all SHK/SDK
        return hint == Hint.PRODOS_BLOCK_ORDER;
    }

    @Override
    public int getSize() {
        return fileEntry.getSize();
    }

    @Override
    public String getName() {
        return fileEntry.getFilename();
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
        return List.of();
    }

    public static class Factory implements Source.Factory {
        @Override
        public Optional<Source> fromObject(Object object) {
            if (object instanceof FileEntry fileEntry) {
                return Optional.of(new FileEntrySource(fileEntry));
            }
            return Optional.empty();
        }

        @Override
        public Optional<Source> fromSource(Source source) {
            return Optional.empty();
        }
    }
}
