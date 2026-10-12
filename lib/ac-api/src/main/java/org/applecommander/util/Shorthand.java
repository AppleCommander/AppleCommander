package org.applecommander.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Shorthand {
    /// This is a shorthand method to only set non-null values.
    public static <T> void copy(Supplier<T> supplier, Consumer<T> consumer) {
        T value = supplier.get();
        if (value != null) {
            consumer.accept(value);
        }
    }
}
