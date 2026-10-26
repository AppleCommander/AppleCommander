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
import org.applecommander.device.BlockDevice;
import org.applecommander.filestore.FileStore;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.util.Container;
import org.applecommander.util.InformationGroup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RDOSFileStore implements FileStore, RDOSConstants {
    private final BlockDevice device;
    private final RDOSVersionAdapter adapter;
    private final RDOSDirectory rootDirectory;

    public RDOSFileStore(BlockDevice device, RDOSVersionAdapter adapter) {
        Objects.requireNonNull(device);
        this.device = device;
        this.adapter = adapter;
        this.rootDirectory = new RDOSDirectory(this);
    }

    public RDOSVersionAdapter getAdapter() {
        return adapter;
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
            .left("Name").string(RDOSFileEntry::getName, RDOSFileEntry::setName).maxLength(NAME_LENGTH).done()
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
        return Container.get(iface, device);
    }

    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("RDOS")
            .item("Version").value(adapter.label)
            .item("Files").value(rootDirectory.getFiles().size())
            .get(device);
    }
}
