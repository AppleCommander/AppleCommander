/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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

import com.webcodepro.applecommander.storage.os.prodos.ProdosDirectoryEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import org.applecommander.capability.Capability;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.DisplayColumn;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.util.Container;

import java.util.*;
import java.util.function.Function;

/**
 * The DiskFileStoreAdapter is a shim that allows FormattedDisk to be mapped into the
 * new/evolving FileStore interface(s).
 */
public class DiskFileStoreAdapter implements FileStore {
    private final FormattedDisk disk;

    public DiskFileStoreAdapter(FormattedDisk disk) {
        this.disk = disk;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, disk);
    }

    @Override
    public String getLabel() {
        return disk.getDiskName();
    }

    @Override
    public DiskDirectoryAdapter getRootDirectory() {
        return new DiskDirectoryAdapter(this, null, disk);
    }

    @Override
    public String getPathSeparator() {
        return disk instanceof ProdosFormatDisk ? "/" : "";
    }

    @Override
    public boolean can(Capability capability) {
        return switch (capability) {
            case CREATE_FILES -> disk.canCreateFile();
            case DELETE_FILES -> disk.canDeleteFile();
            case CREATE_DIRECTORIES -> disk.canCreateDirectories();
            case SUPPORTS_DIRECTORIES -> disk.canHaveDirectories();
            case WRITE_FILES -> disk.canWriteFileData();
            default -> false;
        };
    }

    /**
     * Translate the "legacy" FormattedDisk FileColumnHeader to the new DisplayColumn structure.
     * Note that we need to track what has been seen as well as where it comes from and the index
     * values in order to replicate most of the capability. This does not solve for type since the
     * old API pre-formats everything as a String.
     */
    @Override
    public List<DisplayColumn> getDisplayColumns() {
        List<DisplayColumn> columns = new ArrayList<>();
        Set<String> alreadySeenKeys = new HashSet<>();
        for (int displayMode : List.of(FormattedDisk.FILE_DISPLAY_NATIVE,
                                       FormattedDisk.FILE_DISPLAY_DETAIL)) {
            List<FormattedDisk.FileColumnHeader> headers = disk.getFileColumnHeaders(displayMode);
            DisplayColumn.Mode mode = DisplayColumn.Mode.DETAIL;
            if (displayMode == FormattedDisk.FILE_DISPLAY_NATIVE) {
                mode = DisplayColumn.Mode.NATIVE;
            };
            for (int i = 0; i<headers.size(); i++) {
                var header = headers.get(i);
                DisplayColumn.Alignment alignment = switch(header.getAlignment()) {
                    case FormattedDisk.FileColumnHeader.ALIGN_CENTER -> DisplayColumn.Alignment.CENTER;
                    case FormattedDisk.FileColumnHeader.ALIGN_RIGHT -> DisplayColumn.Alignment.RIGHT;
                    default -> DisplayColumn.Alignment.LEFT;
                };
                if (!alreadySeenKeys.contains(header.getKey())) {
                    alreadySeenKeys.add(header.getKey());
                    final int headerIndex = i;
                    Function<FileEntry,String> mappingFn = entry -> {
                        // TODO investigate to see if generics work across these interfaces
                        if (entry instanceof DiskFileEntryAdapter fileEntryAdapter) {
                            return fileEntryAdapter.fileEntry.getFileColumnData(displayMode).get(headerIndex);
                        }
                        throw new RuntimeException("Unexpected file entry type: " + entry.getClass().getName());
                    };
                    DisplayColumn displayColumn = new DisplayColumn(header.getTitle(),
                            alignment, mappingFn::apply, "%s", mode);
                    columns.add(displayColumn);
                }
            }
        }
        return columns;
    }

    /**
     * The DiskFileEntryAdapter is a shim that allows a FileEntry to be mapped into the
     * new/evolving FileEntry interface(s).
     */
    public static class DiskFileEntryAdapter implements FileEntry {
        private final DiskFileStoreAdapter adapter;
        private final DiskDirectoryAdapter parent;
        private final com.webcodepro.applecommander.storage.FileEntry fileEntry;
        private final DiskDirectoryAdapter subdirectory;

        public DiskFileEntryAdapter(DiskFileStoreAdapter adapter, DiskDirectoryAdapter parent,
                                    com.webcodepro.applecommander.storage.FileEntry fileEntry) {
            this.adapter = adapter;
            this.parent = parent;
            this.fileEntry = fileEntry;
            if (fileEntry instanceof ProdosDirectoryEntry prodosDirectoryEntry) {
                this.subdirectory = new DiskDirectoryAdapter(adapter, parent, prodosDirectoryEntry);
            }
            else {
                this.subdirectory = null;
            }
        }
        @Override
        public DiskDirectoryAdapter getParent() {
            return parent;
        }
        @Override
        public FileStore getFileStore() {
            return adapter;
        }
        @Override
        public <T> Optional<T> get(Class<T> iface) {
            return Container.get(iface, subdirectory);
        }
        @Override
        public byte[] getDataFork() {
            return fileEntry.getFileData();
        }
        @Override
        public byte[] getResourceFork() {
            throw new RuntimeException("Not supported by the legacy AppleCommander.");
        }
        @Override
        public void setDataFork(byte[] fileData) {
            try {
                fileEntry.setFileData(fileData);
            } catch (DiskFullException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public void setResourceFork(byte[] data) {
            throw new RuntimeException("Not supported by the legacy AppleCommander.");
        }
        @Override
        public boolean isDeleted() {
            return fileEntry.isDeleted();
        }
        @Override
        public String getName() {
            return fileEntry.getFilename();
        }
        @Override
        public void setName(String name) {
            fileEntry.setFilename(name);
        }
        @Override
        public int getSize() {
            return fileEntry.getSize();
        }
        @Override
        public String getFiletype() {
            return fileEntry.getFiletype();
        }
    }
    /**
     * The DiskDirectoryEntryAdapter is a shim that allows a FormattedDisk or DirectoryEntry to be mapped into the
     * new/evolving DirectoryEntry interface(s).
     */
    public static class DiskDirectoryAdapter implements Directory {
        private final DiskFileStoreAdapter adapter;
        private final DiskDirectoryAdapter parent;
        private final com.webcodepro.applecommander.storage.DirectoryEntry directoryEntry;

        public DiskDirectoryAdapter(DiskFileStoreAdapter adapter, DiskDirectoryAdapter parent,
                                    com.webcodepro.applecommander.storage.DirectoryEntry directoryEntry) {
            this.adapter = adapter;
            this.parent = parent;
            this.directoryEntry = directoryEntry;
        }
        @Override
        public Optional<Directory> getParent() {
            return Optional.ofNullable(parent);
        }
        @Override
        public List<FileEntry> getFiles() {
            try {
                List<FileEntry> entries = new ArrayList<>();
                for (var file : directoryEntry.getFiles()) {
                    entries.add(new DiskFileEntryAdapter(adapter, parent, file));
                }
                return entries;
            } catch (DiskException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public DiskFileStoreAdapter getFileStore() {
            return adapter;
        }
    }
}

