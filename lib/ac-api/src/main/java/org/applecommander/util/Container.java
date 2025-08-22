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
package org.applecommander.util;

import java.util.Optional;

/**
 * Container marks an object that contains other objects for retrieval.
 * Experimental - the idea is to no longer expose everything that *might*
 * apply, but instead test if it's there and react accordingly.
 * <p/>
 * The end goal is to be able to retrieve the various devices - than have to
 * be handled distinctly anyway - and work from that. A DOS disk won't be backed
 * by a BlockDevice (most of the time), so it's counterintuitive that the DOS
 * file system has block level access. Vice versa for ProDOS/Pascal/RDOS/CPM.
 * Not to mention that it doesn't apply to Shrinkit or Zip archives at all.
 */
public interface Container {
    <T> Optional<T> get(Class<T> iface);

    /**
     * This helper method implements the interface logic. Just use it with:
     * <p/>
     * {@snippet lang=java :
     *     @Override
     *     public <T> Optional<T> get(Class<T> iface) {
     *         return Container.get(iface, this, object1, object2);
     *     }
     * }
     */
    static <T> Optional<T> get(Class<T> iface, Object... objects) {
        // First: Check if we have an the Class<T> type...
        for (Object object : objects) {
            if (iface.isInstance(object)) {
                return Optional.of(iface.cast(object));
            }
        }
        // Second: Check if any of these objects are a Container...
        for (Object object : objects) {
            if (object instanceof Container container) {
                Optional<T> opt = container.get(iface);
                if (opt.isPresent()) {
                    return opt;
                }
            }
        }
        return Optional.empty();
    }
}
