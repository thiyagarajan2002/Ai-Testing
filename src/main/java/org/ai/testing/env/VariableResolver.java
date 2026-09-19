package org.ai.testing.env;

import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.HeaderDto;
import org.ai.testing.dto.common.PathParamDto;
import org.ai.testing.dto.common.QueryParamDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.testcase.dto.TestCaseDto;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes <code>{{name}}</code> placeholders throughout a request.
 *
 * <p>Three behaviours beyond simple replacement:</p>
 * <ul>
 *   <li>values may themselves contain placeholders, resolved up to a small
 *       depth so a cycle cannot hang the run;</li>
 *   <li>unknown names are left verbatim rather than blanked, which makes a
 *       missing environment obvious in the report instead of producing a URL
 *       with a silent hole in it;</li>
 *   <li>Postman-style dynamic variables such as <code>{{$guid}}</code> are
 *       generated on the fly.</li>
 * </ul>
 */
public class VariableResolver {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");

    private static final int MAX_DEPTH = 5;

    /** Resolves placeholders in a single string. */
    public String resolve(String template, VariableStore store) {
        return resolve(template, store, MAX_DEPTH);
    }

    private String resolve(String template, VariableStore store, int depth) {
        if (template == null || store == null || depth <= 0
                || template.indexOf('{') < 0) {
            return template;
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder resolved = new StringBuilder();
        boolean changed = false;

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = key.startsWith("$") ? dynamic(key) : store.get(key);
            if (value == null) {
                // Leave the placeholder in place so the gap is visible.
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                changed = true;
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(resolved);

        String output = resolved.toString();
        return changed ? resolve(output, store, depth - 1) : output;
    }

    /** Postman-compatible generated values. */
    private String dynamic(String key) {
        return switch (key) {
            case "$guid", "$uuid" -> UUID.randomUUID().toString();
            case "$timestamp" -> String.valueOf(Instant.now().getEpochSecond());
            case "$epochMillis" -> String.valueOf(System.currentTimeMillis());
            case "$isoTimestamp" -> DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            case "$randomInt" -> String.valueOf(ThreadLocalRandom.current().nextInt(0, 1001));
            case "$randomUUID" -> UUID.randomUUID().toString();
            case "$randomAlphaNumeric" -> randomToken(12);
            default -> null;
        };
    }

    private String randomToken(int length) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder token = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            token.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }
        return token.toString();
    }

    /** Resolves both keys and values, preserving insertion order. */
    public Map<String, String> resolveMap(Map<String, String> source, VariableStore store) {
        Map<String, String> resolved = new LinkedHashMap<>();
        if (source == null) {
            return resolved;
        }
        source.forEach((key, value) -> resolved.put(resolve(key, store), resolve(value, store)));
        return resolved;
    }

    /** Resolves the request and the case-level auth in place. */
    public void resolveTestCase(TestCaseDto testCase, VariableStore store) {
        if (testCase == null || store == null) {
            return;
        }
        resolveRequest(testCase.getRequest(), store);
        resolveAuth(testCase.getAuth(), store);
    }

    /** Resolves every addressable field of a request in place. */
    public void resolveRequest(BaseRequestDto request, VariableStore store) {
        if (request == null || store == null) {
            return;
        }

        request.setUrl(resolve(request.getUrl(), store));
        request.setHeaders(resolveMap(request.getHeaders(), store));
        request.setQueryParams(resolveMap(request.getQueryParams(), store));
        request.setPathParams(resolveMap(request.getPathParams(), store));

        for (HeaderDto header : request.getHeaderItems()) {
            if (header != null) {
                header.setName(resolve(header.getName(), store));
                header.setValue(resolve(header.getValue(), store));
            }
        }
        for (QueryParamDto param : request.getQueryParamItems()) {
            if (param != null) {
                param.setName(resolve(param.getName(), store));
                param.setValue(resolve(param.getValue(), store));
            }
        }
        for (PathParamDto param : request.getPathParamItems()) {
            if (param != null) {
                param.setName(resolve(param.getName(), store));
                param.setValue(resolve(param.getValue(), store));
            }
        }

        RequestBodyDto body = request.getBody();
        if (body != null) {
            body.setContentType(resolve(body.getContentType(), store));
            body.setRawBody(resolve(body.getRawBody(), store));
            for (QueryParamDto field : body.getFormFields()) {
                if (field != null) {
                    field.setName(resolve(field.getName(), store));
                    field.setValue(resolve(field.getValue(), store));
                }
            }
        }

        resolveAuth(request.getAuth(), store);
    }

    /** Resolves credential fields in place. */
    public void resolveAuth(AuthDto auth, VariableStore store) {
        if (auth == null || store == null) {
            return;
        }
        auth.setToken(resolve(auth.getToken(), store));
        auth.setUsername(resolve(auth.getUsername(), store));
        auth.setPassword(resolve(auth.getPassword(), store));
        auth.setApiKeyName(resolve(auth.getApiKeyName(), store));
        auth.setApiKeyValue(resolve(auth.getApiKeyValue(), store));
    }

    /** Names still unresolved after substitution, for reporting gaps. */
    public java.util.List<String> unresolvedNames(String text) {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (text == null) {
            return names;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1).trim());
        }
        return names;
    }
}
