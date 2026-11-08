package org.applecommander.os.usdos;

import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileEntry;
import org.applecommander.util.DataBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class USDOSDirectory implements Directory, USDOSConstants {
    private final USDOSFileStore fileStore;
    private final TrackSectorDevice device;
    private final int directoryNumber;
    private final String directoryName;

    USDOSDirectory(USDOSFileStore fileStore, int directoryNumber, String directoryName) {
        this.fileStore = fileStore;
        this.device = fileStore.get(TrackSectorDevice.class).orElseThrow();
        this.directoryNumber = directoryNumber;
        this.directoryName = directoryName;
    }

    /// Get the parent directory. If this is the root directory, it returns empty.
    @Override
    public Optional<Directory> getParent() {
        return Directory.super.getParent();
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
        while (directory.hasRemaining()) {
            int offset = directory.position();
            DataBuffer entryData = directory.readBuffer(ENTRY_LENGTH);
            if (entryData.getUnsignedByte(ENTRY_NAME+1) == 0) {
                break;  // end of directory
            }
            if (entryData.getUnsignedByte(ENTRY_DIR_NUM) == directoryNumber) {
                files.add(new USDOSFileEntry(this, offset));
            }
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
}
