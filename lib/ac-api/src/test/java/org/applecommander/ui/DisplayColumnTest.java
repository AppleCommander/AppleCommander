package org.applecommander.ui;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class DisplayColumnTest {
    @Test
    public void testHexPattern() {
        Pattern pattern = DisplayColumn.ColumnBuilder.VALID_HEX_NUMBER;
        assertNotNull(pattern);
        assertTrue(pattern.matcher("$12ab").matches());
        assertTrue(pattern.matcher("0x12AB").matches());
        assertTrue(pattern.matcher("12AbH").matches());
        assertFalse(pattern.matcher("he110").matches());
    }
}
