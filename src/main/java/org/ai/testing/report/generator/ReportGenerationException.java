package org.ai.testing.report.generator;

/** Raised when a report file could not be produced. */
public class ReportGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReportGenerationException(String message) {
        super(message);
    }

    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
