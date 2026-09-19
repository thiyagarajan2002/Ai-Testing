package org.ai.testing.testcase.dto;

/** The outcome of a test case, suite or run. */
public enum TestStatus {

    /** Every assertion passed. */
    PASSED,

    /** The request completed but at least one assertion failed. */
    FAILED,

    /** The request itself could not be completed. */
    ERROR,

    /** Deliberately not run: disabled, filtered out, or skipped after a failure. */
    SKIPPED;

    public boolean isFailure() {
        return this == FAILED || this == ERROR;
    }

    /** CSS-friendly lowercase token used by the HTML report. */
    public String token() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
