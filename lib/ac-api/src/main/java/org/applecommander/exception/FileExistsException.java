package org.applecommander.exception;

public class FileExistsException extends RuntimeException {
    public FileExistsException() {
    }

    public FileExistsException(String fmt, Object ...args) {
        super(String.format(fmt, args));
    }

    public FileExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileExistsException(Throwable cause) {
        super(cause);
    }
}
