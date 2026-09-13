package org.applecommander.usage;

import java.util.Objects;
import java.util.function.IntSupplier;

/// Provides a generic DiskUsage interface. Note that the assumption of free and used applies
/// only to disks. Each disk image will supply a more specific usage subclass. Additionally,
/// the assumption is that supplying a `used` and `free` count is sufficient. That is, `total`
/// is the addition of the two and the byte related numbers are just multiplied by the unit size.
/// Finally, `used` and `free` are supplied as functions to allow the disk usage to be a long-term
/// construct.
/// @see BlockUsage
/// @see SectorUsage
public abstract class DiskUsage {
    private final int unitSize;
    private final IntSupplier used;
    private final IntSupplier free;

    protected DiskUsage(int unitSize, IntSupplier used, IntSupplier free) {
        Objects.requireNonNull(used);
        Objects.requireNonNull(free);
        this.unitSize = unitSize;
        this.used = used;
        this.free = free;
    }

    public int getTotal() {
        return getUsed() + getFree();
    }
    public int getUsed() {
        return used.getAsInt();
    }
    public int getFree() {
        return free.getAsInt();
    }
    public int getTotalBytes() {
        return getTotal() * unitSize;
    }
    public int getBytesUsed() {
        return getUsed() * unitSize;
    }
    public int getBytesFree() {
        return getFree() * unitSize;
    }
}
