package com.webcodepro.applecommander.storage;

import org.applecommander.source.Source;

import java.util.ArrayList;
import java.util.List;

public interface DiskFactory {
    void inspect(Context ctx);

    class Context {
        public final Source source;
        public final List<FormattedDisk> disks = new ArrayList<>();

        public Context(Source source) {
            this.source = source;
        }
    }
}
