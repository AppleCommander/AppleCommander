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
package org.applecommander.os.pascal;

import org.applecommander.capability.Capability;
import org.applecommander.device.BlockDevice;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.usage.BlockUsage;
import org.applecommander.usage.DiskUsage.UsageType;
import org.applecommander.util.Container;
import org.applecommander.util.InformationGroup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PascalFileStore implements FileStore, PascalConstants {
    private final BlockDevice device;
    private final PascalDirectory rootDirectory;
    private final BlockUsage usage;

    PascalFileStore(BlockDevice device) {
        Objects.requireNonNull(device);
        this.device = device;
        this.rootDirectory = new PascalDirectory(this);
        this.usage = new BlockUsage(device.getGeometry(), this::usage);
    }

    public UsageType usage(int blockNumber) {
        if (blockNumber < 2) {
            return UsageType.BOOT;
        }
        else if (blockNumber <= DIRECTORY_LAST_BLOCK) {
            return UsageType.DIRECTORY;
        }
        else {
            for (FileEntry entry : rootDirectory.getFiles()) {
                PascalFileEntry file = (PascalFileEntry) entry;
                if (blockNumber >= file.getFirstBlock() && blockNumber < file.getLastBlock()) {
                    return UsageType.FILE;
                }
            }
        }
        return UsageType.FREE;
    }

    /// The label is a mechanism to distinguish multiple file stores in a single file.
    /// For instance, UniDOS has two file stores in the file, so it would have two
    /// different labels such as "Disk 1" and "Disk 2".
    @Override
    public String getLabel() {
        return rootDirectory.getName();
    }

    /// All FileStores support a "root" directory that references all files.
    @Override
    public PascalDirectory getRootDirectory() {
        return rootDirectory;
    }

    /// This is the path separator character.
    @Override
    public String getPathSeparator() {
        return "";
    }

    /// Yields a list of columns to be used in this display mode.
    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(PascalFileEntry.class)
            .left("Name").string(PascalFileEntry::getName, PascalFileEntry::setName)
                .editInLine()
                .maxLength(FILE_NAME_LENGTH)
                .characterRestriction(PascalFileEntry.NAME_MAPPER)
                .done()
            .right("Offset").integer(PascalFileEntry::getOffset, "%d").mode(DisplayMode.DETAIL).done()
            .left("Type").string(PascalFileEntry::getType, PascalFileEntry::setType).mode(DisplayMode.DETAIL)
                .option("CODE", "p-code")
                .option("TEXT", "Text")
                .option("DATA", "Data")
                .option("INFO", "Info")
                .option("GRAF", "Graphics")
                .option("FOTO", "Photo")
                .done()
            .right("First Block").integer(PascalFileEntry::getFirstBlock, "%d").mode(DisplayMode.DETAIL).done()
            .right("Last Block").integer(PascalFileEntry::getLastBlock, "%d").mode(DisplayMode.DETAIL).done()
            .right("Last Bytes").integer(PascalFileEntry::getBytesUsedInLastBlock, "%d").mode(DisplayMode.DETAIL).done()
            .right("Blocks").integer(PascalFileEntry::getBlocksUsed, "%d").done()
            .right("Size").integer(PascalFileEntry::getSize, "%,d").mode(DisplayMode.DETAIL).done()
            .center("Modified").dateDDMONYY(PascalFileEntry::getLastModified).done()
            .toList();
    }

    /// Helps identify capabilities of the existing component.
    @Override
    public boolean can(Capability capability) {
        return CAPABILITIES.contains(capability);
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device, usage);
    }

    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("Pascal")
            .item("Label").value(getLabel())
            .item("Blocks on Disk").value(rootDirectory.getBlocksOnDisk())
            .item("Files").value(rootDirectory.getFilesOnDisk())
            .item("Most Recent Date").value(rootDirectory.getMostRecentDate())
            .merge(usage)
            .get(device);
    }
}
