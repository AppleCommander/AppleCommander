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

import com.webcodepro.applecommander.storage.os.cpm.CpmFileEntry;
import com.webcodepro.applecommander.storage.os.cpm.CpmFormatDisk;
import com.webcodepro.applecommander.storage.os.dos33.DosFileEntry;
import com.webcodepro.applecommander.storage.os.dos33.DosFormatDisk;
import com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFileEntry;
import com.webcodepro.applecommander.storage.os.gutenberg.GutenbergFormatDisk;
import com.webcodepro.applecommander.storage.os.nakedos.NakedosFileEntry;
import com.webcodepro.applecommander.storage.os.nakedos.NakedosFormatDisk;
import com.webcodepro.applecommander.storage.os.pascal.PascalFileEntry;
import com.webcodepro.applecommander.storage.os.pascal.PascalFormatDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosDirectoryEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFormatDisk;
import com.webcodepro.applecommander.storage.os.rdos.RdosFileEntry;
import com.webcodepro.applecommander.storage.os.rdos.RdosFormatDisk;
import org.applecommander.capability.Capability;
import org.applecommander.device.BlockDevice;
import org.applecommander.device.Device;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.*;
import org.applecommander.filestore.FileEntry;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage;
import org.applecommander.usage.DiskUsage.UsageType;
import org.applecommander.usage.SectorUsage;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.FileMagic;
import org.applecommander.util.InformationGroup;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/// The `DiskFileStoreAdapter` is a shim that allows `FormattedDisk` to be mapped into the
/// new/evolving `FileStore` and associated interface(s).
/// The intent is that this is a short-term strategy.
/// @see FileStore
/// @see FormattedDisk
public class DiskFileStoreAdapter implements FileStore {
    private final FormattedDisk disk;
    private final DiskUsage usage;

    public DiskFileStoreAdapter(FormattedDisk disk) {
        this.disk = disk;
        // Prep the DiskUsage shims
        final Optional<TrackSectorDevice> trackSectorDevice = disk.get(TrackSectorDevice.class);
        final Optional<BlockDevice> blockDevice = disk.get(BlockDevice.class);
        this.usage = switch (disk) {
            case DosFormatDisk dos -> {
                final byte[] vtoc = dos.readVtoc();
                yield new SectorUsage(trackSectorDevice.orElseThrow().getGeometry(),
                        (t,s) -> dos.isSectorUsed(t, s, vtoc) ? UsageType.USED : UsageType.FREE);
            }
            case ProdosFormatDisk prodos -> {
                final byte[] bitmap = prodos.readVolumeBitMap();
                yield new BlockUsage(blockDevice.orElseThrow().getGeometry(),
                        b -> prodos.isBlockUsed(bitmap, b) ? UsageType.USED : UsageType.FREE);
            }
            // Current implementation of GutenbergFormatDisk and NakedosFormatDisk simply marks everything as used.
            case GutenbergFormatDisk gutenberg -> new SectorUsage(trackSectorDevice.orElseThrow().getGeometry(),
                    (_,_) -> UsageType.USED);
            case NakedosFormatDisk nakedos -> new SectorUsage(trackSectorDevice.orElseThrow().getGeometry(),
                    (_,_) -> UsageType.USED);
            // CP/M, Pascal, and RDOS all synthesize the bitmap. So we do too!
            case CpmFormatDisk cpm -> new BlockUsage(blockDevice.orElseThrow().getGeometry(), synthesizeBitmap(cpm));
            case PascalFormatDisk pascal -> new BlockUsage(blockDevice.orElseThrow().getGeometry(), synthesizeBitmap(pascal));
            case RdosFormatDisk rdos -> new BlockUsage(blockDevice.orElseThrow().getGeometry(), synthesizeBitmap(rdos));
            default -> throw new IllegalArgumentException("Unsupported format disk");
        };
    }

    /// This is a helper method to make a copy of the legacy `DiskUsage` into a `BitSet` for the shim.
    private Function<Integer,UsageType> synthesizeBitmap(FormattedDisk formattedDisk) {
        final BitSet used = new BitSet(formattedDisk.getBitmapLength());
        int block = 0;
        FormattedDisk.DiskUsage diskUsage = formattedDisk.getDiskUsage();
        while (diskUsage.hasNext()) {
            diskUsage.next();
            used.set(block++, diskUsage.isUsed());
        }
        assert(block == disk.getBitmapLength());
        return b -> used.get(b) ? UsageType.USED : UsageType.FREE;
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

    /// Translate the "legacy" `FormattedDisk` `FileColumnHeader` to the new `DisplayColumn` structure.
    /// Note that we need to track what has been seen as well as where it comes from and the index
    /// values in order to replicate most of the capability. This does not solve for type since the
    /// old API pre-formats everything as a String.
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
                        alignment, DisplayColumn.DataType.STRING, mappingFn::apply, null, false, "%s", mode);
                columns.add(displayColumn);
            }
        }
        return columns;
    }

    @Override
    public List<InformationGroup> information() {
        InformationGroup.Builder builder = InformationGroup.builder("File Store Adapter");
        // Transformation
        for (FormattedDisk.DiskInformation info : disk.getDiskInformation()) {
            builder.item(info.getLabel()).value(info.getValue());
        }
        // All of these have a Device, so share it as well
        return builder.get(disk.get(Device.class).orElseThrow());
    }

    /// The `DiskFileEntryAdapter` is a shim that allows a `com.webcodepro.applecommander.storage.FileEntry`
    /// to be mapped into the new/evolving `FileEntry` / `WritableFileEntry` and associated interface(s).
    /// @see com.webcodepro.applecommander.storage.FileEntry
    /// @see FileEntry
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
        public ProdosAttributes getProdosAttributes() {
            ProdosAttributes.Builder builder = ProdosAttributes.builder()
                    .name(fileEntry.getFilename())
                    .size(fileEntry.getSize())
                    .locked(fileEntry.isLocked());
            switch (fileEntry) {
                case CpmFileEntry cpm -> {
                    if (CpmFileEntry.TEXT_FILETYPES.contains(cpm.getFiletype())) {
                        builder.TXT();
                    }
                    else {
                        builder.BIN(cpm.getAddress());
                    }
                }
                case DosFileEntry dos -> {
                    switch (dos.getFiletype()) {
                        case "B" -> builder.BIN(dos.getAddress());
                        case "A" -> builder.BAS().auxType(0x0801);
                        case "I" -> builder.INT().auxType(0x0c00);
                        case "T" -> builder.TXT();
                        case "R" -> builder.REL();
                        case "S","a","b" -> builder.BIN(0x0000);
                    }
                }
                case GutenbergFileEntry _ -> builder.TXT();
                case NakedosFileEntry nakedos -> builder.BIN(nakedos.getAddress());
                case PascalFileEntry pascal -> {
                    if (pascal.getFiletype().equals("text")) {
                        builder.TXT();
                    }
                    else {
                        builder.BIN(pascal.getAddress());
                    }
                }
                case ProdosFileEntry prodos -> {
                    builder.fileType(prodos.getFiletypeByte())
                           .auxType(prodos.getAuxiliaryType())
                           .access(prodos.getAccessByte())
                           .creation(prodos.getCreationDate())
                           .modification(prodos.getLastModificationDate());
                }
                case RdosFileEntry rdos -> {
                    switch (rdos.getFiletype()) {
                        case "S" -> builder.BIN(0x0000);
                        case "A" -> builder.BAS();
                        case "B" -> builder.BIN(rdos.getAddress());
                        case "T" -> builder.TXT();
                    }
                }
                default -> throw new IllegalStateException("Unexpected file entry: " + fileEntry.getClass().getName());
            }
            return builder.get();
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
        public ContentType getContentType() {
            if (fileEntry instanceof ProdosFileEntry prodosFileEntry) {
                return FileMagic.getProdosContentType(prodosFileEntry.getFiletypeByte(),
                                                      prodosFileEntry.getAuxiliaryType());
            }
            return ContentType.UNKNOWN;
        }
    }
    /// The `DiskDirectoryEntryAdapter` is a shim that allows a `FormattedDisk` or `DirectoryEntry` to be mapped into the
    /// new/evolving `Directory` / `WritableDirectory` and associated interface(s).
    /// @see DirectoryEntry
    /// @see Directory
    public static class DiskDirectoryAdapter implements WritableDirectory {
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
        public boolean can(Capability capability) {
            return switch (capability) {
                case CREATE_DIRECTORIES -> directoryEntry.canCreateDirectories();
                case CREATE_FILES -> directoryEntry.canCreateFile();
                default -> false;
            };
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
        @Override
        public WritableDirectory createDirectory(String directoryName) {
            try {
                com.webcodepro.applecommander.storage.DirectoryEntry newDirectory = directoryEntry.createDirectory(directoryName);
                return new DiskDirectoryAdapter(adapter, this, newDirectory);
            } catch (DiskException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public WritableFileEntry createFile(String fileName) {
            try {
                com.webcodepro.applecommander.storage.FileEntry fileEntry = directoryEntry.createFile();
                fileEntry.setFilename(fileName);
                return new DiskFileEntryAdapter(adapter, this, fileEntry);
            } catch (DiskException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public WritableFileEntry createFrom(ProdosAttributes prodosAttributes) {
            if (directoryEntry.canCreateFile()) {
                try {
                    FormattedDisk disk = directoryEntry.getFormattedDisk();
                    com.webcodepro.applecommander.storage.FileEntry fileEntry = directoryEntry.createFile();
                    fileEntry.setFilename(disk.getSuggestedFilename(prodosAttributes.name()));
                    fileEntry.setFiletype(disk.toNativeFiletype(prodosAttributes.fileTypeText()));
                    fileEntry.setAddress(prodosAttributes.auxType());
                    fileEntry.setLocked(prodosAttributes.access() != 0xe3);
                    if (prodosAttributes.dataFork().isPresent()) {
                        fileEntry.setFileData(prodosAttributes.dataFork().get().asBytes());
                    }
                    return new DiskFileEntryAdapter(adapter, this, fileEntry);
                } catch (DiskException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                // Generic error
                return WritableDirectory.super.createFrom(prodosAttributes);
            }
        }

        @Override
        public void deleteFile(FileEntry fileEntry) {
            if (fileEntry instanceof DiskFileEntryAdapter fileAdapter) {
                fileAdapter.fileEntry.delete();
            } else {
                throw new RuntimeException("unexpected file entry type: " + fileEntry);
            }
        }
    }
}

