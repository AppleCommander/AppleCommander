package org.applecommander.exception;

public class DirectoryFullException extends RuntimeException {
    public DirectoryFullException() {
    }

    public DirectoryFullException(String fmt, Object ...args) {
        super(String.format(fmt, args));
    }

    public DirectoryFullException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirectoryFullException(Throwable cause) {
        super(cause);
    }
}
