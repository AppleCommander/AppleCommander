package org.applecommander.util;

/// Represents a function that accepts three integers for operation.
/// Modeled after the various Java functional interfaces.
@FunctionalInterface
public interface TriIntConsumer {
    void accept(int i, int j, int k);
}
