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
