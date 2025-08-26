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

import java.util.*;

/**
 * Sources is a hook into the Source discovery and construction logic.
 */
public class Sources {
    private static final List<Source.Factory> FACTORIES;
    static {
         FACTORIES = new ArrayList<>();
         for (Source.Factory factory : ServiceLoader.load(Source.Factory.class)) {
             FACTORIES.add(factory);
         }
    }

    /**
     * This call is a shim for the AntTask to populate its known factories since ServiceLoader
     * doesn't appear to work within the Ant classpath.
     */
    public static void setFactories(Source.Factory... factories) {
        FACTORIES.clear();
        FACTORIES.addAll(Arrays.asList(factories));
    }

    /**
     * Standardized Source creation. Uses the ServiceLoader mechanism to identify
     * all potential Source factories.  Note that this supports a Source being
     * passed as well (which ensures any wrapping layers are added, such as 2IMG
     * or DiskCopy).
     */
    public static Optional<Source> create(Object object) {
        // First: Create source from a generalized object (thus, can be Path, Entry, URL, etc)
        Optional<Source> source = Optional.empty();
        if (object instanceof Source objSource) {
            source = Optional.of(objSource);
        }
        else {
            for (Source.Factory factory : FACTORIES) {
                source = factory.fromObject(object);
                if (source.isPresent()) {
                    break;
                }
            }
        }
        // Second: Once we have a source, run a pass again, checking if there is a wrapper we need (2IMG, DiskCopy, etc)
        if (source.isPresent()) {
            for (Source.Factory factory : FACTORIES) {
                Optional<Source> fromSource = factory.fromSource(source.get());
                if (fromSource.isPresent()) {
                    return fromSource;
                }
            }
        }
        return source;
    }

}
