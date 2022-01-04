package io.github.applecommander.acx.converter;

import io.github.applecommander.acx.SystemType;
import picocli.CommandLine.ITypeConverter;

public class SystemTypeConverter implements ITypeConverter<SystemType> {
	@Override
	public SystemType convert(String value) throws Exception {
		return SystemType.valueOf(value.replaceAll("[^a-zA-Z]", "").toUpperCase());
	}
}