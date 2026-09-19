package org.ai.testing.dto.common;

/** Authentication schemes understood by {@code AuthApplicator}. */
public enum AuthType {

    /** Explicitly no authentication; stops inheritance from parent scopes. */
    NONE,

    /** {@code Authorization: Bearer <token>}. */
    BEARER,

    /** {@code Authorization: Basic base64(user:password)}. */
    BASIC,

    /** A named key sent as a header or a query parameter. */
    API_KEY,

    /** Defer to the enclosing suite, then to the run. This is the default. */
    INHERIT
}
