
package com.serena.rlc.provider.filesystem.exception;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemClientException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FilesystemClientException.class);

    public FilesystemClientException() {
    }

    public FilesystemClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilesystemClientException(String message) {
        super(message);
    }

    public FilesystemClientException(Throwable cause) {
        super(cause);
    }
}
