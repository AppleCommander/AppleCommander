/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.util.function.Function;

public class TranslatorStream extends InputStream {
    private PushbackInputStream sourceStream;
    // Defaults to a no-op transformation
    private Function<Integer,Integer> fn = i -> i;
    
    private TranslatorStream(InputStream sourceStream) {
        this.sourceStream = new PushbackInputStream(sourceStream);
    }
    
    @Override
    public int read() throws IOException {
        int data = sourceStream.read();
        if (data == -1) {
            return -1;
        }
        return fn.apply(data);
    }

    private int setHighBit(int value) {
        return value | 0x80;
    }
    private int clearHighBit(int value) {
        return value & 0x7f;
    }
    private int lfToCr(int value) {
        if (value == '\r') {
            try {
                int nextValue = sourceStream.read();
                if (nextValue == '\n') {
                    return '\r';
                } else {
                    if (nextValue != -1) sourceStream.unread(nextValue);
                    return value;
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return (value == '\n') ? '\r' : value;
    }
    
    public static Builder builder(InputStream sourceStream) {
        return new Builder(sourceStream);
    }
    
    public static class Builder {
        private TranslatorStream stream;
        
        private Builder(InputStream sourceStream) {
            stream = new TranslatorStream(sourceStream);
        }
        private Builder fn(Function<Integer,Integer> andThen) {
            stream.fn = stream.fn.andThen(andThen);
            return this;
        }
        
        public Builder setHighBit() {
            return fn(stream::setHighBit);
        }
        public Builder clearHighBit() {
            return fn(stream::clearHighBit);
        }
        public Builder lfToCr() {
            return fn(stream::lfToCr);
        }
        
        public TranslatorStream get() {
            return stream;
        }
    }
}
