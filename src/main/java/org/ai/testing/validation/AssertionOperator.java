package org.ai.testing.validation;

/** How an actual value is compared with the expected value. */
public enum AssertionOperator {

    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    MATCHES,
    EMPTY,
    NOT_EMPTY,
    EXISTS,
    NOT_EXISTS,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,

    /** Expected value is a comma-separated set the actual value must belong to. */
    IN,

    /** Expected value is a comma-separated set the actual value must not belong to. */
    NOT_IN;

    /** True when the operator ignores the expected value entirely. */
    public boolean isUnary() {
        return this == EMPTY || this == NOT_EMPTY
                || this == EXISTS || this == NOT_EXISTS;
    }

    /** True when both sides must be parsed as numbers. */
    public boolean isNumeric() {
        return this == LESS_THAN || this == LESS_THAN_OR_EQUAL
                || this == GREATER_THAN || this == GREATER_THAN_OR_EQUAL;
    }
}
