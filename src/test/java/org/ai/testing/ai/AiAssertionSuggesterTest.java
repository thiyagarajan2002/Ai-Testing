package org.ai.testing.ai;

import org.ai.testing.ai.model.AiAssertionSuggestionResult;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiAssertionSuggesterTest {

    @Test
    void shouldSuggestStatusBodyAndHeaderAssertions() {
        ResponseDto response = new ResponseDto();
        response.setStatusCode(200);
        response.setBody("{\"id\":1}");
        response.setHeaders(Map.of("Content-Type", "application/json; charset=utf-8"));

        AiAssertionSuggestionResult result =
                new AiAssertionSuggester().suggest(response, 200);

        assertTrue(result.getSuggestions().stream().anyMatch(s ->
                s.getAssertion().getType() == AssertionType.STATUS_CODE
                        && s.getAssertion().getOperator() == AssertionOperator.EQUALS
                        && "200".equals(s.getAssertion().getExpectedValue())));

        assertTrue(result.getSuggestions().stream().anyMatch(s ->
                s.getAssertion().getType() == AssertionType.RESPONSE_BODY
                        && s.getAssertion().getOperator() == AssertionOperator.NOT_EMPTY));

        assertTrue(result.getSuggestions().stream().anyMatch(s ->
                s.getAssertion().getType() == AssertionType.HEADER
                        && "application/json".equals(s.getAssertion().getExpectedValue())));
    }

    @Test
    void shouldSuggestExpectedStatusWhenResponseStatusDiffers() {
        ResponseDto response = new ResponseDto();
        response.setStatusCode(404);

        AiAssertionSuggestionResult result =
                new AiAssertionSuggester().suggest(response, 200);

        assertEquals("200", result.getSuggestions().get(0).getAssertion().getExpectedValue());
    }

    @Test
    void shouldRejectMissingResponse() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiAssertionSuggester().suggest(null, 200));
    }
}
