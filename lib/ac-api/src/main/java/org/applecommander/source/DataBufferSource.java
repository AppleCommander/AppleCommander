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
package org.applecommander.source;

import org.applecommander.capability.Capability;
import org.applecommander.hint.Hint;
import org.applecommander.util.Container;
import org.applecommander.util.DataBuffer;
import org.applecommander.util.Information;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DataBufferSource implements Source {
    private final DataBuffer dataBuffer;
    private final String name;
    private final Set<Capability> capabilities;
    private final Set<Hint> hints;
    private final List<Information> information;
    boolean changed;

    private DataBufferSource(DataBuffer dataBuffer, String name, Set<Capability> capabilities, Set<Hint> hints,
                             List<Information> information, boolean changed) {
        this.dataBuffer = dataBuffer;
        this.name = name;
        this.capabilities = capabilities;
        this.hints = hints;
        this.information = information;
        this.changed = changed;
    }

    @Override
    public int getSize() {
        return dataBuffer.limit();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataBuffer readAllBytes() {
        return dataBuffer;
    }

    @Override
    public DataBuffer readBytes(int offset, int length) {
        return dataBuffer.slice(offset, length);
    }

    @Override
    public void writeBytes(int offset, DataBuffer data) {
        dataBuffer.put(offset, data);
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
        return information;
    }

    @Override
    public boolean can(Capability capability) {
        return capabilities.contains(capability);
    }

    @Override
    public boolean is(Hint hint) {
        return hints.contains(hint);
    }

    @Override
    public <T> Optional<T> get(Class<T> iface) {
        return Container.get(iface, dataBuffer);
    }

    public static Builder create(DataBuffer dataBuffer, String name) {
        return new Builder(dataBuffer, name);
    }
    public static Builder create(byte[] rawData, String name) {
        return new Builder(DataBuffer.wrap(rawData), name);
    }
    public static Builder create(int imageSize, String name) {
        return new Builder(DataBuffer.create(imageSize), name);
    }

    public static class Builder {
        private final DataBuffer dataBuffer;
        private final String name;
        private Set<Capability> capabilities = Collections.emptySet();
        private Set<Hint> hints = Collections.emptySet();
        private List<Information> information = Collections.emptyList();
        private boolean changed = false;

        private Builder(DataBuffer dataBuffer, String name) {
            this.dataBuffer = dataBuffer;
            this.name = name;
        }
        public Builder capabilities(Capability... capabilities) {
            this.capabilities = Set.of(capabilities);
            return this;
        }
        public Builder hints(Hint... hints) {
            this.hints = Set.of(hints);
            return this;
        }
        public Builder information(Information... information) {
            this.information = List.of(information);
            return this;
        }
        public Builder changed(boolean changed) {
            this.changed = changed;
            return this;
        }
        public DataBufferSource get() {
            return new DataBufferSource(dataBuffer, name, capabilities, hints, information, changed);
        }
    }
}
