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
    private final boolean editInLine;
    private final int maxLength;
    private final List<ColumnOption> options;

    private DisplayColumn(String title, ColumnAlignment alignment, Set<DisplayMode> modes, ColumnDataType columnDataType,
                          Function<FileEntry,Object> getValueFn, Function<Object,String> formatFn, boolean editInLine,
                          int maxLength, List<ColumnOption> options) {
        Objects.requireNonNull(title);
        Objects.requireNonNull(alignment);
        Objects.requireNonNull(modes);
        Objects.requireNonNull(columnDataType);
        Objects.requireNonNull(getValueFn);
        Objects.requireNonNull(formatFn);
        Objects.requireNonNull(options);
        this.title = title;
        this.alignment = alignment;
        this.modes = modes;
        this.columnDataType = columnDataType;
        this.getValueFn = getValueFn;
        this.formatFn = formatFn;
        this.editInLine = editInLine;
        this.maxLength = maxLength;
        this.options = Collections.unmodifiableList(options);
    }

    public boolean isEditable() {
        return editInLine || maxLength > 0 || !options.isEmpty();
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
    /// Indicates this column can be edited directly in the file viewer. Typically just the name.
    public boolean canEditInLine() {
        return editInLine;
    }
    /// Get list of options (if any) when editing.
    public List<ColumnOption> getOptions() {
        return options;
    }

    public static <T extends FileEntry> Builder<T> builder(Class<T> fileEntryType) {
        return new Builder<>(fileEntryType);
    }

    public static class Builder<T extends FileEntry> {
        private final Class<T> fileEntryType;
        private final List<DisplayColumn> columns = new ArrayList<>();

        private Builder(Class<T> fileEntryType) {
            Objects.requireNonNull(fileEntryType);
            this.fileEntryType = fileEntryType;
        }

        public ColumnBuilder<T> left(String title) {
            Objects.requireNonNull(title);
            return new ColumnBuilder<>(this, ColumnAlignment.LEFT, title);
        }
        public ColumnBuilder<T> center(String title) {
            Objects.requireNonNull(title);
            return new ColumnBuilder<>(this, ColumnAlignment.CENTER, title);
        }
        public ColumnBuilder<T> right(String title) {
            Objects.requireNonNull(title);
            return new ColumnBuilder<>(this, ColumnAlignment.RIGHT, title);
        }

        public List<DisplayColumn> toList() {
            return columns;
        }
    }

    public static class ColumnBuilder<T extends FileEntry> {
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

        private final Builder<T> builder;
        private final ColumnAlignment alignment;
        private final String title;
        private ColumnDataType columnDataType;
        private Function<T, ?> getValueFn;
        private Function<Object, String> formatFn;
        private DisplayMode[] modes;
        private boolean editInLine = false;
        private int maxLength = -1;
        private final List<ColumnOption> options = new ArrayList<>();

        private ColumnBuilder(Builder<T> builder, ColumnAlignment alignment, String title) {
            Objects.requireNonNull(builder);
            Objects.requireNonNull(alignment);
            Objects.requireNonNull(title);
            this.builder = builder;
            this.alignment = alignment;
            this.title = title;
        }

        public ColumnBuilder<T> string(Function<T, String> getValueFn) {
            Objects.requireNonNull(getValueFn);
            this.columnDataType = ColumnDataType.STRING;
            this.getValueFn = getValueFn;
            this.formatFn = s -> String.format("%s", s);
            return this;
        }
        public ColumnBuilder<T> integer(Function<T, Number> getValueFn, String fmt) {
            Objects.requireNonNull(getValueFn);
            Objects.requireNonNull(fmt);
            this.columnDataType = ColumnDataType.INTEGER;
            this.getValueFn = getValueFn;
            this.formatFn = n -> String.format(fmt, n);
            return this;
        }
        public ColumnBuilder<T> real(Function<T, Number> getValueFn, String fmt) {
            Objects.requireNonNull(getValueFn);
            Objects.requireNonNull(fmt);
            this.columnDataType = ColumnDataType.REAL;
            this.getValueFn = getValueFn;
            this.formatFn = n -> String.format(fmt, n);
            return this;
        }
        public ColumnBuilder<T> date(Function<T, Date> getValueFn) {
            Objects.requireNonNull(getValueFn);
            this.columnDataType = ColumnDataType.STRING;
            this.getValueFn = getValueFn;
            this.formatFn = this::formatDate;
            return this;
        }
        public ColumnBuilder<T> fileTime(Function<T, FileTime> getValueFn) {
            Objects.requireNonNull(getValueFn);
            this.columnDataType = ColumnDataType.STRING;
            this.getValueFn = getValueFn;
            this.formatFn = this::formatDate;
            return this;
        }
        public ColumnBuilder<T> percent(Function<T, Number> numeratorFn, Function<T, Number> denominatorFn, String fmt) {
            Objects.requireNonNull(numeratorFn);
            Objects.requireNonNull(denominatorFn);
            Objects.requireNonNull(fmt);
            this.columnDataType = ColumnDataType.REAL;
            this.getValueFn = t -> percentage(t, numeratorFn, denominatorFn);
            this.formatFn = d -> String.format(fmt, d);
            return this;
        }
        public ColumnBuilder<T> mode(DisplayMode... modes) {
            this.modes = modes;
            return this;
        }
        public ColumnBuilder<T> editInLine() {
            this.editInLine = true;
            return this;
        }
        public ColumnBuilder<T> maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }
        public ColumnBuilder<T> option(String value, String description) {
            Objects.requireNonNull(value);
            Objects.requireNonNull(description);
            this.options.add(new ColumnOption(value, description));
            return this;
        }

        public Builder<T> done() {
            // A helper that allows a short-cut of 0 modes meaning ALL modes.
            if (modes == null || modes.length == 0) {
                modes = new DisplayMode[] { DisplayMode.NATIVE, DisplayMode.DETAIL };
            }
            // This is where generics get sticky. Either the entirety of the API is fully generic, making a bunch
            // of awkward usages with FileStore<?> etc. Or we do magic stuff like this to keep is simple.
            Function<FileEntry, Object> convertedFn = f -> getValueFn.apply(builder.fileEntryType.cast(f));
            builder.columns.add(new DisplayColumn(title, alignment, Set.of(modes), columnDataType, convertedFn, formatFn,
                                                  editInLine, maxLength, options));
            return builder;
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
        private double percentage(FileEntry fileEntry, Function<T, Number> numeratorFn, Function<T, Number> denominatorFn) {
            T typedFileEntry = builder.fileEntryType.cast(fileEntry);
            Number numerator = numeratorFn.apply(typedFileEntry);
            Number denominator = denominatorFn.apply(typedFileEntry);
            if (denominator == null || denominator.doubleValue() == 0) return 0;
            return (1.0 - numerator.doubleValue() / denominator.doubleValue()) * 100.0;
        }
    }
}
