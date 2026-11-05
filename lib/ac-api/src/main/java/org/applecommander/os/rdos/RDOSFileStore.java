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
package org.applecommander.os.rdos;

import org.applecommander.capability.Capability;
import org.applecommander.change.ChangeRegistry;
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

public class RDOSFileStore extends ChangeRegistry<FileStore.FileStoreChangeEvent> implements FileStore, RDOSConstants {
    private final BlockDevice device;
    private final RDOSVersionAdapter adapter;
    private final RDOSDirectory rootDirectory;
    private final BlockUsage usage;

    RDOSFileStore(BlockDevice device, RDOSVersionAdapter adapter) {
        Objects.requireNonNull(device);
        this.device = device;
        this.adapter = adapter;
        this.rootDirectory = new RDOSDirectory(this);
        this.usage = new BlockUsage(device.getGeometry(), this::usage);
        addChangeListener(this.usage);
    }

    public void usage(BlockUsage.UsagePopulator populator) {
        populator.setUsage(0, UsageType.BOOT);
        for (int b=1; b<adapter.lastSystemBlock; b++) {
            populator.setUsage(b, UsageType.SYSTEM);
        }
        for (int b=adapter.firstDirectoryBlock; b<= adapter.lastDirectoryBlock; b++) {
            populator.setUsage(b, UsageType.DIRECTORY);
        }
        for (FileEntry fileEntry : getRootDirectory().getFiles()) {
            RDOSFileEntry entry = (RDOSFileEntry) fileEntry;
            for (int b=entry.getFirstBlock(); b<=entry.getFirstBlock() + entry.getBlocks() - 1; b++) {
                if (b >= device.getGeometry().blocksOnDevice()) {
                    // Bad data when the blocks get crazy?
                    break;
                }
                else if (b <= adapter.lastSystemBlock) {
                    // Don't overwrite those system areas (since RDOS is also expressed as a file)
                    continue;
                }
                populator.setUsage(b, UsageType.FILE);
            }
        }
    }

    void notifyChangeListeners(ChangeType changeType) {
        notifyChangeListeners(new FileStoreChangeEvent(this, changeType));
    }

    public RDOSVersionAdapter getAdapter() {
        return adapter;
    }

    /// Get the type of file store. This is a generic name, such as "RDOS 2.1" or "ShrinkIt".
    @Override
    public String getType() {
        return adapter.label;
    }

    /// The label is a mechanism to distinguish multiple file stores in a single file.
    /// For instance, UniDOS has two file stores in the file, so it would have two
    /// different labels such as "Disk 1" and "Disk 2".
    @Override
    public String getLabel() {
        return adapter.label;
    }

    /// All FileStores support a "root" directory that references all files.
    @Override
    public RDOSDirectory getRootDirectory() {
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
        return DisplayColumn.builder(RDOSFileEntry.class)
            .center("Type").string(RDOSFileEntry::getType, RDOSFileEntry::setType)
                .option("A", "Applesoft BASIC")
                .option("B", "Binary")
                .option("T", "Text")
                .done()
            .right("Blocks").integer(RDOSFileEntry::getBlocks, "%03d").done()
            .left("Name").string(RDOSFileEntry::getName, RDOSFileEntry::setName).maxLength(NAME_LENGTH)
                .characterRestriction(FILE_NAME_MAPPER).done()
            .right("Length").integer(RDOSFileEntry::getSize, "%,d").done()
            .right("First Block").integer(RDOSFileEntry::getFirstBlock, "%03d").done()
            .right("Address").integer(RDOSFileEntry::getAddress, "$%04X", RDOSFileEntry::setAddress)
                .mode(DisplayMode.DETAIL).restrictToHexValues().done()
            .center("Deleted?").yesNo(RDOSFileEntry::isDeleted).mode(DisplayMode.DETAIL).done()
            .right("Dir. Blk.").integer(RDOSFileEntry::getDirectoryBlock, "%03d").mode(DisplayMode.DETAIL).done()
            .right("Dir. Off.").integer(RDOSFileEntry::getDirectoryOffset, "$%02X").mode(DisplayMode.DETAIL).done()
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
        return InformationGroup.builder("RDOS")
            .item("Version").value(adapter.label)
            .item("Files").value(rootDirectory.getFiles().size())
            .merge(usage)
            .get(device);
    }
}
