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

import org.applecommander.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Disks {
    private static final List<DiskFactory> FACTORIES;
    static {
        FACTORIES = new ArrayList<>();
        for (DiskFactory factory : ServiceLoader.load(DiskFactory.class)) {
            FACTORIES.add(factory);
        }
    }

    /**
     * This call is a shim for the AntTask to populate its known factories since ServiceLoader
     * doesn't appear to work within the Ant classpath.
     */
    public static void setFactories(DiskFactory... factories) {
        FACTORIES.clear();
        FACTORIES.addAll(List.of(factories));
    }

    /**
     * Standardized FormattedDisk creation. Uses the ServiceLoader mechanism to identify
     * all potential FormattedDisk factories.
     */
    static List<FormattedDisk> inspect(Source source) {
        DiskFactory.Context ctx = new DiskFactory.Context(source);
        FACTORIES.forEach(factory -> {
            try {
                factory.inspect(ctx);
            } catch (Throwable t) {
                // ignore it
            }
        });
        return ctx.disks;
    }
}
