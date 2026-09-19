package org.ai.testing.dto.common;

/** How a request body is serialised before it is sent. */
public enum BodyMode {
    NONE,
    RAW,
    JSON,
    TEXT,
    XML,
    GRAPHQL,
    URLENCODED,
    FORMDATA
}
