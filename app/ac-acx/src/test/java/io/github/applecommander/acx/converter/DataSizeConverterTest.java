package io.github.applecommander.acx.converter;

import static io.github.applecommander.acx.converter.DataSizeConverter.KB;
import static io.github.applecommander.acx.converter.DataSizeConverter.MB;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DataSizeConverterTest {
	@Test
	public void testFormat() {
		assertEquals("1B", DataSizeConverter.format(1));
		assertEquals("100B", DataSizeConverter.format(100));
		assertEquals("2KB", DataSizeConverter.format(2*KB));
		assertEquals("140KB", DataSizeConverter.format(140*KB));
		assertEquals("800KB", DataSizeConverter.format(800*KB));
		assertEquals("5MB", DataSizeConverter.format(5*MB));
	}
	
	@Test
	public void testConvert() throws Exception {
		DataSizeConverter converter = new DataSizeConverter();
		assertEquals(140*KB, (int)converter.convert("140kb"));
		assertEquals(800*KB, (int)converter.convert("800KB"));
		assertEquals(5*MB, (int)converter.convert("5Mb"));
	}
}
