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
package org.applecommander.filestore;

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A DisplayColumn supports formatted output from various FileStores in a 
 * dynamic fashion. The intent is to provide a flexible display interface
 * that does not rely on hard-coded implementations.
 */
public record DisplayColumn(String headerText, Alignment alignment, Function<FileEntry,Object> valueFn, String fmt, Mode... modes) {
	/**
	 * Indicates if this DisplayColumn supports the mode. In this manner, the columns
	 * can be filtered out. Or it can be ignored to display everything.
	 */
	public boolean supports(Mode mode) {
		return Set.of(modes).contains(mode);
	}
	/**
	 * Helper method to convert the column into a formatted string for display.
	 */
	public String formatAsText(FileEntry fileEntry) {
		return String.format(fmt, valueFn.apply(fileEntry));
	}

	/**
	 * Indicates the display mode to help filter columns to users' preference.
	 */
	public enum Mode {
		NATIVE,
		DETAIL
	}
	/**
	 * Indicates how this column should be aligned.
	 */
	public enum Alignment {
		LEFT,
		CENTER,
		RIGHT
	}

	public static <T extends FileEntry> Builder<T> builder(Class<T> clazz) {
		return new Builder<>(clazz);
	}
	public static class Builder<T extends FileEntry> {
		private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
		private final Class<T> clazz;
		private final List<DisplayColumn> columns = new ArrayList<>();

		private Builder(Class<T> clazz) {
			this.clazz = clazz;
		}

		public List<DisplayColumn> toList() {
			return columns;
		}

		/**
		 * A helper method that allows a short-cut of 0 modes meaning ALL modes.
		 */
		private Builder<T> add(String name, Alignment alignment, Function<FileEntry,Object> valueFn, String fmt, Mode ...modes) {
			if (modes.length == 0) {
				modes = new Mode[] { Mode.NATIVE, Mode.DETAIL };
			}
			columns.add(new DisplayColumn(name, alignment, valueFn, fmt, modes));
			return this;
		}

		public Builder<T> addIntField(String name, Function<T,Integer> valueFn, Mode ...modes) {
			return addIntField(name, valueFn, "%d", modes);
		}
		public Builder<T> addIntField(String name, Function<T,Integer> valueFn, String fmt, Mode ...modes) {
			return add(name, Alignment.RIGHT, entry -> convert(entry, valueFn), fmt, modes);
		}
		public Builder<T> addStringField(String name, Function<T,String> valueFn, Mode ...modes) {
			return add(name, Alignment.LEFT, entry -> convert(entry, valueFn), "%s", modes);
		}
		public Builder<T> addStringField(String name, Alignment alignment, Function<T,String> valueFn, Mode ...modes) {
			return add(name, alignment, entry -> convert(entry, valueFn), "%s", modes);
		}
		public Builder<T> addPercentField(String name, Function<T,Number> numeratorFn, Function<T,Number> denominatorFn, String fmt, Mode ...modes) {
			return add(name, Alignment.RIGHT, entry -> percentage(entry, numeratorFn, denominatorFn), fmt, modes);
		}
		public Builder<T> addFileTimeField(String name, Function<T,FileTime> valueFn, Mode ...modes) {
			return add(name, Alignment.CENTER, entry -> formatFileTime(entry, valueFn), "%s", modes);
		}
		public Builder<T> addLongField(String name, Function<T,Long> valueFn, Mode ...modes) {
			return addLongField(name, valueFn, "%d", modes);
		}
		public Builder<T> addLongField(String name, Function<T,Long> valueFn, String fmt, Mode ...modes) {
			columns.add(new DisplayColumn(name, Alignment.RIGHT, entry -> convert(entry, valueFn), fmt, modes));
			return this;
			return add(name, Alignment.RIGHT, entry -> convert(entry, valueFn), fmt, modes);
		}
		}

		private <S> S convert(FileEntry fileEntry, Function<T,S> valueFn) {
			T typedFileEntry = clazz.cast(fileEntry);
			return valueFn.apply(typedFileEntry);
		}
		private double percentage(FileEntry fileEntry, Function<T,Number> numeratorFn, Function<T,Number> denominatorFn) {
			T typedFileEntry = clazz.cast(fileEntry);
			Number numerator = numeratorFn.apply(typedFileEntry);
			Number denominator = denominatorFn.apply(typedFileEntry);
			return (1.0 - numerator.doubleValue() / denominator.doubleValue()) * 100.0;
		}
		private String formatFileTime(FileEntry fileEntry, Function<T,FileTime> valueFn) {
			T typedFileEntry = clazz.cast(fileEntry);
			// https://mkyong.com/java/how-to-format-filetime-in-java/
			LocalDateTime localDateTime = valueFn.apply(typedFileEntry)
					.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime();
			FileTime fileTime = valueFn.apply(typedFileEntry);
			if (fileTime == null) {
				return "- No Date -";
			}
			LocalDateTime localDateTime = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			return localDateTime.format(FILE_TIME_FORMATTER);
		}
			return localDateTime.format(FILE_TIME_FORMATTER);
		}
 	}
}