package org.ai.testing.dto.common;

/** Credentials for one scope: a run (collection), a suite (folder) or a case. */
public class AuthDto {

    private AuthType type = AuthType.INHERIT;
    private String token;
    private String username;
    private String password;
    private String apiKeyName;
    private String apiKeyValue;

    /** {@code HEADER} or {@code QUERY} — mirrors Postman and Bruno placement. */
    private String apiKeyIn = "HEADER";

    public static AuthDto bearer(String token) {
        AuthDto auth = new AuthDto();
        auth.type = AuthType.BEARER;
        auth.token = token;
        return auth;
    }

    public static AuthDto basic(String username, String password) {
        AuthDto auth = new AuthDto();
        auth.type = AuthType.BASIC;
        auth.username = username;
        auth.password = password;
        return auth;
    }

    public static AuthDto apiKey(String name, String value, String placement) {
        AuthDto auth = new AuthDto();
        auth.type = AuthType.API_KEY;
        auth.apiKeyName = name;
        auth.apiKeyValue = value;
        auth.apiKeyIn = placement == null ? "HEADER" : placement;
        return auth;
    }

    public AuthType getType() {
        return type;
    }

    public void setType(AuthType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public void setApiKeyName(String apiKeyName) {
        this.apiKeyName = apiKeyName;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    public String getApiKeyIn() {
        return apiKeyIn;
    }

    public void setApiKeyIn(String apiKeyIn) {
        this.apiKeyIn = apiKeyIn;
    }

    public AuthDto copy() {
        AuthDto copy = new AuthDto();
        copy.type = type;
        copy.token = token;
        copy.username = username;
        copy.password = password;
        copy.apiKeyName = apiKeyName;
        copy.apiKeyValue = apiKeyValue;
        copy.apiKeyIn = apiKeyIn;
        return copy;
    }

    @Override
    public String toString() {
        return "AuthDto{type=" + type + "}";
    }
}
