package com.webcodepro.applecommander.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class TranslatorStreamTest {
    @Test
    public void testUnixLineEndings() throws IOException {
        byte[] source = "Hello\nWorld!\n".getBytes();
        testAgainstExpected(source);
    }

    @Test
    public void testDosLineEndings() throws IOException {
        byte[] source = "Hello\r\nWorld!\r\n".getBytes();
        testAgainstExpected(source);
    }

    @Test
    public void testAppleLineEndings() throws IOException {
        byte[] source = "Hello\rWorld!\r".getBytes();
        testAgainstExpected(source);
    }

    private void testAgainstExpected(final byte[] source) throws IOException {
        final byte[] expected = { (byte)0xc8, (byte)0xe5, (byte)0xec, (byte)0xec, 
                      (byte)0xef, (byte)0x8d, (byte)0xd7, (byte)0xef, (byte)0xf2,
                      (byte)0xec, (byte)0xe4, (byte)0xa1, (byte)0x8d };
        
        InputStream is = TranslatorStream.builder(new ByteArrayInputStream(source)).lfToCr().setHighBit().get();
        
        byte[] actual = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            StreamUtil.copy(is, os);
            actual = os.toByteArray();
        }
        
        Assert.assertArrayEquals(expected, actual);
    }
}
