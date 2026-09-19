package org.ai.testing.parser;

/** Raised when a collection or environment file cannot be imported. */
public class CollectionParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CollectionParseException(String message) {
        super(message);
    }

    public CollectionParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
