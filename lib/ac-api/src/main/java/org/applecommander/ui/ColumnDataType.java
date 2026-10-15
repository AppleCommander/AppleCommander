package org.applecommander.ui;

public enum ColumnDataType {
    /// Represents strings or things that are converted to strings.
    STRING,
    /// Represents numbers without decimal places: `byte`, `short`, `int`, `long`.
    INTEGER,
    /// Represents numbers _with_ decimal places: `float`, `double`.
    REAL
}
