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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TranslatorStreamTest {
    public static Collection<TestData> data() {
        return Arrays.asList(
                TestData.builder().unixLineEndings().highBitSet().build(),
                TestData.builder().dosLineEndings().highBitSet().build(),
                TestData.builder().appleLineEndings().highBitSet().build(),
                TestData.builder().unixLineEndings().highBitClear().build(),
                TestData.builder().dosLineEndings().highBitClear().build(),
                TestData.builder().appleLineEndings().highBitClear().build()
            );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(TestData testData) throws IOException {
        InputStream is = testData.getSourceStream();
        
        byte[] actual = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            StreamUtil.copy(is, os);
            actual = os.toByteArray();
        }
        
        assertArrayEquals(testData.getExpected(), actual);
    }
    
    public static class TestData {
        private final String name;
        private final byte[] expected;
        private final InputStream sourceStream;
        
        public TestData(String name, byte[] expected, InputStream sourceStream) {
            this.name = name;
            this.expected = expected;
            this.sourceStream = sourceStream;
        }
        public byte[] getExpected() {
            return expected;
        }
        public InputStream getSourceStream() {
            return sourceStream;
        }
        @Override
        public String toString() {
            return name;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String expectedDescription;
            private byte[] expected;
            private String sourceDescription;
            private byte[] source;
            private Supplier<TranslatorStream> configurer;
            
            public Builder unixLineEndings() {
                this.sourceDescription = "unix line endings";
                this.source = "Hello\nWorld!\n".getBytes();
                return this;
            }
            public Builder dosLineEndings() {
                this.sourceDescription = "dos line endings";
                this.source = "Hello\r\nWorld!\r\n".getBytes();
                return this;
            }
            public Builder appleLineEndings() {
                this.sourceDescription = "apple line endings";
                this.source = "Hello\rWorld!\r".getBytes();
                return this;
            }
            /** "Hello^MWorld!^M" with high bit set. */
            public Builder highBitSet() {
                this.expectedDescription = "high bit set";
                this.expected = new byte[] {
                        (byte)0xc8, (byte)0xe5, (byte)0xec, (byte)0xec, 
                        (byte)0xef, (byte)0x8d, (byte)0xd7, (byte)0xef, (byte)0xf2,
                        (byte)0xec, (byte)0xe4, (byte)0xa1, (byte)0x8d 
                    };
                this.configurer = this::highBitSetConfigurer; 
                return this;
            }
            private TranslatorStream highBitSetConfigurer() {
                return TranslatorStream.builder(new ByteArrayInputStream(source)).lfToCr().setHighBit().get();
            }
            /** "Hello^MWorld!^M" with high bit clear. */
            public Builder highBitClear() {
                this.expectedDescription = "high bit clear";
                this.expected = new byte[] {
                        0x48, 0x65, 0x6c, 0x6c, 
                        0x6f, 0x0d, 0x57, 0x6f, 0x72,
                        0x6c, 0x64, 0x21, 0x0d 
                    };
                this.configurer = this::highBitClearConfigurer;
                return this;
            }
            private TranslatorStream highBitClearConfigurer() {
                return TranslatorStream.builder(new ByteArrayInputStream(source)).lfToCr().clearHighBit().get();
            }
            public TestData build() {
                if (this.expected == null || this.expectedDescription == null
                        || this.source == null || this.sourceDescription == null
                        || this.configurer == null) {
                    throw new RuntimeException("Not all variables are set!");
                }
                String name = String.format("%s with %s", this.sourceDescription, this.expectedDescription);
                return new TestData(name, this.expected, this.configurer.get());
            }
        }
    }
}
