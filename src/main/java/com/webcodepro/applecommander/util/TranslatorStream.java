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
        return fn.apply(sourceStream.read());
    }

    private int setHighBit(int value) {
        return value | 0x80;
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
        public Builder lfToCr() {
            return fn(stream::lfToCr);
        }
        
        public TranslatorStream get() {
            return stream;
        }
    }
}
