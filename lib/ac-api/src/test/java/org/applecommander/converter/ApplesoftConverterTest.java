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

public class ApplesoftConverterTest {
    @Test
    public void testStringMaker() {
        final byte[] program = HexDumpUtil.loadByteSampleFromText("""
                .hexdump
                00000000  1b 08 0a 00 41 24 d0 22  54 48 49 53 20 49 53 20  |....A$."THIS IS |
                00000010  41 20 53 54 52 49 4e 47  22 00 33 08 14 00 42 24  |A STRING".3...B$|
                00000020  d0 22 41 4e 4f 54 48 45  52 20 53 54 52 49 4e 47  |."ANOTHER STRING|
                00000030  22 00 50 08 1e 00 ba e7  28 34 29 3b 22 53 54 4f  |".P.....(4);"STO|
                00000040  52 45 20 53 54 52 49 4e  47 56 41 52 53 22 00 00  |RE STRINGVARS"..|
                00000050  00 c4                                             |..|
                00000052
                """);
        final String expectedText = """
                10 A$ = "THIS IS A STRING"
                20 B$ = "ANOTHER STRING"
                30  PRINT  CHR$ (4);"STORE STRINGVARS"
                """;
        ApplesoftConverter converter = new ApplesoftConverter(DataBuffer.wrap(program), 0x801);
        BASICConverter.EventHandler handler = new BasicText();
        converter.generate(handler);
        assertEquals(expectedText, handler.toString());
    }

    @Test
    public void testArrayMaker() {
        final byte[] program = HexDumpUtil.loadByteSampleFromText("""
                .hexdump
                00000000  0e 08 0a 00 86 41 25 28  35 2c 37 29 00 17 08 14  |.....A%(5,7)....|
                00000010  00 49 25 d0 30 00 22 08  1e 00 81 58 d0 30 c1 35  |.I%.0."....X.0.5|
                00000020  00 2d 08 28 00 81 59 d0  30 c1 37 00 39 08 32 00  |.-.(..Y.0.7.9.2.|
                00000030  49 25 d0 49 25 c8 31 00  48 08 3c 00 41 25 28 58  |I%.I%.1.H.<.A%(X|
                00000040  2c 59 29 d0 49 25 00 51  08 46 00 82 59 2c 58 00  |,Y).I%.Q.F..Y,X.|
                00000050  62 08 5a 00 ba 22 53 41  56 49 4e 47 2e 2e 2e 22  |b.Z.."SAVING..."|
                00000060  00 7e 08 64 00 ba e7 28  34 29 3b 22 53 54 4f 52  |.~.d...(4);"STOR|
                00000070  45 20 41 52 52 41 59 56  41 52 53 22 00 00 00 c4  |E ARRAYVARS"....|
                00000080
                """);
        final String expectedText = """
                10  DIM A%(5,7)
                20 I% = 0
                30  FOR X = 0 TO 5
                40  FOR Y = 0 TO 7
                50 I% = I% + 1
                60 A%(X,Y) = I%
                70  NEXT Y,X
                90  PRINT "SAVING..."
                100  PRINT  CHR$ (4);"STORE ARRAYVARS"
                """;
        ApplesoftConverter converter = new ApplesoftConverter(DataBuffer.wrap(program), 0x801);
        BASICConverter.EventHandler handler = new BasicText();
        converter.generate(handler);
        assertEquals(expectedText, handler.toString());
    }

    public static class BasicText implements BASICConverter.EventHandler {
        private final StringBuilder program = new StringBuilder();

        @Override
        public String toString() {
            return program.toString();
        }

        @Override
        public void startOfLine(String lineNumber) {
            program.append(lineNumber);
        }

        @Override
        public void rawBytes(byte[] data) {
            program.append("*** HEX BYTES ***");
        }

        @Override
        public void token(String text) {
            program.append(text);
        }

        @Override
        public void comment(String comment) {
            program.append(comment);
        }

        @Override
        public void endOfLine() {
            program.append("\n");
        }

        @Override
        public void string(String string) {
            program.append(string);
        }

        @Override
        public void targetLine(String lineNumber) {
            program.append(lineNumber);
        }

        @Override
        public void identifier(String identifier) {
            program.append(identifier);
        }

        @Override
        public void number(String number) {
            program.append(number);
        }

        @Override
        public void syntax(String syntax) {
            program.append(syntax);
        }
    }
}
