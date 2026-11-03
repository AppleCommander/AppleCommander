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
package org.applecommander.change;

import java.util.EventObject;
import java.util.concurrent.CopyOnWriteArrayList;

/// The `ChangeRegistry` is a base class that can be used to support change notification.
/// Usually the interface implements `ChangeSource` and the class(es) implementing the
/// interface extend the `ChangeRegistry`.
public class ChangeRegistry<T extends EventObject> implements ChangeSource<T> {
    private final CopyOnWriteArrayList<ChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void addChangeListener(ChangeListener<T> listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeChangeListener(ChangeListener<T> listener) {
        listeners.remove(listener);
    }

    protected void notifyChangeListeners(T event) {
        for (ChangeListener<T> listener : listeners) {
            listener.stateChanged(event);
        }
    }
}
