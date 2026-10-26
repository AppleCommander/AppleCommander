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

import org.applecommander.device.BlockDevice;
import org.applecommander.device.TrackSectorDevice;
import org.applecommander.exception.DiskFullException;
import org.applecommander.filestore.WritableFileEntry;
import org.applecommander.os.SequentialAllocator;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.util.DataBuffer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class RDOSFileEntry implements WritableFileEntry, RDOSConstants {
    private final BlockDevice device;
    private final RDOSDirectory parent;
    private final int block;
    private final int offset;

    RDOSFileEntry(RDOSDirectory parent, int block, int offset) {
        Objects.requireNonNull(parent);
        RDOSVersionAdapter adapter = parent.getFileStore().getAdapter();
        assert block >= adapter.firstDirectoryBlock && block <= adapter.lastDirectoryBlock;
        assert offset >= 0 && offset < TrackSectorDevice.SECTOR_SIZE;
        this.device = parent.getFileStore().get(BlockDevice.class).orElseThrow();
        this.parent = parent;
        this.block = block;
        this.offset = offset;
    }

    public DataBuffer readEntry() {
        return device.readBlock(block).slice(offset, ENTRY_LENGTH);
    }
    public void modifyEntry(Consumer<DataBuffer> consumer) {
        DataBuffer blockData = device.readBlock(block);
        DataBuffer entryData = blockData.slice(offset, ENTRY_LENGTH);
        consumer.accept(entryData);
        device.writeBlock(block, blockData);
    }

    public int getDirectoryBlock() {
        return block;
    }
    public int getDirectoryOffset() {
        return offset;
    }

    /// The parent `Directory`, if applicable. Can return `null` if this is the root directory.
    @Override
    public RDOSDirectory getParent() {
        return parent;
    }

    /// Indicates if this `FileEntry` is deleted.
    @Override
    public boolean isDeleted() {
        DataBuffer data = readEntry();
        return data.getUnsignedByte(NAME_OFFSET) == 0x80;
    }

    /// Returns the `FileStore` that this `Entry` originates from.
    @Override
    public RDOSFileStore getFileStore() {
        return parent.getFileStore();
    }

    /// Returns the name of this entry: either a directory name or a file name.
    /// This does not contain any directory components -- strictly the name of this entry.
    /// Note that depending on context, this may be a computed field.
    @Override
    public String getName() {
        DataBuffer data = readEntry();
        if (data.getUnsignedByte(NAME_OFFSET) == 0x80) {
            return "<NOT IN USE>";
        }
        return data.getFixedLengthString(NAME_OFFSET, NAME_LENGTH).trim();
    }
    public void setName(String name) {
        modifyEntry(entry -> entry.putFixedLengthString(NAME_OFFSET, NAME_LENGTH, name, true));
    }

    public String getType() {
        DataBuffer data = readEntry();
        return data.getFixedLengthString(TYPE_OFFSET, 1);
    }
    public void setType(String type) {
        modifyEntry(entry -> entry.putFixedLengthString(TYPE_OFFSET, 1, type, true));
    }

    public int getBlocks() {
        DataBuffer data = readEntry();
        return data.getUnsignedByte(BLOCKS_OFFSET);
    }
    public void setBlocks(int blocks) {
        modifyEntry(entry -> entry.putUnsignedShort(BLOCKS_OFFSET, blocks));
    }

    public int getAddress() {
        DataBuffer data = readEntry();
        return data.getUnsignedShort(ADDRESS_OFFSET);
    }
    public void setAddress(int addr) {
        modifyEntry(entry -> entry.putUnsignedShort(ADDRESS_OFFSET, addr));
    }

    public int getFirstBlock() {
        DataBuffer data = readEntry();
        return data.getUnsignedShort(FIRST_BLOCK_OFFSET);
    }
    public void setFirstBlock(int firstBlock) {
        modifyEntry(entry -> entry.putUnsignedShort(FIRST_BLOCK_OFFSET, firstBlock));
    }

    /// Returns the size, in bytes, of this item.
    /// It may be approximate (based off a sector count, for instance).
    @Override
    public long getSize() {
        DataBuffer data = readEntry();
        return data.getUnsignedShort(LENGTH_OFFSET);
    }
    public void setSize(int size) {
        modifyEntry(entry -> entry.putUnsignedShort(LENGTH_OFFSET, size));
    }

    /// Return the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public DataBuffer getDataFork() {
        return device.readRange(getFirstBlock(), getBlocks()).slice(0, (int)getSize());
    }

    /// Interpret the `FileEntry` as if it were on a ProDOS volume.
    /// The intention is that ProDOS ended up being the common denominator in many tools and formats,
    /// so this allows each file store to manage the interpretation itself.
    @Override
    public ProdosAttributes getProdosAttributes() {
        return null;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Optional.empty();
    }

    /// Store the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public void setDataFork(DataBuffer data) {
        final int requiredBlocks = device.calculateRequiredBlocks(data.limit());
        BlockDevice.Geometry geometry = device.getGeometry();
        SequentialAllocator allocator = new SequentialAllocator(geometry.blocksOnDevice());
        getParent().getFiles().forEach(file -> {
            RDOSFileEntry entry = (RDOSFileEntry) file;
            if (entry.getDirectoryBlock() != this.getDirectoryBlock() || entry.getDirectoryOffset() != this.getDirectoryOffset()) {
                allocator.addUsedByLength(entry.getFirstBlock(), entry.getBlocks());
            }
        });
        int firstBlock = allocator.find(requiredBlocks);
        if (firstBlock != -1) {
            device.writeRange(firstBlock, data);
            setFirstBlock(firstBlock);
            setBlocks(requiredBlocks);
            if ("T".equals(getType())) {
                setSize(requiredBlocks * device.getGeometry().blockSize());
            }
            else {
                setSize(data.limit());
            }
        }
        else {
            throw new DiskFullException("Unable to allocate %d blocks on disk", requiredBlocks);
        }

    }
}
