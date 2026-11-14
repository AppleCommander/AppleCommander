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
package org.applecommander.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataBufferTest {
    @Test
    public void testApplesoftReal() {
        // This is -10 and 10 from the Applesoft: Internals document.
        DataBuffer buffer = DataBuffer.wrap(0x84, 0xa0, 0x00, 0x00, 0x00, 0x84, 0x20, 0x00, 0x00, 0x00);
        assertEquals(-10.0, buffer.getApplesoftReal(0));
        assertEquals(10.0, buffer.getApplesoftReal(5));
    }
}
