package org.ai.testing.executor.common;

/** Raised when a request could not be completed at the transport level. */
public class ApiExecutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ApiExecutionException(String message) {
        super(message);
    }

    public ApiExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
