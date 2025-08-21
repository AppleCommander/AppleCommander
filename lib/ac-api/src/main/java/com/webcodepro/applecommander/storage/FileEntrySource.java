/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
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
package com.webcodepro.applecommander.storage;

import org.applecommander.capability.Capability;
import org.applecommander.hint.Hint;
import org.applecommander.source.Source;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.util.List;
import java.util.Optional;

public class FileEntrySource implements Source {
    private FileEntry fileEntry;
    private DataBuffer buffer;
    private boolean changed;

    public FileEntrySource(FileEntry fileEntry) {
        this.fileEntry = fileEntry;
        this.buffer = DataBuffer.wrap(fileEntry.getFileData());
    }

    @Override
    public boolean can(Capability capability) {
        // TODO?
        return false;
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, fileEntry, buffer);
    }

    @Override
    public boolean is(Hint hint) {
        // Right now this is all SHK/SDK
        return hint == Hint.PRODOS_BLOCK_ORDER;
    }

    @Override
    public int getSize() {
        return fileEntry.getSize();
    }

    @Override
    public String getName() {
        return fileEntry.getFilename();
    }

    @Override
    public DataBuffer readAllBytes() {
        return buffer;
    }

    @Override
    public DataBuffer readBytes(int offset, int length) {
        return buffer.slice(offset, length);
    }

    @Override
    public void writeBytes(int offset, DataBuffer data) {
        buffer.put(offset, data);
        changed = true;
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    @Override
    public void clearChanges() {
        changed = false;
    }

    @Override
    public List<Information> information() {
        return List.of();
    }

    public static class Factory implements Source.Factory {
        @Override
        public Optional<Source> fromObject(Object object) {
            if (object instanceof FileEntry fileEntry) {
                return Optional.of(new FileEntrySource(fileEntry));
            }
            return Optional.empty();
        }

        @Override
        public Optional<Source> fromSource(Source source) {
            return Optional.empty();
        }
    }
}
