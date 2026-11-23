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
package org.applecommander.os.gutenberg;

import org.applecommander.capability.Capability;
import org.applecommander.change.ChangeRegistry;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileStore;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.usage.ComputedSectorUsage;
import org.applecommander.usage.UsageType;
import org.applecommander.util.Container;
import org.applecommander.util.InformationGroup;

import java.util.List;
import java.util.Optional;

public class GutenbergFileStore extends ChangeRegistry<FileStore.FileStoreChangeEvent> implements FileStore, GutenbergConstants {
    private final TrackSectorDevice device;
    private final GutenbergDirectory rootDirectory;
    private final GutenbergFileHelper helper;
    private final ComputedSectorUsage usage;

    GutenbergFileStore(TrackSectorDevice device) {
        this.device = device;
        this.helper = new GutenbergFileHelper(device);
        this.rootDirectory = new GutenbergDirectory(this);
        this.usage = new ComputedSectorUsage(device.getGeometry(), this::usage);
    }

    public void usage(ComputedSectorUsage.UsagePopulator populator) {
        for (int t=0; t<3; t++) {
            for (int s=0; s<device.getGeometry().sectorsPerTrack(); s++) {
                populator.setUsage(t, s, UsageType.SYSTEM);
            }
        }
        populator.setUsage(0, 0, UsageType.BOOT);
        helper.follow(FIRST_CATALOG_TRACK, FIRST_CATALOG_SECTOR, (t,s,_) -> {
            populator.setUsage(t, s, UsageType.DIRECTORY);
        });
        rootDirectory.getFiles().forEach(fileEntry -> {
            GutenbergFileEntry file = (GutenbergFileEntry) fileEntry;
            if (file.isDeleted()) {
                return;
            }
            helper.follow(file.getFirstTrack(), file.getFirstSector(), (t, s, _) -> {
                populator.setUsage(t, s, UsageType.FILE);
            });
        });
    }

    /// Get the type of file store. This is a generic name, such as "RDOS 2.1" or "ShrinkIt".
    @Override
    public String getType() {
        return "Gutenberg";
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
    public Directory getRootDirectory() {
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
        return DisplayColumn.builder(GutenbergFileEntry.class)
            .left("Name").string(GutenbergFileEntry::getName).done()
            .right("Track").integer(GutenbergFileEntry::getFirstTrack, "%d").done()
            .right("Sector").integer(GutenbergFileEntry::getFirstSector, "%d").done()
            .center("Type").string(GutenbergFileEntry::getType).done()
            .right("Size").integer(GutenbergFileEntry::getSize, "%d").done()
            .right("Sectors").integer(GutenbergFileEntry::getSectors, "%d").mode(DisplayMode.DETAIL).done()
            .center("Deleted?").yesNo(GutenbergFileEntry::isDeleted).done()
            .right("Dir Trk").integer(GutenbergFileEntry::getDirectoryTrack, "%d").mode(DisplayMode.DETAIL).done()
            .right("Dir Sct").integer(GutenbergFileEntry::getDirectorySector, "%d").mode(DisplayMode.DETAIL).done()
            .right("Dir Off").integer(GutenbergFileEntry::getDirectoryOffset, "%d").mode(DisplayMode.DETAIL).done()
            .toList();
    }

    /// Helps identify capabilities of the existing component.
    @Override
    public boolean can(Capability capability) {
        return false;
    }

    /// Retrieve non-standard components.
    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device, usage, helper);
    }

    /// Get information about this component.
    @Override
    public List<InformationGroup> information() {
        return InformationGroup.builder("Gutenberg")
                .item("Label").value(getLabel())
                .item("Files").value(rootDirectory.getFiles().size())
                .get(device);
    }
}
