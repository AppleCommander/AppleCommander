package org.applecommander.usage;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntSupplier;

/// Provides block-level usage information. Note that block size can vary from 256 bytes (RDOS) to
/// 512 bytes (Pascal, ProDOS) to 1024 bytes (CP/M), so it must be passed in on the constructor.
public class BlockUsage extends DiskUsage {
    private final Function<Integer,Boolean> usedFn;

    public BlockUsage(int blockSize, IntSupplier used, IntSupplier free, Function<Integer,Boolean> usedFn) {
        super(blockSize, used, free);
        Objects.requireNonNull(usedFn);
        this.usedFn = usedFn;
    }

    public boolean isUsed(int block) {
        return usedFn.apply(block);
    }
}
