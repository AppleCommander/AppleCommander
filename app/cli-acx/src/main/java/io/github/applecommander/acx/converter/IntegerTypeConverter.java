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

import com.webcodepro.applecommander.util.Host;
import picocli.CommandLine.ITypeConverter;

/** Add support for "$801" and "0x801" instead of just decimal like 2049. */
public class IntegerTypeConverter implements ITypeConverter<Integer> {
	@Override
	public Integer convert(String value) {
		try {
			if (value == null) {
				return null;
			} else if (value.startsWith("$")) {
				return Integer.valueOf(value.substring(1), 16);
			} else if (value.startsWith("0x") || value.startsWith("0X")) {
				return Integer.valueOf(value.substring(2), 16);
			} else {
				return Integer.valueOf(value);
			}
		} catch (NumberFormatException ex) {
			String msg = ex.getMessage();
			if (Host.isLinux() || Host.isMacosx()) {
				msg += " (check shell quoting if using '$')";
			}
			throw new NumberFormatException(msg);
		}
	}
}