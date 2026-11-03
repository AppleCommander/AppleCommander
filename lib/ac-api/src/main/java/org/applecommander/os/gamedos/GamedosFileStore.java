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
package org.applecommander.os.gamedos;

import org.applecommander.capability.Capability;
import org.applecommander.change.ChangeRegistry;
import org.applecommander.device.SectorOrder;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.filestore.FileEntry;
import org.applecommander.filestore.FileStore;
import org.applecommander.ui.DisplayColumn;
import org.applecommander.ui.DisplayMode;
import org.applecommander.usage.DiskUsage.UsageType;
import org.applecommander.usage.SectorUsage;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.InformationGroup;

import java.util.List;
import java.util.Optional;

public class GamedosFileStore extends ChangeRegistry<FileStore.FileStoreChangeEvent> implements FileStore, GamedosConstants {
    private final TrackSectorDevice device;
    private final GamedosDirectory rootDirectory;
    private final SectorUsage usage;

    GamedosFileStore(TrackSectorDevice device) {
        this.device = device.request(SectorOrder.DOS);
        this.rootDirectory = new GamedosDirectory(this);
        this.usage = new SectorUsage(device.getGeometry(), this::usage);
        addChangeListener(this.usage);
    }

    public void usage(SectorUsage.UsagePopulator populator) {
        for (int s=0; s<device.getGeometry().sectorsPerTrack(); s++) {
            if (s == 0) {
                populator.setUsage(0, s, UsageType.BOOT);
            }
            else if (s < DIRECTORY_SECTOR) {
                populator.setUsage(0, s, UsageType.SYSTEM);
            }
            else {
                populator.setUsage(0, s, UsageType.DIRECTORY);
            }
        }
        for (FileEntry file : getRootDirectory().getFiles()) {
            GamedosFileEntry entry = (GamedosFileEntry) file;
            device.performRange(entry.getFirstTrack(), entry.getFirstSector(), entry.getSectorCount(),
                    (t,s,_) -> populator.setUsage(t, s, UsageType.FILE));
        }
    }

    void notifyChangeListeners(ChangeType changeType) {
        notifyChangeListeners(new FileStoreChangeEvent(this, changeType));
    }

    /// Get the type of file store. This is a generic name, such as "RDOS 2.1" or "ShrinkIt".
    @Override
    public String getType() {
        return "GameDOS";
    }

    @Override
    public String getLabel() {
        DataBuffer track0 = device.readRange(0, 0, 14);
        Optional<Integer> opt = track0.scan(MARKER,0);
        if (opt.isPresent()) {
            int pos = opt.get() + MARKER.length;
            int ch;
            // Search for the "V9.9"
            do {
                ch = track0.getUnsignedByte(pos);
                pos++;
            } while (ch == 'V' || (ch >= '0' && ch <= '9') || ch == '.');
            return track0.getFixedLengthString(opt.get(), pos-opt.get()-1);
        }
        return "GameDOS";
    }

    @Override
    public GamedosDirectory getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public String getPathSeparator() {
        return "";
    }

    @Override
    public List<DisplayColumn> getDisplayColumns() {
        return DisplayColumn.builder(GamedosFileEntry.class)
            .left("Type").string(GamedosFileEntry::getFiletype, GamedosFileEntry::setFiletype)
                .option("A", "Applesoft BASIC")
                .option("B", "Binary")
                .option("I", "Integer BASIC")
                .option("P", "Picture")
                .option("S", "System")
                .option("T", "Text")
                .done()
            .left("Name").string(GamedosFileEntry::getName, GamedosFileEntry::setName)
                .editInLine().maxLength(26).done()
            .right("Meta").integer(GamedosFileEntry::getMeta, "$%04X", GamedosFileEntry::setMeta)
                .restrictToHexValues().done()
            .right("First Track").integer(GamedosFileEntry::getFirstTrack, "%d").mode(DisplayMode.DETAIL).done()
            .right("First Sector").integer(GamedosFileEntry::getFirstSector, "%d").mode(DisplayMode.DETAIL).done()
            .right("Sector Count").integer(GamedosFileEntry::getSectorCount, "%d").mode(DisplayMode.DETAIL).done()
            .right("Size").integer(GamedosFileEntry::getSize, "%,d").done()
            .toList();
    }

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
        return InformationGroup.builder("GameDOS")
            .item("Label").value(getLabel())
            .item("Files").value(rootDirectory.getFiles().size())
            .merge(usage)
            .get(device);
    }
}
