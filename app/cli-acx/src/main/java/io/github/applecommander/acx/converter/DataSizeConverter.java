/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx.converter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class DataSizeConverter implements ITypeConverter<Integer> {
	public static final int KB = 1024;
	public static final int MB = KB * 1024;

	@Override
	public Integer convert(String value) throws Exception {
		Pattern pattern = Pattern.compile("([0-9]+)([km]b?)?", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(value);
		if (matcher.matches()) {
			String number = matcher.group(1);
			String kmb = matcher.group(2);
			if (kmb != null) {
				kmb = kmb.toLowerCase();
			}
			int bytes = Integer.parseInt(number);
			if (kmb.startsWith("k")) {
				bytes *= KB;
			}
			else if (kmb.startsWith("m")) {
				bytes *= MB;
			}
			else {
				throw new TypeConversionException(String.format("Unexpected data size '%s'", kmb));
			}
			return bytes;
		}
		throw new TypeConversionException("Expecting format like '140kb' or '5mb'");
	}
	
	public static String format(int value) {
		if (value < KB) {
			return String.format("%,dB", value);
		}
		if (value < MB) {
			return String.format("%,dKB", value / KB);
		}
		return String.format("%,dMB", value / MB);
	}

}
