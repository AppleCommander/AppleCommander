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
package org.applecommander.converter;

import org.applecommander.util.DataBuffer;
import org.applecommander.util.HexDumpUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BusinessBASICConverterTest {
    @Test
    public void testHello() {
        final byte[] program = HexDumpUtil.loadByteSampleFromText("""
                .hexdump
                00000000  00 00 35 05 00 c0 20 57  41 50 20 2f 2f 2f 20 53  |..5... WAP /// S|
                00000010  49 47 20 48 45 4c 4c 4f  20 50 52 4f 47 52 41 4d  |IG HELLO PROGRAM|
                00000020  2e 20 20 55 50 44 41 54  45 44 20 34 2f 31 32 2f  |.  UPDATED 4/12/|
                00000030  38 39 20 44 47 4f 00 07  0a 00 8b 3a ae 00 1b 14  |89 DGO.....:....|
                00000040  00 88 23 31 ff 9c 83 2c  22 2e 44 31 2f 68 65 6c  |..#1...,".D1/hel|
                00000050  6c 6f 2e 74 65 78 74 22  00 0b 1e 00 c2 9e 23 31  |lo.text"......#1|
                00000060  ba 37 30 00 0d 28 00 83  23 31 3b 45 52 49 4e 24  |.70..(..#1;ERIN$|
                00000070  00 0a 32 00 d9 45 52 49  4e 24 00 07 3c 00 ba 34  |..2..ERIN$..<..4|
                00000080  30 00 07 46 00 89 23 31  00 2f 50 00 9a 3d 32 33  |0..F..#1./P..=23|
                00000090  3a 99 3d 32 35 3a d9 22  50 52 45 53 53 20 41 4e  |:.=25:."PRESS AN|
                000000a0  59 20 4b 45 59 20 54 4f  20 43 4f 4e 54 49 4e 55  |Y KEY TO CONTINU|
                000000b0  45 2e 22 3a df 47 24 00  05 55 00 ae 00 2d 5a 00  |E.":.G$..U...-Z.|
                000000c0  88 23 32 ff 9c 83 2c 22  2e 44 31 2f 46 4c 41 53  |.#2...,".D1/FLAS|
                000000d0  48 4e 41 4d 45 2e 44 41  54 22 3a 83 23 32 3b 46  |HNAME.DAT":.#2;F|
                000000e0  4c 41 53 48 24 3a 89 23  32 00 25 64 00 81 46 4c  |LASH$:.#2.%d..FL|
                000000f0  41 53 48 3d 31 ff 81 31  30 3a c0 20 46 4c 41 53  |ASH=1..10:. FLAS|
                00000100  48 45 52 20 53 55 42 52  4f 55 54 49 4e 45 00 0f  |HER SUBROUTINE..|
                00000110  6e 00 81 53 4c 4f 57 3d  31 ff 81 31 30 00 1d 78  |n..SLOW=1..10..x|
                00000120  00 bb 46 4c 41 53 48 2f  32 3d ff 9e 46 4c 41 53  |..FLASH/2=..FLAS|
                00000130  48 2f 32 29 ff 84 b5 3a  db b4 00 0d 82 00 9a 3d  |H/2)...:.......=|
                00000140  31 31 3a 99 3d 30 37 00  0b 8c 00 d9 46 4c 41 53  |11:.=07.....FLAS|
                00000150  48 24 00 09 96 00 82 53  4c 4f 57 00 0a a0 00 82  |H$.....SLOW.....|
                00000160  46 4c 41 53 48 00 07 aa  00 b4 3a ae 00 15 b4 00  |FLASH.....:.....|
                00000170  c7 22 2e 64 31 2f 4d 65  6e 75 2e 4d 61 6b 65 72  |.".d1/Menu.Maker|
                00000180  22 00 00                                          |"..|
                00000183
                """);
        final String expectedText = """
                5  REM WAP /// SIG HELLO PROGRAM.  UPDATED 4/12/89 DGO
                10  TEXT:HOME
                20  OPEN #1 AS INPUT,".D1/hello.text"
                30  ON EOF #1 GOTO 70
                40  INPUT #1;ERIN$
                50  PRINT ERIN$
                60  GOTO 40
                70  CLOSE #1
                80  VPOS=23:HPOS=25:PRINT "PRESS ANY KEY TO CONTINUE.":GET G$
                85  HOME
                90  OPEN #2 AS INPUT,".D1/FLASHNAME.DAT":INPUT #2;FLASH$:CLOSE #2
                100  FOR FLASH=1 TO 10:REM FLASHER SUBROUTINE
                110    FOR SLOW=1 TO 10
                120      IF FLASH/2=INT(FLASH/2) THEN INVERSE:ELSE NORMAL
                130      VPOS=11:HPOS=07
                140      PRINT FLASH$
                150      NEXT SLOW
                160    NEXT FLASH
                170  NORMAL:HOME
                180  RUN ".d1/Menu.Maker"
                """;
        BusinessBASICConverter converter = new BusinessBASICConverter(DataBuffer.wrap(program));
        BASICConverter.EventHandler handler = new ApplesoftConverterTest.BasicText();
        converter.generate(handler);
        assertEquals(expectedText, handler.toString());
    }

}
