package org.ai.testing.validation;


import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.dto.ValidationSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationEngineTest {

    private ValidationEngine validationEngine;
    private ResponseDto response;

    @BeforeEach
    void setUp() {

        validationEngine =
                new ValidationEngine();

        response =
                new ResponseDto();

        response.setStatusCode(200);

        response.setBody(
                "{\"userId\":1,\"id\":1,\"title\":\"Test Post\"}"
        );

        response.getHeaders().put(
                "Content-Type",
                "application/json"
        );

        response.getHeaders().put(
                "X-Test",
                "automation"
        );
    }

    // =============================================
    // STATUS CODE
    // =============================================

    @Test
    void shouldValidateStatusCodeSuccessfully() {

        ValidationSummaryDto result =
                validationEngine.validateStatusCode(
                        response,
                        200
                );

        assertTrue(result.isPassed());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPassedCount());
        assertEquals(0, result.getFailedCount());
    }

    @Test
    void shouldFailWhenStatusCodeDoesNotMatch() {

        ValidationSummaryDto result =
                validationEngine.validateStatusCode(
                        response,
                        404
                );

        assertFalse(result.isPassed());
        assertEquals(1, result.getTotal());
        assertEquals(0, result.getPassedCount());
        assertEquals(1, result.getFailedCount());
    }

    // =============================================
    // RESPONSE BODY - EQUALS
    // =============================================

    @Test
    void shouldValidateBodyEquals() {

        String body =
                response.getBody();

        ValidationSummaryDto result =
                validationEngine.validateBody(
                        response,
                        AssertionOperator.EQUALS,
                        body
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // RESPONSE BODY - NOT_EQUALS
    // =============================================

    @Test
    void shouldValidateBodyNotEquals() {

        ValidationSummaryDto result =
                validationEngine.validateBody(
                        response,
                        AssertionOperator.NOT_EQUALS,
                        "invalid body"
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // RESPONSE BODY - CONTAINS
    // =============================================

    @Test
    void shouldValidateBodyContains() {

        ValidationSummaryDto result =
                validationEngine.validateBody(
                        response,
                        AssertionOperator.CONTAINS,
                        "userId"
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // RESPONSE BODY - NOT_CONTAINS
    // =============================================

    @Test
    void shouldValidateBodyNotContains() {

        ValidationSummaryDto result =
                validationEngine.validateBody(
                        response,
                        AssertionOperator.NOT_CONTAINS,
                        "password"
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // RESPONSE BODY - EMPTY
    // =============================================

    @Test
    void shouldFailBodyEmptyValidationForNonEmptyBody() {

        ValidationSummaryDto result =
                validationEngine.validateBody(
                        response,
                        AssertionOperator.EMPTY,
                        null
                );

        assertFalse(result.isPassed());
    }

    // =============================================
    // RESPONSE BODY - NOT_EMPTY
    // =============================================

    @Test
    void shouldValidateBodyNotEmpty() {

        ValidationSummaryDto result =
                validationEngine.validateBody(
                        response,
                        AssertionOperator.NOT_EMPTY,
                        null
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // HEADER - EXISTS
    // =============================================

    @Test
    void shouldValidateHeaderExists() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "Content-Type",
                        AssertionOperator.EXISTS,
                        null
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // HEADER - NOT_EXISTS
    // =============================================

    @Test
    void shouldValidateHeaderNotExists() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "Authorization",
                        AssertionOperator.NOT_EXISTS,
                        null
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // HEADER - EQUALS
    // =============================================

    @Test
    void shouldValidateHeaderEquals() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "Content-Type",
                        AssertionOperator.EQUALS,
                        "application/json"
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // HEADER - NOT_EQUALS
    // =============================================

    @Test
    void shouldValidateHeaderNotEquals() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "Content-Type",
                        AssertionOperator.NOT_EQUALS,
                        "text/plain"
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // HEADER - CONTAINS
    // =============================================

    @Test
    void shouldValidateHeaderContains() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "Content-Type",
                        AssertionOperator.CONTAINS,
                        "json"
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // HEADER - NOT_CONTAINS
    // =============================================

    @Test
    void shouldValidateHeaderNotContains() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "Content-Type",
                        AssertionOperator.NOT_CONTAINS,
                        "xml"
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // HEADER - EMPTY
    // =============================================

    @Test
    void shouldFailHeaderEmptyForNonEmptyHeader() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "Content-Type",
                        AssertionOperator.EMPTY,
                        null
                );

        assertFalse(result.isPassed());
    }

    // =============================================
    // HEADER - NOT_EMPTY
    // =============================================

    @Test
    void shouldValidateHeaderNotEmpty() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "Content-Type",
                        AssertionOperator.NOT_EMPTY,
                        null
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // CASE INSENSITIVE HEADER
    // =============================================

    @Test
    void shouldFindHeaderCaseInsensitively() {

        ValidationSummaryDto result =
                validationEngine.validateHeader(
                        response,
                        "content-type",
                        AssertionOperator.EQUALS,
                        "application/json"
                );

        assertTrue(result.isPassed());
    }

    // =============================================
    // FAILURE RESULT DETAILS
    // =============================================

    @Test
    void shouldReturnFailureDetails() {

        ValidationSummaryDto result =
                validationEngine.validateStatusCode(
                        response,
                        500
                );

        assertFalse(result.isPassed());

        assertEquals(
                "STATUS_CODE",
                result.getResults()
                        .get(0)
                        .getValidationType()
        );

        assertEquals(
                "500",
                result.getResults()
                        .get(0)
                        .getExpected()
        );

        assertEquals(
                "200",
                result.getResults()
                        .get(0)
                        .getActual()
        );
    }
}