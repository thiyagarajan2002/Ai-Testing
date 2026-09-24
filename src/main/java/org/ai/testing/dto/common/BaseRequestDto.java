package org.ai.testing.dto.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The common request model shared by every HTTP method.
 *
 * <p>Two representations of headers and parameters coexist on purpose:</p>
 * <ul>
 *   <li>the {@code *Items} lists keep the ordering and the enabled flag exactly
 *       as they appear in an imported Postman or Bruno collection;</li>
 *   <li>the flat maps are the effective values actually sent, produced by
 *       {@code RequestNormalizer} after disabled entries are dropped.</li>
 * </ul>
 *
 * <p>Hand-written test cases can populate only the maps and ignore the lists.</p>
 */
public class BaseRequestDto {

    private String url;

    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> queryParams = new LinkedHashMap<>();
    private Map<String, String> pathParams = new LinkedHashMap<>();

    private List<HeaderDto> headerItems = new ArrayList<>();
    private List<QueryParamDto> queryParamItems = new ArrayList<>();
    private List<PathParamDto> pathParamItems = new ArrayList<>();

    private RequestBodyDto body;
    private AuthDto auth;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<>() : headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams == null ? new LinkedHashMap<>() : queryParams;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public void setPathParams(Map<String, String> pathParams) {
        this.pathParams = pathParams == null ? new LinkedHashMap<>() : pathParams;
    }

    public List<HeaderDto> getHeaderItems() {
        return headerItems;
    }

    public void setHeaderItems(List<HeaderDto> headerItems) {
        this.headerItems = headerItems == null ? new ArrayList<>() : headerItems;
    }

    public List<QueryParamDto> getQueryParamItems() {
        return queryParamItems;
    }

    public void setQueryParamItems(List<QueryParamDto> queryParamItems) {
        this.queryParamItems = queryParamItems == null ? new ArrayList<>() : queryParamItems;
    }

    public List<PathParamDto> getPathParamItems() {
        return pathParamItems;
    }

    public void setPathParamItems(List<PathParamDto> pathParamItems) {
        this.pathParamItems = pathParamItems == null ? new ArrayList<>() : pathParamItems;
    }

    public RequestBodyDto getBody() {
        return body;
    }

    public void setBody(RequestBodyDto body) {
        this.body = body;
    }

    public AuthDto getAuth() {
        return auth;
    }

    public void setAuth(AuthDto auth) {
        this.auth = auth;
    }

    // ------------------------------------------------------------------
    // Fluent helpers for hand-written test cases
    // ------------------------------------------------------------------

    public BaseRequestDto header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public BaseRequestDto query(String name, String value) {
        queryParams.put(name, value);
        return this;
    }

    public BaseRequestDto pathParam(String name, String value) {
        pathParams.put(name, value);
        return this;
    }

    public BaseRequestDto jsonBody(String rawBody) {
        this.body = RequestBodyDto.json(rawBody);
        return this;
    }

    /** Deep copy, used so reports show the request exactly as it was sent. */
    public void copyInto(BaseRequestDto target) {
        target.setUrl(url);
        target.setHeaders(new LinkedHashMap<>(headers));
        target.setQueryParams(new LinkedHashMap<>(queryParams));
        target.setPathParams(new LinkedHashMap<>(pathParams));

        List<HeaderDto> copiedHeaders = new ArrayList<>();
        for (HeaderDto item : headerItems) {
            if (item != null) {
                copiedHeaders.add(item.copy());
            }
        }
        target.setHeaderItems(copiedHeaders);

        List<QueryParamDto> copiedQuery = new ArrayList<>();
        for (QueryParamDto item : queryParamItems) {
            if (item != null) {
                copiedQuery.add(item.copy());
            }
        }
        target.setQueryParamItems(copiedQuery);

        List<PathParamDto> copiedPath = new ArrayList<>();
        for (PathParamDto item : pathParamItems) {
            if (item != null) {
                copiedPath.add(item.copy());
            }
        }
        target.setPathParamItems(copiedPath);

        target.setBody(body == null ? null : body.copy());
        target.setAuth(auth == null ? null : auth.copy());
    }

    public BaseRequestDto copy() {
        BaseRequestDto copy = new BaseRequestDto();
        copyInto(copy);
        return copy;
    }

    @Override
    public String toString() {
        return url;
    }
}
