/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.applecommander.ui;

import org.applecommander.filestore.FileEntry;

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/// A `DisplayColumn` supports formatted output from various `FileStore`s in a
/// dynamic fashion. The intent is to provide a flexible display interface
/// across interfaces that does not rely on hard-coded implementations.
///
/// The `Builder` should be utilized to construct the `DisplayColumn`.
///
/// The read-only settings include:
/// * `title` is the column title
/// * `alignment` is the alignment of the column data
/// * `mode` is the display mode that the column applies to (native, detail, or both)
/// * `columnDataType` is the (simplified) data type
/// * `getValueFn` is the method reference to get the column's value
/// * `formatFn` is the function that takes the column's value and prepares it for display
///
/// The editable attributes are:
/// * `setValueFn` is the method reference to set the value; note that the builder wraps
///   the actual setter with a conversion function
/// * `editInLine` indicates if this specific cell can be edited in a grid; at this time
///   this is only used for the name field
/// * `maxLength` is the maximum length of the input
/// * `options` are the options available for selection
/// * `validCharacters` is a `CharacterMapper` to transform input text
/// * `validInputPattern` is a `Pattern` used to validate the input
///
public class DisplayColumn {
    private final String title;
    private final ColumnAlignment alignment;
    private final Set<DisplayMode> modes;
    private final ColumnDataType columnDataType;
    private final Function<FileEntry,Object> getValueFn;
    private final Function<Object,String> formatFn;
    // These are all editable attributes
    private final BiConsumer<FileEntry,String> setValueFn;
    private final boolean editable;
    private final boolean editInLine;
    private final int maxLength;
    private final List<ColumnOption> options;
    private final CharacterMapper validCharacters;
    private final Pattern validInputPattern;

    private DisplayColumn(String title, ColumnAlignment alignment, Set<DisplayMode> modes, ColumnDataType columnDataType,
                          Function<FileEntry,Object> getValueFn, Function<Object,String> formatFn, BiConsumer<FileEntry,String> setValueFn,
                          boolean editInLine, int maxLength, List<ColumnOption> options,
                          CharacterMapper validCharacters, Pattern validInputPattern) {
        Objects.requireNonNull(title);
        Objects.requireNonNull(alignment);
        Objects.requireNonNull(modes);
        Objects.requireNonNull(columnDataType);
        Objects.requireNonNull(getValueFn);
        // setValueFn is null when not editable
        Objects.requireNonNull(formatFn);
        Objects.requireNonNull(options);
        Objects.requireNonNull(validCharacters);
        // validInputPattern is null when not used
        this.title = title;
        this.alignment = alignment;
        this.modes = modes;
        this.columnDataType = columnDataType;
        this.getValueFn = getValueFn;
        this.formatFn = formatFn;

        this.editable = setValueFn != null || editInLine || maxLength > 0 || !options.isEmpty() || validInputPattern != null;
        this.setValueFn = setValueFn;
        this.editInLine = editInLine;
        this.maxLength = maxLength;
        this.options = Collections.unmodifiableList(options);
        this.validCharacters = validCharacters;
        this.validInputPattern = validInputPattern;
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
    public void setValue(FileEntry fileEntry, String value) {
        setValueFn.accept(fileEntry, value);
    }

    /// Indicates if this field is editable
    public boolean isEditable() {
        return editable;
    }
    /// Indicates this column can be edited directly in the file viewer. Typically just the name.
    public boolean canEditInLine() {
        return editInLine;
    }
    /// Indicates the maximum length for the input. 0 means not applicable.
    public int getMaxLength() {
        return maxLength;
    }
    /// Get list of options (if any) when editing.
    public List<ColumnOption> getOptions() {
        return options;
    }
    /// Get the CharacterMapper that maps the input string into the valid set of characters.
    public CharacterMapper getValidCharacters() {
        return validCharacters;
    }
    /// Get the valid input regex. If input does not match this regex, it will be ignored.
    /// The intent is to be able to limit to numbers, or the valid filename characters.
    public Pattern getValidInputPattern() {
        return validInputPattern;
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
        public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        public static final DateTimeFormatter DDMONYY_FORMATTER = DateTimeFormatter.ofPattern("dd-LLL-yy");
        public static final Pattern VALID_HEX_NUMBER = Pattern.compile("^(?i)0x[0-9a-f]+|[$][0-9a-f]+|[0-9a-f]+H?$");

        private final Builder<T> builder;
        private final ColumnAlignment alignment;
        private final String title;
        private ColumnDataType columnDataType;
        private Function<T, ?> getValueFn;
        private BiConsumer<FileEntry,String> setValueFn;
        private Function<Object,String> formatFn;
        private DisplayMode[] modes;
        private boolean editInLine = false;
        private int maxLength = 0;
        private final List<ColumnOption> options = new ArrayList<>();
        private CharacterMapper validCharacters;
        private Pattern validInputPattern;

        private ColumnBuilder(Builder<T> builder, ColumnAlignment alignment, String title) {
            Objects.requireNonNull(builder);
            Objects.requireNonNull(alignment);
            Objects.requireNonNull(title);
            this.builder = builder;
            this.alignment = alignment;
            this.title = title;
        }

        public ColumnBuilder<T> string(Function<T,String> getValueFn) {
            Objects.requireNonNull(getValueFn);
            this.columnDataType = ColumnDataType.STRING;
            this.getValueFn = getValueFn;
            this.formatFn = s -> String.format("%s", s);
            return this;
        }
        public ColumnBuilder<T> string(Function<T,String> getValueFn, BiConsumer<T,String> setValueFn) {
            Objects.requireNonNull(getValueFn);
            Objects.requireNonNull(setValueFn);
            this.setValueFn = (e,t) -> setValueFn.accept(builder.fileEntryType.cast(e), t);
            return string(getValueFn);
        }
        public ColumnBuilder<T> integer(Function<T,Number> getValueFn, String fmt) {
            Objects.requireNonNull(getValueFn);
            Objects.requireNonNull(fmt);
            this.columnDataType = ColumnDataType.INTEGER;
            this.getValueFn = getValueFn;
            this.formatFn = n -> String.format(fmt, n);
            return this;
        }
        public ColumnBuilder<T> integer(Function<T,Number> getValueFn, String fmt, BiConsumer<T,Integer> setValueFn) {
            Objects.requireNonNull(getValueFn);
            Objects.requireNonNull(fmt);
            Objects.requireNonNull(setValueFn);
            this.setValueFn = (e,t) -> setInteger(e, t, setValueFn);
            return integer(getValueFn, fmt);
        }
        public ColumnBuilder<T> real(Function<T,Number> getValueFn, String fmt) {
            Objects.requireNonNull(getValueFn);
            Objects.requireNonNull(fmt);
            this.columnDataType = ColumnDataType.REAL;
            this.getValueFn = getValueFn;
            this.formatFn = n -> String.format(fmt, n);
            return this;
        }
        public ColumnBuilder<T> dateDDMONYY(Function<T,Date> getValueFn) {
            Objects.requireNonNull(getValueFn);
            this.columnDataType = ColumnDataType.STRING;
            this.getValueFn = getValueFn;
            this.formatFn = this::formatDateDDMONYY;
            return this;
        }
        public ColumnBuilder<T> dateAndTime(Function<T,Date> getValueFn) {
            Objects.requireNonNull(getValueFn);
            this.columnDataType = ColumnDataType.STRING;
            this.getValueFn = getValueFn;
            this.formatFn = this::formatDateAndTime;
            return this;
        }
        public ColumnBuilder<T> fileTime(Function<T, FileTime> getValueFn) {
            Objects.requireNonNull(getValueFn);
            this.columnDataType = ColumnDataType.STRING;
            this.getValueFn = getValueFn;
            this.formatFn = this::formatDateAndTime;
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
        public ColumnBuilder<T> yesNo(Function<T,Boolean> getValueFn) {
            Objects.requireNonNull(getValueFn);
            this.columnDataType = ColumnDataType.STRING;
            this.getValueFn = getValueFn;
            this.formatFn = this::formatBoolean;
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
        public ColumnBuilder<T> restrictToHexValues() {
            this.validCharacters = CharacterMapper.builder()
                    .mapRange('a', 'f', false)
                    .mapRange('A', 'F', false)
                    .mapRange('0', '9', false)
                    .map('$', '$')  // $1234 format
                    .map('x', 'x')  // 0x1234 format
                    .map('X', 'X')  // 0X1234 format
                    .map('h', 'h')  // 1234h format
                    .map('H', 'H')  // 1234H format
                    .build();
            this.validInputPattern = VALID_HEX_NUMBER;
            return this;
        }

        public Builder<T> done() {
            // A helper that allows a short-cut of 0 modes meaning ALL modes.
            if (modes == null || modes.length == 0) {
                modes = new DisplayMode[] { DisplayMode.NATIVE, DisplayMode.DETAIL };
            }
            // Give a default map everything character mapper
            if (validCharacters == null) {
                validCharacters = CharacterMapper.builder().build();
            }
            // This is where generics get sticky. Either the entirety of the API is fully generic, making a bunch
            // of awkward usages with FileStore<?> etc. Or we do magic stuff like this to keep is simple.
            Function<FileEntry, Object> convertedFn = f -> getValueFn.apply(builder.fileEntryType.cast(f));
            builder.columns.add(new DisplayColumn(title, alignment, Set.of(modes), columnDataType, convertedFn, formatFn,
                                                  setValueFn, editInLine, maxLength, options,
                                                  validCharacters, validInputPattern));
            return builder;
        }

        /// This is a helper function to format date and time objects.
        private String formatDateDDMONYY(Object value) {
            if (value == null) {
                return "- No Date -";
            }
            return switch (value) {
                case Date date -> {
                    LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    yield localDateTime.format(DDMONYY_FORMATTER);
                }
                case FileTime fileTime -> {
                    LocalDateTime localDateTime = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    yield localDateTime.format(DDMONYY_FORMATTER);

                }
                default -> "(Unknown)";
            };
        }

        /// This is a helper function to format date and time objects.
        private String formatDateAndTime(Object value) {
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

        private String formatBoolean(Object value) {
            if (value instanceof Boolean bool) {
                return bool ? "Yes" : "No";
            }
            return "???";
        }

        /// This is a helper function to help compute percentages.
        private double percentage(FileEntry fileEntry, Function<T, Number> numeratorFn, Function<T, Number> denominatorFn) {
            T typedFileEntry = builder.fileEntryType.cast(fileEntry);
            Number numerator = numeratorFn.apply(typedFileEntry);
            Number denominator = denominatorFn.apply(typedFileEntry);
            if (denominator == null || denominator.doubleValue() == 0) return 0;
            return (1.0 - numerator.doubleValue() / denominator.doubleValue()) * 100.0;
        }

        private void setInteger(FileEntry fileEntry, String text, BiConsumer<T,Integer> setter) {
            T typedFileEntry = builder.fileEntryType.cast(fileEntry);
            int value = 0;
            if (text.startsWith("0x") || text.startsWith("0X")) {
                value = Integer.parseInt(text.substring(2), 16);
            }
            else if (text.startsWith("$")) {
                value = Integer.parseInt(text.substring(1), 16);
            }
            else if (text.endsWith("h") || text.endsWith("H")) {
                value = Integer.parseInt(text.substring(0, text.length()-1), 16);
            }
            else {
                try {
                    // We may not be able to distinguish base 10 from base 16. Try the more restrictive one first!
                    value = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    value = Integer.parseInt(text, 16);
                }
            }
            setter.accept(typedFileEntry, value);
        }
    }
}
