/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RangeTest {
    @Test
    public void testToString() {
        assertEquals("1", new Range(1,1).toString());
        assertEquals("1-5", new Range(1,5).toString());
    }
    
    @Test
    public void testFrom() {
        assertEquals("[1-3, 5, 7, 9-12]", Range.from(Arrays.asList(1,2,3,5,7,9,10,11,12)).toString());
    }

    @Test
    public void testFromUnordered() {
        assertEquals("[1-3, 5, 7, 9-12]", Range.from(Arrays.asList(9,10,1,5,2,12,3,11,7)).toString());
    }
    
    @Test
    public void testFromEmpty() {
        assertEquals("[]", Range.from(Arrays.asList()).toString());
    }
}
