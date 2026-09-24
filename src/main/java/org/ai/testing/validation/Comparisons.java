package org.ai.testing.validation;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * One place where every operator is interpreted.
 *
 * <p>The previous design repeated a near-identical switch in each validator,
 * which is how {@code MATCHES}, {@code LESS_THAN} and {@code GREATER_THAN} ended
 * up silently unsupported in some of them. Centralising the logic means a new
 * operator works everywhere at once.</p>
 */
public final class Comparisons {

    private Comparisons() {
    }

    /** The result of applying one operator. */
    public record Outcome(boolean passed, String detail) {

        static Outcome pass() {
            return new Outcome(true, null);
        }

        static Outcome fail(String detail) {
            return new Outcome(false, detail);
        }
    }

    /**
     * Applies {@code operator} to {@code actual} and {@code expected}.
     *
     * @param present whether the value exists at all; drives EXISTS/NOT_EXISTS
     *                and makes every other operator fail cleanly on an absent
     *                value instead of throwing
     */
    public static Outcome apply(AssertionOperator operator,
                                String actual,
                                String expected,
                                boolean present) {

        if (operator == null) {
            return Outcome.fail("Assertion operator cannot be null");
        }

        if (operator == AssertionOperator.EXISTS) {
            return present ? Outcome.pass() : Outcome.fail("Value is not present");
        }
        if (operator == AssertionOperator.NOT_EXISTS) {
            return present ? Outcome.fail("Value is present") : Outcome.pass();
        }
        if (!present) {
            return Outcome.fail("Value is not present");
        }

        String actualValue = actual == null ? "" : actual;

        return switch (operator) {
            case EMPTY -> actualValue.isBlank()
                    ? Outcome.pass()
                    : Outcome.fail("Value is not empty");
            case NOT_EMPTY -> actualValue.isBlank()
                    ? Outcome.fail("Value is empty")
                    : Outcome.pass();
            case EQUALS -> actualValue.equals(nullToEmpty(expected))
                    ? Outcome.pass()
                    : Outcome.fail("Values differ");
            case NOT_EQUALS -> actualValue.equals(nullToEmpty(expected))
                    ? Outcome.fail("Values are equal")
                    : Outcome.pass();
            case CONTAINS -> actualValue.contains(nullToEmpty(expected))
                    ? Outcome.pass()
                    : Outcome.fail("Expected substring not found");
            case NOT_CONTAINS -> actualValue.contains(nullToEmpty(expected))
                    ? Outcome.fail("Unexpected substring found")
                    : Outcome.pass();
            case STARTS_WITH -> actualValue.startsWith(nullToEmpty(expected))
                    ? Outcome.pass()
                    : Outcome.fail("Value does not start with the expected prefix");
            case ENDS_WITH -> actualValue.endsWith(nullToEmpty(expected))
                    ? Outcome.pass()
                    : Outcome.fail("Value does not end with the expected suffix");
            case MATCHES -> matches(actualValue, expected);
            case IN -> inSet(actualValue, expected, true);
            case NOT_IN -> inSet(actualValue, expected, false);
            case LESS_THAN, LESS_THAN_OR_EQUAL,
                 GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                    compareNumbers(operator, actualValue, expected);
            default -> Outcome.fail("Unsupported operator: " + operator);
        };
    }

    private static Outcome matches(String actual, String expected) {
        if (expected == null || expected.isEmpty()) {
            return Outcome.fail("MATCHES requires a regular expression");
        }
        try {
            return Pattern.compile(expected, Pattern.DOTALL).matcher(actual).find()
                    ? Outcome.pass()
                    : Outcome.fail("Value does not match the pattern");
        } catch (PatternSyntaxException e) {
            return Outcome.fail("Invalid regular expression: " + e.getDescription());
        }
    }

    private static Outcome inSet(String actual, String expected, boolean shouldContain) {
        if (expected == null) {
            return Outcome.fail("A comma-separated set of values is required");
        }
        boolean found = Arrays.stream(expected.split(","))
                .map(String::trim)
                .anyMatch(candidate -> candidate.equals(actual));
        if (found == shouldContain) {
            return Outcome.pass();
        }
        return Outcome.fail(shouldContain
                ? "Value is not in the allowed set"
                : "Value is in the forbidden set");
    }

    private static Outcome compareNumbers(AssertionOperator operator,
                                          String actual, String expected) {
        Double actualNumber = toNumber(actual);
        Double expectedNumber = toNumber(expected);
        if (actualNumber == null) {
            return Outcome.fail("Actual value is not numeric: " + actual);
        }
        if (expectedNumber == null) {
            return Outcome.fail("Expected value is not numeric: " + expected);
        }
        int comparison = Double.compare(actualNumber, expectedNumber);
        boolean passed = switch (operator) {
            case LESS_THAN -> comparison < 0;
            case LESS_THAN_OR_EQUAL -> comparison <= 0;
            case GREATER_THAN -> comparison > 0;
            case GREATER_THAN_OR_EQUAL -> comparison >= 0;
            default -> false;
        };
        return passed
                ? Outcome.pass()
                : Outcome.fail("Numeric comparison failed");
    }

    private static Double toNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Human-readable operator wording used in report messages. */
    public static String describe(AssertionOperator operator) {
        if (operator == null) {
            return "";
        }
        return operator.name().toLowerCase().replace('_', ' ');
    }
}
