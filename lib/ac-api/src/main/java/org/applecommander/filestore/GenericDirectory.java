package org.applecommander.filestore;

import org.applecommander.capability.Capability;

import java.util.List;
import java.util.Optional;

/// Provides a generic `Directory` structure for simple `FileStore`s.
public class GenericDirectory<FS extends FileStore> implements Directory {
    private final FS fileStore;
    private final List<FileEntry> files;

    public GenericDirectory(FS fileStore, List<FileEntry> files) {
        this.fileStore = fileStore;
        this.files = files;
    }

    /// Get the parent directory. If this is the root directory, it returns empty.
    @Override
    public Optional<Directory> getParent() {
        return Optional.empty();
    }

    /// Returns the FileStore that this Directory belongs to.
    @Override
    public FS getFileStore() {
        return fileStore;
    }

    /// Returns the name of this directory.
    @Override
    public String getName() {
        return fileStore.getLabel();
    }

    /// Return all entries that are stored in this directory.
    @Override
    public List<FileEntry> getFiles() {
        return files;
    }

    @Override
    public boolean can(Capability capability) {
        return false;
    }
}
