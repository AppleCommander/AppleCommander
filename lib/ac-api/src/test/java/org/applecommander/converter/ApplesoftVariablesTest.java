package org.applecommander.converter;

import org.applecommander.util.DataBuffer;
import org.applecommander.util.HexDumpUtil;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApplesoftVariablesTest {
    @Test
    public void testStringVariables() {
        final byte[] data = HexDumpUtil.loadByteSampleFromText("""
            .hexdump
            00000000  0e 00 0e 00 96 41 80 10  f0 95 00 00 42 80 0e e2  |.....A......B...|
            00000010  95 00 00 41 4e 4f 54 48  45 52 20 53 54 52 49 4e  |...ANOTHER STRIN|
            00000020  47 54 48 49 53 20 49 53  20 41 20 53 54 52 49 4e  |GTHIS IS A STRIN|
            00000030  47                                                |G|
            00000031
            """);
        ApplesoftVariables vars = new ApplesoftVariables(DataBuffer.wrap(data));
        assertEquals(List.of("A$", "B$"), vars.getSimpleNames());
        assertEquals(Collections.emptyList(), vars.getArrayNames());

        assertEquals("THIS IS A STRING", vars.getSimpleValue("A$"));
        assertEquals("ANOTHER STRING", vars.getSimpleValue("B$"));
    }

    @Test
    public void testArrayVariables() {
        final byte[] data = HexDumpUtil.loadByteSampleFromText("""
                .hexdump
                00000000  7e 00 15 00 96 c9 80 00  30 00 00 00 58 00 83 40  |~.......0...X..@|
                00000010  00 00 00 59 00 84 00 00  00 00 c1 80 69 00 02 00  |...Y........i...|
                00000020  08 00 06 00 01 00 09 00  11 00 19 00 21 00 29 00  |............!.).|
                00000030  02 00 0a 00 12 00 1a 00  22 00 2a 00 03 00 0b 00  |........".*.....|
                00000040  13 00 1b 00 23 00 2b 00  04 00 0c 00 14 00 1c 00  |....#.+.........|
                00000050  24 00 2c 00 05 00 0d 00  15 00 1d 00 25 00 2d 00  |$.,.........%.-.|
                00000060  06 00 0e 00 16 00 1e 00  26 00 2e 00 07 00 0f 00  |........&.......|
                00000070  17 00 1f 00 27 00 2f 00  08 00 10 00 18 00 20 00  |....'./....... .|
                00000080  28 00 30                                          |(.0|
                00000083
                """);
        ApplesoftVariables vars = new ApplesoftVariables(DataBuffer.wrap(data));
        assertEquals(List.of("I%", "X", "Y"), vars.getSimpleNames());
        assertEquals(List.of("A%"), vars.getArrayNames());

        assertEquals(6.0d, vars.getSimpleValue("X"));
        assertEquals(8.0d, vars.getSimpleValue("Y"));
        assertEquals(48, vars.getSimpleValue("I%"));

        assertEquals(List.of(6,8), vars.getArrayDimensions("A%"));
    }
}
