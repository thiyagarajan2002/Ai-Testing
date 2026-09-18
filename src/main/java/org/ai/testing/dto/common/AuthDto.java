package org.ai.testing.dto.common;

import lombok.Data;

@Data
public class AuthDto {

    private AuthType type = AuthType.INHERIT;

    private String token;

    private String username;

    private String password;

    private String apiKeyName;

    private String apiKeyValue;

    /**
     * HEADER or QUERY — matches Postman/Bruno API-key placement.
     */
    private String apiKeyIn = "HEADER";
}
