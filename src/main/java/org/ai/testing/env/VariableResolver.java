package org.ai.testing.env;

import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.HeaderDto;
import org.ai.testing.dto.common.PathParamDto;
import org.ai.testing.dto.common.QueryParamDto;
import org.ai.testing.dto.common.RequestBodyDto;
import org.ai.testing.testcase.dto.TestCaseDto;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableResolver {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");

    public String resolve(String template, VariableStore store) {
        if (template == null || store == null) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = store.get(key);
            matcher.appendReplacement(
                    resolved,
                    Matcher.quoteReplacement(value != null ? value : matcher.group(0)));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    public Map<String, String> resolveMap(Map<String, String> source, VariableStore store) {
        if (source == null) {
            return new HashMap<>();
        }
        Map<String, String> resolved = new HashMap<>();
        source.forEach((key, value) ->
                resolved.put(resolve(key, store), resolve(value, store)));
        return resolved;
    }

    public void resolveTestCase(TestCaseDto testCase, VariableStore store) {
        if (testCase == null || store == null) {
            return;
        }
        resolveRequest(testCase.getRequest(), store);
        resolveAuth(testCase.getAuth(), store);
    }

    public void resolveRequest(BaseRequestDto request, VariableStore store) {
        if (request == null) {
            return;
        }
        request.setUrl(resolve(request.getUrl(), store));
        request.setHeaders(resolveMap(request.getHeaders(), store));
        request.setQueryParams(resolveMap(request.getQueryParams(), store));
        request.setPathParams(resolveMap(request.getPathParams(), store));
        if (request.getHeaderItems() != null) {
            for (HeaderDto header : request.getHeaderItems()) {
                header.setName(resolve(header.getName(), store));
                header.setValue(resolve(header.getValue(), store));
            }
        }
        if (request.getQueryParamItems() != null) {
            for (QueryParamDto param : request.getQueryParamItems()) {
                param.setName(resolve(param.getName(), store));
                param.setValue(resolve(param.getValue(), store));
            }
        }
        if (request.getPathParamItems() != null) {
            for (PathParamDto param : request.getPathParamItems()) {
                param.setName(resolve(param.getName(), store));
                param.setValue(resolve(param.getValue(), store));
            }
        }
        RequestBodyDto body = request.getBody();
        if (body != null) {
            body.setContentType(resolve(body.getContentType(), store));
            body.setRawBody(resolve(body.getRawBody(), store));
            if (body.getFormFields() != null) {
                for (QueryParamDto field : body.getFormFields()) {
                    field.setName(resolve(field.getName(), store));
                    field.setValue(resolve(field.getValue(), store));
                }
            }
        }
        resolveAuth(request.getAuth(), store);
    }

    public void resolveAuth(AuthDto auth, VariableStore store) {
        if (auth == null) {
            return;
        }
        auth.setToken(resolve(auth.getToken(), store));
        auth.setUsername(resolve(auth.getUsername(), store));
        auth.setPassword(resolve(auth.getPassword(), store));
        auth.setApiKeyName(resolve(auth.getApiKeyName(), store));
        auth.setApiKeyValue(resolve(auth.getApiKeyValue(), store));
    }
}
