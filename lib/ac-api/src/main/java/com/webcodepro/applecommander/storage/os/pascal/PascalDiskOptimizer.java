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
package com.webcodepro.applecommander.storage.os.pascal;

import com.webcodepro.applecommander.storage.FileEntry;
import org.applecommander.os.DiskOptimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A simple Pascal disk optimizer. This should emulate a "krunch" to the front of the disk.
 */
public class PascalDiskOptimizer implements DiskOptimizer {
    private PascalFormatDisk disk;

    public PascalDiskOptimizer(PascalFormatDisk disk) {
        this.disk = disk;
    }

    @Override
    public boolean canOptimizeDisk() {
        int nextBlock = 6;
        for (PascalFileEntry fileEntry : getSortedFiles()) {
            if (fileEntry.getFirstBlock() != nextBlock) {
                return true;
            }
            nextBlock = fileEntry.getLastBlock();
        }
        return false;
    }

    @Override
    public void optimizeDisk() {
        int nextBlock = 6;
        for (PascalFileEntry fileEntry : getSortedFiles()) {
            if (fileEntry.getFirstBlock() != nextBlock) {
                int offset = fileEntry.getFirstBlock() - nextBlock;
                // We move both pointers where we think they should be. And then save the file to move the data.
                byte[] fileData = fileEntry.getFileData();
                fileEntry.setFirstBlock(fileEntry.getFirstBlock() - offset);
                fileEntry.setLastBlock(fileEntry.getLastBlock() - offset);
                try {
                    fileEntry.setFileData(fileData);
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected error", e);
                }
            }
            nextBlock = fileEntry.getLastBlock();
        }
    }

    public List<PascalFileEntry> getSortedFiles() {
        List<PascalFileEntry> sortedFiles = new ArrayList<>();
        for (FileEntry fileEntry : disk.getFiles()) {
            sortedFiles.add((PascalFileEntry) fileEntry);
        }
        sortedFiles.sort(Comparator.comparingInt(PascalFileEntry::getFirstBlock));
        return sortedFiles;
    }
}
