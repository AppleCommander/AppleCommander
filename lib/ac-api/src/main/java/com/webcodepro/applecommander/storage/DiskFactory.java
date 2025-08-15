package com.webcodepro.applecommander.storage;

import org.applecommander.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface DiskFactory {
    void inspect(Context ctx);

    class Context {
        public final Source source;
        public final List<FormattedDisk> disks = new ArrayList<>();

        public Context(Source source) {
            this.source = source;
        }
    }

    static List<FormattedDisk> inspect(Source source) {
        Context ctx = new Context(source);
        ServiceLoader<DiskFactory> factories = ServiceLoader.load(DiskFactory.class);
        factories.forEach(factory -> factory.inspect(ctx));
        return ctx.disks;
    }
}
