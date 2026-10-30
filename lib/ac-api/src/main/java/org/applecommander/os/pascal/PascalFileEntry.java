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

import org.applecommander.device.BlockDevice;
import org.applecommander.exception.DiskFullException;
import org.applecommander.filestore.Directory;
import org.applecommander.filestore.FileStore;
import org.applecommander.filestore.WritableFileEntry;
import org.applecommander.os.SequentialAllocator;
import org.applecommander.transfer.ProdosAttributes;
import org.applecommander.ui.CharacterMapper;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class PascalFileEntry implements WritableFileEntry, PascalConstants {
    /// Per the Apple Pascal documentation:
    ///
    /// * Lower-case are translated to upper-case.
    /// * Spaces and non-printing characters are removed from the filename.
    /// * You should not include dollar sign (`$`), left square bracket (`[`),
    ///   equals sign (`=`), or question mark (`?`).
    public static final CharacterMapper NAME_MAPPER = CharacterMapper.builder()
            .mapRange((char)0x21, (char)0x7e, true, '$', '[', '=', '?')
            .build();

    private final BlockDevice device;
    private final PascalDirectory parent;
    private final int offset;

    PascalFileEntry(PascalDirectory parent, int offset) {
        Objects.requireNonNull(parent);
        this.device = parent.getFileStore().get(BlockDevice.class).orElseThrow();
        this.parent = parent;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public DataBuffer readEntry() {
        return device.readRange(DIRECTORY_FIRST_BLOCK, DIRECTORY_BLOCKS).slice(offset, ENTRY_LENGTH);
    }
    public void modifyEntry(Consumer<DataBuffer> consumer) {
        DataBuffer directory = device.readRange(DIRECTORY_FIRST_BLOCK, DIRECTORY_BLOCKS);
        DataBuffer entryData = directory.slice(offset, ENTRY_LENGTH);
        consumer.accept(entryData);
        device.writeRange(DIRECTORY_FIRST_BLOCK, directory);
    }

    /// The parent `Directory`, if applicable. Can return `null` if this is the root directory.
    @Override
    public Directory getParent() {
        return parent;
    }

    /// Indicates if this `FileEntry` is deleted.
    @Override
    public boolean isDeleted() {
        return false;
    }

    /// Returns the `FileStore` that this `Entry` originates from.
    @Override
    public FileStore getFileStore() {
        return parent.getFileStore();
    }

    /// Returns the name of this entry: either a directory name or a file name.
    /// This does not contain any directory components -- strictly the name of this entry.
    /// Note that depending on context, this may be a computed field.
    @Override
    public String getName() {
        return readEntry().getPascalString(ENTRY_NAME);
    }
    /// Sets the name of this file. Uses the `NAME_MAPPER` to fix characters.
    /// This method only errors if, after applying the rules, the string is empty.
    public void setName(String name) {
        String fixedName = NAME_MAPPER.map(name);
        if (fixedName.isEmpty()) {
            throw new IllegalArgumentException("Illegal name '" + name + "'");
        }
        modifyEntry((buffer) -> buffer.putPascalString(ENTRY_NAME, FILE_NAME_LENGTH, fixedName, false));
    }

    /// Store the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public void setDataFork(DataBuffer data) {
        final int requiredBlocks = device.calculateRequiredBlocks(data.limit());
        BlockDevice.Geometry geometry = device.getGeometry();
        SequentialAllocator allocator = new SequentialAllocator(geometry.blocksOnDevice());
        allocator.addUsed(0, DIRECTORY_LAST_BLOCK);
        getParent().getFiles().forEach(file -> {
            PascalFileEntry entry = (PascalFileEntry) file;
            if (entry.getOffset() != this.getOffset()) {
                allocator.addUsedByLength(entry.getFirstBlock(), entry.getBlocksUsed());
            }
        });
        int firstBlock = allocator.find(requiredBlocks);
        if (firstBlock != -1) {
            device.writeRange(firstBlock, data);
            setFirstBlock(firstBlock);
            setLastBlock(firstBlock + requiredBlocks);
            int bytesInLastBlock = data.limit() % geometry.blockSize();
            setBytesUsedInLastBlock(bytesInLastBlock == 0 ? geometry.blockSize() : bytesInLastBlock);
            setLastModified(new Date());
        }
        else {
            throw new DiskFullException("Unable to allocate %d blocks on disk", requiredBlocks);
        }
    }

    /// Get the block number of the files first block.
    public int getFirstBlock() {
        return readEntry().getUnsignedShort(ENTRY_FIRST_BLOCK);
    }
    public void setFirstBlock(int firstBlock) {
        modifyEntry(entry -> entry.putUnsignedShort(ENTRY_FIRST_BLOCK, firstBlock));
    }

    /// Get the block number of the files last block + 1.
    public int getLastBlock() {
        return readEntry().getUnsignedShort(ENTRY_LAST_BLOCK);
    }
    public void setLastBlock(int lastBlock) {
        modifyEntry(entry -> entry.putUnsignedShort(ENTRY_LAST_BLOCK, lastBlock));
    }

    public int getBlocksUsed() {
        return getLastBlock() - getFirstBlock();
    }

    public String getType() {
        int type = readEntry().getUnsignedByte(ENTRY_TYPE) & 0x7f;
        return switch (type) {
            case 0 -> "VOLUME";
            case 1 -> "XDSK";
            case 2 -> "CODE";
            case 3 -> "TEXT";
            case 4 -> "INFO";
            case 5 -> "DATA";
            case 6 -> "GRAF";
            case 7 -> "FOTO";
            case 8 -> "SECURE";
            default -> "UNKNOWN";
        };
    }
    public void setType(String type) {
        int value = switch (type.toUpperCase()) {
            case "VOLUME" -> 0;
            case "XDSK" -> 1;
            case "CODE" -> 2;
            case "TEXT" -> 3;
            case "INFO" -> 4;
            case "DATA" -> 5;
            case "GRAF" -> 6;
            case "FOTO" -> 7;
            case "SECURE" -> 8;
            default -> throw new IllegalArgumentException("Illegal type '" + type + "'");
        };
        modifyEntry(entry -> entry.putUnsignedShort(ENTRY_TYPE, value));
    }

    public int getBytesUsedInLastBlock() {
        return readEntry().getUnsignedShort(FILE_BYTES_USED_IN_LAST_BLOCK);
    }
    public void setBytesUsedInLastBlock(int bytesUsedInLastBlock) {
        modifyEntry(entry -> entry.putUnsignedShort(FILE_BYTES_USED_IN_LAST_BLOCK, bytesUsedInLastBlock));
    }

    public Date getLastModified() {
        return readEntry().getPascalDate(FILE_MODIFICATION_DATE);
    }
    public void setLastModified(Date lastModified) {
        modifyEntry(entry -> entry.putPascalDate(FILE_MODIFICATION_DATE, lastModified));
    }

    /// Returns the size, in bytes, of this item.
    /// It may be approximate (based off a sector count, for instance).
    @Override
    public long getSize() {
        int blocksUsed = getBlocksUsed();
        int bytesUsedInLastBlock = getBytesUsedInLastBlock();
        return (long) (blocksUsed-1) * device.getGeometry().blockSize() + bytesUsedInLastBlock;
    }

    /// Return the file's data.
    /// This does not include any metadata that may be embedded with the file.
    @Override
    public DataBuffer getDataFork() {
        int firstBlock = getFirstBlock();
        int blocks = getBlocksUsed();
        long size = getSize();
        return device.readRange(firstBlock,blocks).slice(0, (int)size);
    }

    /// Interpret the `FileEntry` as if it were on a ProDOS volume.
    /// The intention is that ProDOS ended up being the common denominator in many tools and formats,
    /// so this allows each file store to manage the interpretation itself.
    @Override
    public ProdosAttributes getProdosAttributes() {
        ProdosAttributes.Builder builder = ProdosAttributes.builder()
                .name(getName())
                .unlocked()
                .size(getSize())
                .modification(getLastModified());
        switch (getType()) {
            case "XDSK" -> builder.fileType(BAD);
            case "CODE" -> builder.fileType(PCD);
            case "TEXT" -> builder.fileType(PTX);
            case "DATA" -> builder.fileType(PDA);
            // The other types don't have distinct related ProDOS file types.
            // There ARE instances of INFO, GRAF, and FOTO but their usage varies.
            default -> builder.BIN(0x0000);
        }
        return builder.get();
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, device);
    }
}
