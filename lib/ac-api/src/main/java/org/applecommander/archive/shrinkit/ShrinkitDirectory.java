package org.applecommander.archive.shrinkit;

import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;

import java.util.List;
import java.util.Optional;

public class ShrinkitDirectory implements Directory {
    private final ShrinkitFileStore fileStore;
    private final List<FileEntry> files;

    public ShrinkitDirectory(ShrinkitFileStore fileStore, List<FileEntry> files) {
        this.fileStore = fileStore;
        this.files = files;
    }

    @Override
    public Optional<Directory> getParent() {
        return Optional.empty();
    }

    @Override
    public ShrinkitFileStore getFileStore() {
        return fileStore;
    }

    @Override
    public String getName() {
        return fileStore.getLabel();
    }

    @Override
    public List<FileEntry> getFiles() {
        return files;
    }
}
