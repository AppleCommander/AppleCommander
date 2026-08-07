package com.webcodepro.applecommander.storage.os.prodos;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.DiskException;
import com.webcodepro.applecommander.storage.FileEntry;
import org.applecommander.device.BlockDevice;
import org.applecommander.os.DiskCheck;
import org.applecommander.util.DataBuffer;

import java.util.*;
import java.util.logging.Logger;

public class ProdosDiskCheck implements DiskCheck {
    private static final Logger LOG = Logger.getLogger(ProdosDiskCheck.class.getName());

    // Classifications - maybe an enum?
    public static final String DIRECTORY = "directory";
    public static final String ALLOCATION = "allocation";

    private final ProdosFormatDisk disk;
    private final BlockDevice device;

    private final List<Finding> findings = new ArrayList<>();
    private final BitSet usedBlocks = new BitSet();
    private boolean applePascalAreaFound = false;
    private boolean gsosExtendedFileFound = false;

    public ProdosDiskCheck(ProdosFormatDisk disk, BlockDevice device) {
        this.disk = disk;
        this.device = device;
    }

    @Override
    public List<Finding> scan() throws Exception {
        initVolumeBitmap();
        handleSubdirectory(disk, 2);
        checkVolumeBitmap();
        return findings;
    }

    private void handleSubdirectory(DirectoryEntry parentDirectory, int parentKeyPointer) throws DiskException {
        for (var entry : parentDirectory.getFiles()) {
            switch (entry) {
                case ProdosDirectoryEntry subdirectory -> {
                    markDirectoryBlocks(usedBlocks, subdirectory.getKeyPointer());
                    checkSubdirectoryHeaderPointer(subdirectory, parentKeyPointer, parentDirectory.getDirname());
                    handleSubdirectory(subdirectory, subdirectory.getKeyPointer());
                }
                case ProdosFileEntry file -> {
                    switch (file.getStorageType()) {
                        case 0x00 -> { /* Deleted file - ignore */ }
                        case 0x01 -> usedBlocks.set(file.getKeyPointer());
                        case 0x02 -> markIndexBlocks(usedBlocks, file.getKeyPointer(), false);
                        case 0x03 -> markIndexBlocks(usedBlocks, file.getKeyPointer(), true);
                        case 0x04 -> applePascalAreaFound = true;
                        case 0x05 -> gsosExtendedFileFound = true;
                        case 0x0d -> { /* Subdirectory file, should never get here? */ }
                        case 0x0e -> { /* Subdirectory Header - ignore */ }
                        case 0x0f -> { /* Volume Directory Header - ignore */ }
                        default -> throw new RuntimeException("Unexpected storage_type: " + file.getStorageType());
                    }
                }
                default -> throw new RuntimeException("Unexpected file entry type: " + entry.getClass().getName());
            }
        }
    }

    private void checkSubdirectoryHeaderPointer(ProdosDirectoryEntry subdirectory, int parentHeaderPointer, String parentName) {
        final String pointingAtVolumeHeader = "Subdirectory %s is not pointing to the key block of disk %s.";
        final String pointingAtSubdirectoryHeader = "Subdirectory %s is not pointing to the key block of directory %s.";
        if (subdirectory.getHeaderPointer() != parentHeaderPointer) {
            String description = String.format(parentHeaderPointer == 2 ? pointingAtVolumeHeader : pointingAtSubdirectoryHeader,
                    subdirectory.getDirname(), parentName);
            Finding finding = new Finding(description,
                    Optional.of(() -> subdirectory.setHeaderPointer(parentHeaderPointer)),
                    DIRECTORY, new Coordinate(subdirectory.getFileEntryBlock()));
            findings.add(finding);
        }
    }

    private void initVolumeBitmap() {
        // Boot block(s)
        usedBlocks.set(0, 2);
        // Volume directory
        markDirectoryBlocks(usedBlocks, 2);
        // Volume bitmap
        int block = disk.getVolumeHeader().getBitMapPointer();
        usedBlocks.set(block, block + disk.getVolumeBitmapBlockCount());
    }
    private void checkVolumeBitmap() throws DiskException {
        // ProDOS only allows 65535 blocks
        if (Math.min(device.getGeometry().blocksOnDevice(),65535) != disk.getBitmapLength()) {
            String description = String.format("""
                        Blocks on device and ProDOS total blocks do not match (%d != %d). \
                        This could be due to sparse allocation of the image -or- a genuine mismatch. \
                        Skipping allocation scan.""",
                device.getGeometry().blocksOnDevice(), disk.getBitmapLength());
            Finding finding = new Finding(description,
                    Optional.empty(),
                    ALLOCATION, new Coordinate(2));
            findings.add(finding);
            return;
        }

        // Identify known problem areas
        if (applePascalAreaFound || gsosExtendedFileFound) {
            String description;
            if (applePascalAreaFound && gsosExtendedFileFound) description = "Both an Apple Pascal Area and GS/OS Extended File";
            else if (applePascalAreaFound) description = "An Apple Pascal Area";
            else description = "A GS/OS Extended File";
            Finding finding = new Finding(description + " was found, making block allocation incomplete.",
                    Optional.empty(), ALLOCATION, new Coordinate(2));
            findings.add(finding);
            return;
        }
        // Figure out allocation errors
        List<Integer> allocationErrors = new ArrayList<>();
        byte[] volumeBitMap = disk.readVolumeBitMap();
        for (int block = 0; block < device.getGeometry().blocksOnDevice(); block++) {
            if (disk.isBlockUsed(volumeBitMap, block) != usedBlocks.get(block)) {
                allocationErrors.add(block);
            }
        }
        if (!allocationErrors.isEmpty()) {
            String description = String.format("There are %d block allocation errors.", allocationErrors.size());
            Optional<Runnable> action = Optional.empty();
            if (allocationErrors.size() < 10) {
                description = String.format("There are block allocation errors for: %s.", allocationErrors);
                action = Optional.of(() -> {
                    // Note that we only offer to fix the allocations if the count is rather minimal.
                    for (int b = 0; b < device.getGeometry().blocksOnDevice(); b++) {
                        if (usedBlocks.get(b)) {
                            disk.setBlockUsed(volumeBitMap, b);
                        }
                        else {
                            disk.setBlockFree(volumeBitMap, b);
                        }
                        disk.writeVolumeBitMap(volumeBitMap);
                    }
                });
            }
            Finding finding = new Finding(description,
                    action, ALLOCATION, new Coordinate(disk.getVolumeHeader().getBitMapPointer()));
            findings.add(finding);
        }
    }
    private void markDirectoryBlocks(BitSet usedBlocks, int block) {
        while (block != 0) {
            usedBlocks.set(block);
            DataBuffer data = device.readBlock(block);
            block = data.getUnsignedShort(2);
        }
    }
    private void markIndexBlocks(BitSet usedBlocks, int block, boolean masterIndexBlock) {
        usedBlocks.set(block);
        DataBuffer data = device.readBlock(block);
        for (int i=0; i<256; i++) {
            block = data.getUnsignedByte(i) + (data.getUnsignedByte(0x100+i)<<8);
            usedBlocks.set(block);
            if (block > 0 && masterIndexBlock) {
                markIndexBlocks(usedBlocks, block, false);
            }
        }
    }
}
