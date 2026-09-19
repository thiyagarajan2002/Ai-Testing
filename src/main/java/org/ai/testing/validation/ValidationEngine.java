package org.ai.testing.validation;

import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.json.Json;
import org.ai.testing.json.JsonPathReader;
import org.ai.testing.json.JsonValue;
import org.ai.testing.validation.dto.ValidationResultDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;

import java.util.List;

/**
 * Evaluates assertions against a captured response.
 *
 * <p>Every {@link AssertionType} is routed here, including {@code JSON_PATH} and
 * {@code RESPONSE_TIME}, which the earlier executor rejected as "unsupported"
 * even though the enum and the standalone validators already existed.</p>
 */
public class ValidationEngine {

    /** Runs the implicit expected status code plus every enabled assertion. */
    public ValidationSummaryDto validate(ResponseDto response,
                                         Integer expectedStatusCode,
                                         List<AssertionDto> assertions) {

        ValidationSummaryDto summary = new ValidationSummaryDto();

        if (expectedStatusCode != null) {
            summary.add(validateStatusCode(response, expectedStatusCode));
        }

        if (assertions == null) {
            return summary;
        }

        boolean statusAlreadyChecked = expectedStatusCode != null;

        for (AssertionDto assertion : assertions) {
            if (assertion == null || !assertion.isEnabled() || assertion.getType() == null) {
                continue;
            }
            // The explicit expectedStatusCode already covers a plain
            // "status equals N" assertion, so do not report it twice.
            if (statusAlreadyChecked
                    && assertion.getType() == AssertionType.STATUS_CODE
                    && assertion.getOperator() == AssertionOperator.EQUALS
                    && String.valueOf(expectedStatusCode).equals(assertion.getExpectedValue())) {
                continue;
            }
            summary.add(evaluate(response, assertion));
        }
        return summary;
    }

    /** Compares the status code for equality; the most common single check. */
    public ValidationResultDto validateStatusCode(ResponseDto response, int expected) {
        int actual = response == null ? 0 : response.getStatusCode();
        ValidationResultDto result = new ValidationResultDto();
        result.setValidationType(AssertionType.STATUS_CODE.name());
        result.setField("statusCode");
        result.setOperator(AssertionOperator.EQUALS.name());
        result.setExpected(String.valueOf(expected));
        result.setActual(String.valueOf(actual));
        boolean passed = actual == expected;
        result.setPassed(passed);
        result.setMessage(passed
                ? "Status code is " + expected
                : "Expected status " + expected + " but received " + actual);
        return result;
    }

    /** Evaluates a single assertion of any supported type. */
    public ValidationResultDto evaluate(ResponseDto response, AssertionDto assertion) {

        AssertionType type = assertion.getType();
        AssertionOperator operator = assertion.getOperator();

        ValidationResultDto result = new ValidationResultDto();
        result.setValidationType(type == null ? "UNKNOWN" : type.name());
        result.setField(assertion.getField());
        result.setOperator(operator == null ? "" : operator.name());
        result.setExpected(assertion.getExpectedValue());
        result.setDescription(assertion.getDescription());

        if (type == null) {
            result.setPassed(false);
            result.setActual("");
            result.setMessage("Assertion type cannot be null");
            return result;
        }
        if (operator == null) {
            result.setPassed(false);
            result.setActual("");
            result.setMessage("Assertion operator cannot be null");
            return result;
        }

        Target target = resolveTarget(response, type, assertion.getField());
        result.setActual(target.value());
        result.setField(target.label());

        Comparisons.Outcome outcome =
                Comparisons.apply(operator, target.value(),
                        assertion.getExpectedValue(), target.present());

        result.setPassed(outcome.passed());
        result.setMessage(buildMessage(type, target, operator,
                assertion.getExpectedValue(), outcome));
        return result;
    }

    // ------------------------------------------------------------------
    // Target resolution
    // ------------------------------------------------------------------

    /** The concrete value an assertion will be applied to. */
    private record Target(String label, String value, boolean present) {
    }

    private Target resolveTarget(ResponseDto response, AssertionType type, String field) {
        if (response == null) {
            return new Target(field == null ? type.name() : field, "", false);
        }

        return switch (type) {
            case STATUS_CODE -> new Target("statusCode",
                    String.valueOf(response.getStatusCode()), true);

            case RESPONSE_TIME -> new Target("responseTimeMs",
                    String.valueOf(response.getResponseTimeMs()), true);

            case RESPONSE_SIZE -> new Target("bodySizeBytes",
                    String.valueOf(response.getBodySizeBytes()), true);

            case RESPONSE_BODY -> {
                String body = response.getBody();
                yield new Target("body", body == null ? "" : body, body != null);
            }

            case CONTENT_TYPE -> {
                String value = response.header("Content-Type");
                yield new Target("Content-Type", value == null ? "" : value, value != null);
            }

            case HEADER -> {
                String value = response.header(field);
                yield new Target(field == null ? "" : field,
                        value == null ? "" : value, value != null);
            }

            case JSON_PATH -> resolveJsonPath(response, field);
        };
    }

    private Target resolveJsonPath(ResponseDto response, String field) {
        String path = JsonPathReader.normalize(field);
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            return new Target(path, "", false);
        }
        JsonValue root = Json.parseOrMissing(body);
        if (root.isMissing()) {
            return new Target(path, "", false);
        }
        JsonValue match = JsonPathReader.read(root, path);
        if (match.isMissing()) {
            return new Target(path, "", false);
        }
        String rendered = switch (match.kind()) {
            case STRING, NUMBER, BOOLEAN -> match.asText();
            case NULL -> "null";
            default -> Json.write(match, false);
        };
        return new Target(path, rendered, true);
    }

    // ------------------------------------------------------------------
    // Messages
    // ------------------------------------------------------------------

    private String buildMessage(AssertionType type, Target target,
                                AssertionOperator operator, String expected,
                                Comparisons.Outcome outcome) {

        String subject = switch (type) {
            case HEADER -> "Header '" + target.label() + "'";
            case JSON_PATH -> "JSONPath " + target.label();
            case CONTENT_TYPE -> "Content-Type";
            case RESPONSE_BODY -> "Response body";
            case RESPONSE_TIME -> "Response time";
            case RESPONSE_SIZE -> "Response size";
            case STATUS_CODE -> "Status code";
        };

        if (outcome.passed()) {
            if (operator.isUnary()) {
                return subject + " " + Comparisons.describe(operator);
            }
            return subject + " " + Comparisons.describe(operator) + " '"
                    + truncate(expected) + "'";
        }

        StringBuilder message = new StringBuilder(subject);
        message.append(": ").append(outcome.detail());
        if (!operator.isUnary()) {
            message.append(". Expected ").append(Comparisons.describe(operator))
                    .append(" '").append(truncate(expected)).append('\'');
        }
        message.append(", actual '").append(truncate(target.value())).append('\'');
        return message.toString();
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        String single = value.replace("\n", "\\n").replace("\r", "");
        return single.length() <= 180 ? single : single.substring(0, 180) + "…";
    }
}
