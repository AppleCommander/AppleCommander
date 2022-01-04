package io.github.applecommander.acx.converter;

import picocli.CommandLine.ITypeConverter;

/** Add support for "$801" and "0x801" instead of just decimal like 2049. */
public class IntegerTypeConverter implements ITypeConverter<Integer> {
	@Override
	public Integer convert(String value) {
		if (value == null) {
			return null;
		} else if (value.startsWith("$")) {
			return Integer.valueOf(value.substring(1), 16);
		} else if (value.startsWith("0x") || value.startsWith("0X")) {
			return Integer.valueOf(value.substring(2), 16);
		} else {
			return Integer.valueOf(value);
		}
	}
}