package org.ai.testing.validation;

/** What part of the response an assertion inspects. */
public enum AssertionType {

    /** The numeric HTTP status code. */
    STATUS_CODE,

    /** The raw response body as text. */
    RESPONSE_BODY,

    /** A single response header, named by the assertion field. */
    HEADER,

    /** A JSONPath expression evaluated against a JSON body. */
    JSON_PATH,

    /** Wall-clock round trip in milliseconds. */
    RESPONSE_TIME,

    /** Body length in bytes. */
    RESPONSE_SIZE,

    /** Shorthand for the {@code Content-Type} response header. */
    CONTENT_TYPE
}
