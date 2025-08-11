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

import org.junit.jupiter.api.Test;

import static io.github.applecommander.acx.converter.DataSizeConverter.KB;
import static io.github.applecommander.acx.converter.DataSizeConverter.MB;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
