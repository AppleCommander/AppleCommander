package org.applecommander.exception;

public class DiskFullException extends RuntimeException {
    public DiskFullException() {
    }

    public DiskFullException(String fmt, Object ...args) {
        super(String.format(fmt, args));
    }

    public DiskFullException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiskFullException(Throwable cause) {
        super(cause);
    }
}
