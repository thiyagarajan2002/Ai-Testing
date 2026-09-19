package org.ai.testing.validation;

import org.ai.testing.TestFixtures;
import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationResultDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Validation engine")
class ValidationEngineTest {

    private final ValidationEngine engine = new ValidationEngine();

    private ResponseDto response() {
        return TestFixtures.response(200,
                "{\"id\":7,\"name\":\"Rex\",\"tags\":[\"a\",\"b\"],\"active\":true}");
    }

    @Test
    @DisplayName("compares the expected status code")
    void validatesStatusCode() {
        assertTrue(engine.validateStatusCode(response(), 200).isPassed());
        ValidationResultDto failure = engine.validateStatusCode(response(), 404);
        assertFalse(failure.isPassed());
        assertEquals("200", failure.getActual());
        assertTrue(failure.getMessage().contains("404"));
    }

    @Test
    @DisplayName("evaluates JSONPath assertions, which the old executor rejected")
    void evaluatesJsonPath() {
        assertTrue(engine.evaluate(response(),
                AssertionDto.jsonPath("$.name", AssertionOperator.EQUALS, "Rex")).isPassed());
        assertTrue(engine.evaluate(response(),
                AssertionDto.jsonPath("$.tags.length()",
                        AssertionOperator.EQUALS, "2")).isPassed());
        assertFalse(engine.evaluate(response(),
                AssertionDto.jsonPath("$.missing", AssertionOperator.EXISTS, "")).isPassed());
        assertTrue(engine.evaluate(response(),
                AssertionDto.jsonPath("$.missing", AssertionOperator.NOT_EXISTS, "")).isPassed());
    }

    @Test
    @DisplayName("evaluates response time assertions, which the old executor rejected")
    void evaluatesResponseTime() {
        assertTrue(engine.evaluate(response(),
                AssertionDto.responseTimeBelow(1000)).isPassed());
        assertFalse(engine.evaluate(response(),
                AssertionDto.responseTimeBelow(1)).isPassed());
    }

    @Test
    @DisplayName("matches headers without regard to case")
    void evaluatesHeadersCaseInsensitively() {
        assertTrue(engine.evaluate(response(),
                AssertionDto.header("content-type",
                        AssertionOperator.CONTAINS, "json")).isPassed());
        assertTrue(engine.evaluate(response(),
                AssertionDto.header("X-Missing", AssertionOperator.NOT_EXISTS, "")).isPassed());
    }

    @Test
    @DisplayName("supports the CONTENT_TYPE and RESPONSE_SIZE shorthands")
    void evaluatesShorthandTypes() {
        assertTrue(engine.evaluate(response(), new AssertionDto(AssertionType.CONTENT_TYPE,
                "Content-Type", AssertionOperator.CONTAINS, "application")).isPassed());
        assertTrue(engine.evaluate(response(), new AssertionDto(AssertionType.RESPONSE_SIZE,
                "bodySizeBytes", AssertionOperator.GREATER_THAN, "5")).isPassed());
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @DisplayName("applies every operator consistently")
    @CsvSource({
            "EQUALS,Rex,true",
            "NOT_EQUALS,Rex,false",
            "CONTAINS,Re,true",
            "NOT_CONTAINS,zz,true",
            "STARTS_WITH,Re,true",
            "ENDS_WITH,ex,true",
            "MATCHES,'R.x',true",
            "NOT_EMPTY,,true",
            "EMPTY,,false",
            "EXISTS,,true",
            "IN,'Rex,Milo',true",
            "NOT_IN,'Milo,Bella',true"
    })
    void appliesOperators(String operator, String expected, boolean shouldPass) {
        AssertionDto assertion = AssertionDto.jsonPath("$.name",
                AssertionOperator.valueOf(operator), expected);
        assertEquals(shouldPass, engine.evaluate(response(), assertion).isPassed());
    }

    @Test
    @DisplayName("fails cleanly rather than throwing on a null operator or type")
    void handlesIncompleteAssertions() {
        AssertionDto noOperator = new AssertionDto(AssertionType.RESPONSE_BODY, "body", null, "x");
        ValidationResultDto result = engine.evaluate(response(), noOperator);
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("operator"));

        AssertionDto noType = new AssertionDto(null, "body", AssertionOperator.EQUALS, "x");
        assertFalse(engine.evaluate(response(), noType).isPassed());
    }

    @Test
    @DisplayName("fails a numeric operator when the value is not a number")
    void handlesNonNumericComparison() {
        ValidationResultDto result = engine.evaluate(response(),
                AssertionDto.jsonPath("$.name", AssertionOperator.LESS_THAN, "10"));
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("not numeric"));
    }

    @Test
    @DisplayName("skips disabled assertions")
    void skipsDisabledAssertions() {
        AssertionDto disabled = AssertionDto.body(AssertionOperator.EQUALS, "never matches");
        disabled.setEnabled(false);
        ValidationSummaryDto summary =
                engine.validate(response(), 200, List.of(disabled));
        assertEquals(1, summary.getTotal());
        assertTrue(summary.isPassed());
    }

    @Test
    @DisplayName("does not double-count the expected status code")
    void avoidsDuplicateStatusAssertions() {
        ValidationSummaryDto summary = engine.validate(response(), 200,
                List.of(AssertionDto.status(200)));
        assertEquals(1, summary.getTotal());
    }

    @Test
    @DisplayName("keeps summary counters in step with the results")
    void tracksCounters() {
        ValidationSummaryDto summary = engine.validate(response(), 200, List.of(
                AssertionDto.body(AssertionOperator.NOT_EMPTY, ""),
                AssertionDto.body(AssertionOperator.EQUALS, "wrong")));
        assertEquals(3, summary.getTotal());
        assertEquals(2, summary.getPassedCount());
        assertEquals(1, summary.getFailedCount());
        assertFalse(summary.isPassed());
        assertEquals("RESPONSE_BODY", summary.firstFailure().getValidationType());
    }
}
