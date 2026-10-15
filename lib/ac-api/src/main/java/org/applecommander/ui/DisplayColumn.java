package org.applecommander.ui;

import org.applecommander.filestore.FileEntry;

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/// A DisplayColumn supports formatted output from various FileStores in a
/// dynamic fashion. The intent is to provide a flexible display interface
/// across interfaces that does not rely on hard-coded implementations.
public class DisplayColumn {
    private final String title;
    private final ColumnAlignment alignment;
    private final Set<DisplayMode> modes;
    private final ColumnDataType columnDataType;
    private final Function<FileEntry,Object> getValueFn;
    private final Function<Object,String> formatFn;

    private DisplayColumn(String title, ColumnAlignment alignment, Set<DisplayMode> modes, ColumnDataType columnDataType,
                          Function<FileEntry,Object> getValueFn,
                          Function<Object,String> formatFn) {
        Objects.requireNonNull(title);
        Objects.requireNonNull(alignment);
        Objects.requireNonNull(modes);
        Objects.requireNonNull(columnDataType);
        Objects.requireNonNull(getValueFn);
        Objects.requireNonNull(formatFn);
        this.title = title;
        this.alignment = alignment;
        this.modes = modes;
        this.columnDataType = columnDataType;
        this.getValueFn = getValueFn;
        this.formatFn = formatFn;
    }

    /// The column title.
    public String getTitle() {
        return title;
    }
    /// Retrieve the column alignment.
    public ColumnAlignment getAlignment() {
        return alignment;
    }
    /// Indicates if this DisplayColumn supports the DisplayMode. In this manner, the columns
    /// can be filtered out. Or it can be ignored to display everything.
    public boolean supports(DisplayMode mode) {
        return modes.contains(mode);
    }
    /// Indicates the (generic) data type of this column.
    public ColumnDataType getDataType() {
        return columnDataType;
    }
    /// Get the column value for this FileEntry.
    public Object getValue(FileEntry fileEntry) {
        return getValueFn.apply(fileEntry);
    }
    /// Convert the column into a formatted string for display.
    public String getDisplayText(FileEntry fileEntry) {
        return formatObject(getValue(fileEntry));
    }
    /// Explicitly format the object. This, for instance, is used by JavaFX when it is managing the data types.
    public String formatObject(Object object) {
        return formatFn.apply(object);
    }

    public static <T extends FileEntry> Builder<T> builder(Class<T> fileEntryType) {
        return new Builder<>(fileEntryType);
    }

    public static class Builder<T extends FileEntry> {
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

        private final Class<T> fileEntryType;
        private final List<DisplayColumn> columns = new ArrayList<>();

        private Builder(Class<T> fileEntryType) {
            this.fileEntryType = fileEntryType;
        }

        /// A helper method that allows a short-cut of 0 modes meaning ALL modes.
        private Builder<T> add(String name, ColumnAlignment alignment, ColumnDataType columnDataType,
                               Function<T,?> getValueFn, Function<Object,String> formatFn, DisplayMode... modes) {
            if (modes.length == 0) {
                modes = new DisplayMode[] { DisplayMode.NATIVE, DisplayMode.DETAIL };
            }
            // This is where generics get sticky. Either the entirety of the API is fully generic, making a bunch
            // of awkward usages with FileStore<?> etc. Or we do magic stuff like this to keep is simple.
            Function<FileEntry,Object> convertedFn = f -> getValueFn.apply(fileEntryType.cast(f));
            columns.add(new DisplayColumn(name, alignment, Set.of(modes), columnDataType, convertedFn, formatFn));
            return this;
        }

        /// Add a left-aligned string field.
        public Builder<T> leftString(String name, Function<T,String> getValueFn, DisplayMode... modes) {
            return add(name, ColumnAlignment.LEFT, ColumnDataType.STRING, getValueFn, s -> String.format("%s",s), modes);
        }
        /// Add a centered string field.
        public Builder<T> centerString(String name, Function<T,String> getValueFn, DisplayMode... modes) {
            return add(name, ColumnAlignment.CENTER, ColumnDataType.STRING, getValueFn, s -> String.format("%s",s), modes);
        }
        /// Add a right-aligned string field.
        public Builder<T> rightString(String name, Function<T, String> getValueFn, DisplayMode... modes) {
            return add(name, ColumnAlignment.RIGHT, ColumnDataType.STRING, getValueFn, s -> String.format("%s",s), modes);
        }
        /// Add a right-aligned integer field. Mathematical definition of this includes "whole numbers and their negatives".
        /// It covers `byte`, `short`, `int`, `long` and their respective objects.
        public Builder<T> rightNumber(String name, Function<T,Number> getValueFn, String fmt, DisplayMode... modes) {
            return add(name, ColumnAlignment.RIGHT, ColumnDataType.INTEGER, getValueFn, n -> String.format(fmt,n), modes);
        }
        /// Add a centered Date field.
        public Builder<T> centerDate(String name, Function<T,Date> getValueFn, DisplayMode... modes) {
            return add(name, ColumnAlignment.CENTER, ColumnDataType.STRING, getValueFn, this::formatDate, modes);
        }
        /// Add a centered FileTime field.
        public Builder<T> centerFileTime(String name, Function<T, FileTime> getValueFn, DisplayMode... modes) {
            return add(name, ColumnAlignment.CENTER, ColumnDataType.STRING, getValueFn, this::formatDate, modes);
        }
        /// Add a right-aligned percent field.
        public Builder<T> rightPercent(String name, Function<T,Number> numeratorFn, Function<T,Number> denominatorFn,
                                       String fmt, DisplayMode... modes) {
            return add(name, ColumnAlignment.RIGHT, ColumnDataType.REAL,
                    t -> percentage(t,numeratorFn,denominatorFn),
                    d -> String.format(fmt, d), modes);
        }

        public List<DisplayColumn> toList() {
            return columns;
        }

        /// This is a helper function to format date objects.
        private String formatDate(Object value) {
            if (value == null) {
                return "- No Date -";
            }
            return switch (value) {
                case Date date -> {
                    LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    yield localDateTime.format(DATE_TIME_FORMATTER);
                }
                case FileTime fileTime -> {
                    LocalDateTime localDateTime = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    yield localDateTime.format(DATE_TIME_FORMATTER);

                }
                default -> "(Unknown Date)";
            };
        }
        /// This is a helper function to help compute percentages.
        private double percentage(FileEntry fileEntry, Function<T,Number> numeratorFn, Function<T,Number> denominatorFn) {
            T typedFileEntry = fileEntryType.cast(fileEntry);
            Number numerator = numeratorFn.apply(typedFileEntry);
            Number denominator = denominatorFn.apply(typedFileEntry);
            if (denominator == null || denominator.doubleValue() == 0) return 0;
            return (1.0 - numerator.doubleValue() / denominator.doubleValue()) * 100.0;
        }
    }
}
