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
