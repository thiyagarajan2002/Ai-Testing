package org.ai.testing.auth;

import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.AuthType;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.util.Strings;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Resolves credential inheritance and writes the resulting header or parameter.
 *
 * <p>Precedence is request, then suite, then run. {@link AuthType#INHERIT} keeps
 * looking upwards; {@link AuthType#NONE} deliberately stops the search so a
 * public endpoint inside an authenticated collection stays unauthenticated.</p>
 */
public class AuthApplicator {

    /** Picks the effective credentials for one request. */
    public AuthDto resolve(AuthDto requestAuth, AuthDto suiteAuth, AuthDto runAuth) {
        AuthDto chosen = concrete(requestAuth);
        if (chosen != null) {
            return chosen;
        }
        chosen = concrete(suiteAuth);
        if (chosen != null) {
            return chosen;
        }
        return concrete(runAuth);
    }

    /** Applies credentials to the request. Returns a short label for reports. */
    public String apply(BaseRequestDto request, AuthDto auth) {
        if (request == null || auth == null || auth.getType() == null) {
            return "none";
        }

        switch (auth.getType()) {
            case BEARER -> {
                if (Strings.isBlank(auth.getToken())) {
                    return "bearer (no token)";
                }
                request.getHeaders().put("Authorization", "Bearer " + auth.getToken());
                return "bearer";
            }
            case BASIC -> {
                String credentials = Strings.nullToEmpty(auth.getUsername())
                        + ":" + Strings.nullToEmpty(auth.getPassword());
                String encoded = Base64.getEncoder()
                        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                request.getHeaders().put("Authorization", "Basic " + encoded);
                return "basic";
            }
            case API_KEY -> {
                if (Strings.isBlank(auth.getApiKeyName()) || auth.getApiKeyValue() == null) {
                    return "api key (incomplete)";
                }
                if ("QUERY".equalsIgnoreCase(auth.getApiKeyIn())) {
                    request.getQueryParams().put(auth.getApiKeyName(), auth.getApiKeyValue());
                    return "api key (query)";
                }
                request.getHeaders().put(auth.getApiKeyName(), auth.getApiKeyValue());
                return "api key (header)";
            }
            case NONE -> {
                return "none";
            }
            default -> {
                return "inherit";
            }
        }
    }

    private AuthDto concrete(AuthDto auth) {
        if (auth == null || auth.getType() == null || auth.getType() == AuthType.INHERIT) {
            return null;
        }
        return auth;
    }
}
