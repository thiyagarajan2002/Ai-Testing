package org.ai.testing.auth;

import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.AuthType;
import org.ai.testing.dto.common.BaseRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Authentication")
class AuthApplicatorTest {

    private final AuthApplicator applicator = new AuthApplicator();

    @Test
    @DisplayName("prefers request credentials over suite and run")
    void prefersMostSpecificScope() {
        AuthDto chosen = applicator.resolve(
                AuthDto.bearer("request"), AuthDto.bearer("suite"), AuthDto.bearer("run"));
        assertEquals("request", chosen.getToken());
    }

    @Test
    @DisplayName("falls through INHERIT to the enclosing scope")
    void inheritFallsThrough() {
        AuthDto inherit = new AuthDto();
        inherit.setType(AuthType.INHERIT);
        AuthDto chosen = applicator.resolve(inherit, null, AuthDto.bearer("run"));
        assertEquals("run", chosen.getToken());
    }

    @Test
    @DisplayName("NONE stops inheritance so a public endpoint stays unauthenticated")
    void noneStopsInheritance() {
        AuthDto none = new AuthDto();
        none.setType(AuthType.NONE);
        AuthDto chosen = applicator.resolve(none, null, AuthDto.bearer("run"));
        assertEquals(AuthType.NONE, chosen.getType());

        BaseRequestDto request = new BaseRequestDto();
        applicator.apply(request, chosen);
        assertFalse(request.getHeaders().containsKey("Authorization"));
    }

    @Test
    @DisplayName("returns null when nothing is configured anywhere")
    void returnsNullWhenUnset() {
        assertNull(applicator.resolve(null, null, null));
    }

    @Test
    @DisplayName("writes a bearer header")
    void writesBearerHeader() {
        BaseRequestDto request = new BaseRequestDto();
        assertEquals("bearer", applicator.apply(request, AuthDto.bearer("t0ken")));
        assertEquals("Bearer t0ken", request.getHeaders().get("Authorization"));
    }

    @Test
    @DisplayName("base64-encodes basic credentials")
    void writesBasicHeader() {
        BaseRequestDto request = new BaseRequestDto();
        applicator.apply(request, AuthDto.basic("user", "pass"));
        assertEquals("Basic dXNlcjpwYXNz", request.getHeaders().get("Authorization"));
    }

    @Test
    @DisplayName("places an API key in a header or the query string")
    void placesApiKey() {
        BaseRequestDto headerRequest = new BaseRequestDto();
        applicator.apply(headerRequest, AuthDto.apiKey("X-Api-Key", "k", "HEADER"));
        assertEquals("k", headerRequest.getHeaders().get("X-Api-Key"));

        BaseRequestDto queryRequest = new BaseRequestDto();
        applicator.apply(queryRequest, AuthDto.apiKey("api_key", "k", "QUERY"));
        assertEquals("k", queryRequest.getQueryParams().get("api_key"));
    }

    @Test
    @DisplayName("reports incomplete credentials instead of sending a blank header")
    void reportsIncompleteCredentials() {
        BaseRequestDto request = new BaseRequestDto();
        assertEquals("bearer (no token)", applicator.apply(request, AuthDto.bearer("")));
        assertFalse(request.getHeaders().containsKey("Authorization"));
    }
}
