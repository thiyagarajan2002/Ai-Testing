package org.ai.testing.auth;

import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.AuthType;
import org.ai.testing.dto.common.BaseRequestDto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

public class AuthApplicator {

    public AuthDto resolve(AuthDto requestAuth, AuthDto suiteAuth, AuthDto collectionAuth) {
        AuthDto chosen = firstConcrete(requestAuth);
        if (chosen != null) {
            return chosen;
        }
        chosen = firstConcrete(suiteAuth);
        if (chosen != null) {
            return chosen;
        }
        return firstConcrete(collectionAuth);
    }

    public void apply(BaseRequestDto request, AuthDto auth) {
        if (request == null || auth == null || auth.getType() == null) {
            return;
        }
        if (request.getHeaders() == null) {
            request.setHeaders(new HashMap<>());
        }
        if (request.getQueryParams() == null) {
            request.setQueryParams(new HashMap<>());
        }

        switch (auth.getType()) {
            case BEARER -> {
                if (auth.getToken() != null && !auth.getToken().isBlank()) {
                    request.getHeaders().put("Authorization", "Bearer " + auth.getToken());
                }
            }
            case BASIC -> {
                String credentials = nullToEmpty(auth.getUsername())
                        + ":"
                        + nullToEmpty(auth.getPassword());
                String encoded = Base64.getEncoder()
                        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                request.getHeaders().put("Authorization", "Basic " + encoded);
            }
            case API_KEY -> {
                if (auth.getApiKeyName() == null || auth.getApiKeyName().isBlank()
                        || auth.getApiKeyValue() == null) {
                    return;
                }
                if ("QUERY".equalsIgnoreCase(auth.getApiKeyIn())) {
                    request.getQueryParams().put(auth.getApiKeyName(), auth.getApiKeyValue());
                } else {
                    request.getHeaders().put(auth.getApiKeyName(), auth.getApiKeyValue());
                }
            }
            default -> {
            }
        }
    }

    private AuthDto firstConcrete(AuthDto auth) {
        if (auth == null || auth.getType() == null) {
            return null;
        }
        if (auth.getType() == AuthType.INHERIT || auth.getType() == AuthType.NONE) {
            return auth.getType() == AuthType.NONE ? auth : null;
        }
        return auth;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
