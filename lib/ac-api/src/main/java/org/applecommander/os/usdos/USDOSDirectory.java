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
package org.applecommander.os.usdos;

import org.applecommander.capability.Capability;
import org.applecommander.exception.DirectoryFullException;
import org.applecommander.filestore.*;
import org.applecommander.filestore.ConfigurationOption.StringConfigurationOption;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.usage.SectorAllocator;
import org.applecommander.util.DataBuffer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class USDOSDirectory implements WritableDirectory, USDOSConstants {
    private final USDOSDirectory parent;
    private final USDOSFileStore fileStore;
    private final int directoryNumber;
    private final String directoryName;

    USDOSDirectory(USDOSFileStore fileStore, int directoryNumber, String directoryName) {
        this(fileStore, directoryNumber, directoryName, null);
    }
    USDOSDirectory(USDOSFileStore fileStore, int directoryNumber, String directoryName, USDOSDirectory parent) {
        this.fileStore = fileStore;
        this.directoryNumber = directoryNumber;
        this.directoryName = directoryName;
        this.parent = parent;
    }

    /// Get the parent directory. If this is the root directory, it returns empty.
    @Override
    public Optional<Directory> getParent() {
        return Optional.ofNullable(parent);
    }

    /// Returns the FileStore that this Directory belongs to.
    @Override
    public USDOSFileStore getFileStore() {
        return fileStore;
    }

    /// Returns the name of this directory.
    @Override
    public String getName() {
        return directoryName;
    }

    /// Return all entries that are stored in this directory.
    @Override
    public List<FileEntry> getFiles() {
        List<FileEntry> files = new ArrayList<>();
        // Locate all files matching our directory number.
        DataBuffer directory = fileStore.readDirectory();
        directory.position(0);
        int count = DIRECTORY_MAX_ENTRIES;
        while (count > 0) {
            int offset = directory.position();
            DataBuffer entryData = directory.readBuffer(ENTRY_LENGTH);
            if (entryData.getUnsignedByte(ENTRY_NAME+1) == 0) {
                break;  // end of directory
            }
            if (entryData.getUnsignedByte(ENTRY_DIR_NUM) == directoryNumber) {
                files.add(new USDOSFileEntry(this, offset));
            }
            count--;
        }
        return files;
    }

    /// Perform a quick count of the number of files (including directories) that are on this disk.
    /// Deleted files are not part of the count.
    public int getTotalFileCount() {
        int fileCount = 0;
        DataBuffer directory = fileStore.readDirectory();
        directory.position(0);
        while (directory.hasRemaining()) {
            DataBuffer entryData = directory.readBuffer(ENTRY_LENGTH);
            if (entryData.getUnsignedByte(ENTRY_NAME+1) == 0) {
                break;  // end of directory
            }
            if (entryData.getUnsignedByte(ENTRY_NAME) != 0) {
                fileCount++;
            }
        }
        return fileCount;
    }

    /// Create a file. By definition, any file created is a `WritableFileEntry`.
    /// @see Capability#CREATE_FILES
    @Override
    public USDOSFileEntry createFile(String fileName) {
        for (int i=0; i<DIRECTORY_MAX_ENTRIES; i++) {
            USDOSFileEntry fileEntry = new USDOSFileEntry(this, i*ENTRY_LENGTH);
            if (fileEntry.isDeleted()) {
                int dot = fileName.lastIndexOf('.');
                String extension = "BIN";
                if (dot > -1) {
                    extension = fileName.substring(dot+1);
                    fileName = fileName.substring(0, dot);
                }
                fileEntry.setName(fileName);
                fileEntry.setExtension(extension);
                fileEntry.setDirectoryNumber(this.directoryNumber);
                fileEntry.setDate(LocalDate.now());
                fileStore.notifyChangeListeners(FileStore.ChangeType.FILE_CREATED);
                return fileEntry;
            }
        }
        throw new DirectoryFullException("there are no more directory entries available on this disk");
    }

    /// Create a file given the supplied ProDOS attributes. This is expected to handle
    /// setting all appropriate `FileEntry` attributes that align. File type should be
    /// converted, dates applied, etc.
    /// @see Capability#CREATE_FILES
    @Override
    public WritableFileEntry createFrom(ProdosAttributes prodosAttributes) {
        USDOSFileEntry fileEntry = createFile(prodosAttributes.name());
        fileEntry.setAddress(prodosAttributes.auxType());
        fileEntry.setDate(prodosAttributes.creation().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        // This is a second attempt at setting the extension. BIN is the default.
        String currentExtension = fileEntry.getExtension();
        if ("BIN".equals(currentExtension) || currentExtension.isBlank()) {
            String extension = switch (prodosAttributes.fileType()) {
                case ProdosAttributes.BAS -> "BAS";
                case ProdosAttributes.TXT -> "TXT";
                default -> currentExtension;
            };
            fileEntry.setExtension(extension);
        }
        prodosAttributes.dataFork().ifPresent(fileEntry::setDataFork);
        prodosAttributes.resourceFork().ifPresent(fileEntry::setResourceFork);
        return fileEntry;
    }

    /// Get the options that are applied to directory names.
    /// @see Capability#CREATE_DIRECTORIES
    @Override
    public StringConfigurationOption getDirectoryConfigurationOption() {
        return ConfigurationOption.builder().stringValue("directory_name")
                .enforceLength(1, ENTRY_NAME_LENGTH)
                .restrictions(DIRECTORY_NAME_MAPPER)
                .single();
    }

    /// Create a directory. By definition, any directory create is a `WritableDirectory`.
    /// @see Capability#CREATE_DIRECTORIES
    @Override
    public USDOSDirectory createDirectory(String directoryName) {
        // Identify all existing directory numbers.
        Set<Integer> currentDirectoryNumbers = new HashSet<>();
        currentDirectoryNumbers.add(0);     // Need to be certain the ROOT directory is represented!
        for (int i=0; i<DIRECTORY_MAX_ENTRIES; i++) {
            USDOSFileEntry fileEntry = new USDOSFileEntry(this, i*ENTRY_LENGTH);
            DataBuffer data = fileEntry.readEntry();
            if (data.getUnsignedByte(ENTRY_EXT) == ENTRY_DIR_VALUE) {
                currentDirectoryNumbers.add(data.getUnsignedByte(ENTRY_EXT+1));
            }
        }
        // Now we need to create the directory entry and set it up.
        int nextDirectoryNumber = 0;
        while (currentDirectoryNumbers.contains(nextDirectoryNumber)) {
            nextDirectoryNumber++;
        }
        USDOSFileEntry subdirectory = createFile(directoryName);
        subdirectory.setExtensionAsDirectory(nextDirectoryNumber);
        return new USDOSDirectory(fileStore, nextDirectoryNumber, directoryName, this);
    }

    /// Delete a file.
    /// @see Capability#DELETE_FILES
    @Override
    public void deleteFile(FileEntry fileEntry) {
        USDOSFileEntry file = (USDOSFileEntry) fileEntry;
        file.modifyEntry(entry -> entry.putByte(ENTRY_NAME, 0));
        // Free both the file sectors and the sector index sector
        SectorAllocator allocator = fileStore.get(SectorAllocator.class).orElseThrow();
        file.readSectorIndex().forEach(allocator::markFree);
        allocator.markFree(file.getTrackSectorIndex());
        fileStore.writeBitmap();
        // Tell everyone about it.
        fileStore.notifyChangeListeners(FileStore.ChangeType.FILE_DELETED);
    }
}
