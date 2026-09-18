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

import com.webcodepro.applecommander.storage.os.cpm.CpmFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFormatDisk;
import com.webcodepro.applecommander.storage.os.nakedos.NakedosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosDirectoryEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk;
import org.applecommander.capability.Capability;
import org.applecommander.filestore.*;
import org.applecommander.filestore.FileEntry;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.SectorUsage;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The DiskFileStoreAdapter is a shim that allows FormattedDisk to be mapped into the
 * new/evolving FileStore interface(s).
 */
public class DiskFileStoreAdapter implements FileStore {
    private final FormattedDisk disk;
    private final DiskUsage usage;

    public DiskFileStoreAdapter(FormattedDisk disk) {
        this.disk = disk;
        // Prep the DiskUsage shims
        this.usage = switch (disk) {
            case DosFormatDisk dos -> {
                final byte[] vtoc = dos.readVtoc();
                yield new SectorUsage(dos::getUsedSectors, dos::getFreeSectors,
                        (t,s) -> dos.isSectorUsed(t, s, vtoc),
                        dos.getTracks(), dos.getSectors());
            }
            case ProdosFormatDisk prodos -> {
                final byte[] bitmap = prodos.readVolumeBitMap();
                yield new BlockUsage(DiskConstants.BLOCK_SIZE, prodos::getUsedBlocks,
                        prodos::getFreeBlocks, (b) -> prodos.isBlockUsed(bitmap, b));
            }
            // Current implementation of GutenbergFormatDisk and NakedosFormatDisk simply marks everything as used.
            case GutenbergFormatDisk gutenberg -> new SectorUsage(gutenberg::getUsedSectors,
                    gutenberg::getFreeSectors, (_,_) -> true,
                    gutenberg.getTracks(), gutenberg.getSectors());
            case NakedosFormatDisk nakedos -> new SectorUsage(nakedos::getUsedSectors,
                    nakedos::getFreeSectors, (_,_) -> true,
                    nakedos.getTracks(), nakedos.getSectors());
            // CP/M, Pascal, and RDOS all synthesize the bitmap. So we do too!
            case CpmFormatDisk cpm -> new BlockUsage(CpmFormatDisk.CPM_BLOCKSIZE, cpm::getBlocksUsed, cpm::getBlocksFree,
                    synthesizeBitmap(cpm));
            case PascalFormatDisk pascal -> new BlockUsage(DiskConstants.BLOCK_SIZE, pascal::getUsedBlocks,
                    pascal::getFreeBlocks, synthesizeBitmap(pascal));
            case RdosFormatDisk rdos -> new BlockUsage(DiskConstants.SECTOR_SIZE, rdos::getUsedBlocks, rdos::getFreeBlocks,
                    synthesizeBitmap(rdos));
            default -> throw new IllegalArgumentException("Unsupported format disk");
        };
    }

    /**
     * This is a helper method to make a copy of the legacy DiskUsage into a BitSet for the shim.
     */
    private Function<Integer,Boolean> synthesizeBitmap(FormattedDisk formattedDisk) {
        final BitSet used = new BitSet(formattedDisk.getBitmapLength());
        int block = 0;
        FormattedDisk.DiskUsage diskUsage = formattedDisk.getDiskUsage();
        while (diskUsage.hasNext()) {
            diskUsage.next();
            used.set(block++, diskUsage.isUsed());
        }
        assert(block == disk.getBitmapLength());
        return used::get;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, disk, usage);
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
        return columns;
    }

    /**
     * The DiskFileEntryAdapter is a shim that allows a FileEntry to be mapped into the
     * new/evolving FileEntry interface(s).
     */
    public static class DiskFileEntryAdapter implements WritableFileEntry {
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
        public DataBuffer getDataFork() {
            return DataBuffer.wrap(fileEntry.getFileData());
        }
        @Override
        public Optional<DataBuffer> getResourceFork() {
            throw new RuntimeException("Not supported by the legacy AppleCommander.");
        }
        @Override
        public void setDataFork(DataBuffer fileData) {
            try {
                fileEntry.setFileData(fileData.asBytes());
            } catch (DiskFullException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public void setResourceFork(DataBuffer data) {
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
        public long getSize() {
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
        @Override
        public String getName() {
            return directoryEntry.getDirname();
        }
    }
}

