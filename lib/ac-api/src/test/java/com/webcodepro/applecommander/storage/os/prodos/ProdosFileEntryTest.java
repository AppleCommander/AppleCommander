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
package com.webcodepro.applecommander.storage.os.prodos;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProdosFileEntryTest {
    @Test
    public void testGetMixedCaseName_GSOS() {
        final String originalName = "DESK.ACCS";
        final int flags = 0xb9c0;
        final String expectedName = "Desk.Accs";
        
        assertEquals(expectedName, ProdosFileEntry.getMixedCaseName(originalName, flags, 
                ProdosFileEntry::calculateGSOSBit));
    }
    
    @Test
    public void testSetMixedCaseName_GSOS() {
        final String originalName = "Desk.Accs";
        final int expectedFlags = 0xb9c0 & 0x7fff;  // the flag is set outside this method
        final String expectedName = "DESK.ACCS";
        
        StringBuilder sb = new StringBuilder(originalName);
        int actualFlags = ProdosFileEntry.setMixedCaseFlags(sb, ProdosFileEntry::calculateGSOSBit);
        assertEquals(expectedName, sb.toString());
        assertEquals(expectedFlags, actualFlags);
    }

    @Test
    public void testGetMixedCaseName_AppleWorks() {
        final String originalName = "MOUSEFIXER.DOCS";
        final int auxtype = 0xee7b;
        final String expectedName = "MouseFixer Docs";
        
        assertEquals(expectedName, ProdosFileEntry.getMixedCaseName(originalName, auxtype, 
                ProdosFileEntry::calculateAppleWorksBit));
    }
    
    @Test
    public void testSetMixedCaseName_AppleWorks() {
        final String originalName = "MouseFixer Docs";
        final int expectedFlags = 0xee7b;
        final String expectedName = "MOUSEFIXER.DOCS";
        
        StringBuilder sb = new StringBuilder(originalName);
        int actualFlags = ProdosFileEntry.setMixedCaseFlags(sb, ProdosFileEntry::calculateAppleWorksBit);
        assertEquals(expectedName, sb.toString());
        assertEquals(expectedFlags, actualFlags);
    }
}
