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
