package org.ai.testing.testcase.factory;


import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.delete.DeleteRequestDto;
import org.ai.testing.dto.get.GetRequestDto;
import org.ai.testing.dto.patch.PatchRequestDto;
import org.ai.testing.dto.post.PostRequestDto;
import org.ai.testing.dto.put.PutRequestDto;
import org.ai.testing.testcase.dto.TestCaseDto;

public class TestCaseRequestFactory {

    public BaseRequestDto createRequest(TestCaseDto testCase) {

        if (testCase == null) {
            throw new IllegalArgumentException(
                    "Test case cannot be null"
            );
        }

        if (testCase.getMethod() == null
                || testCase.getMethod().isBlank()) {

            throw new IllegalArgumentException(
                    "HTTP method cannot be null or empty"
            );
        }

        return switch (testCase.getMethod().toUpperCase()) {

            case "GET" -> createGetRequest(testCase);

            case "POST" -> createPostRequest(testCase);

            case "PUT" -> createPutRequest(testCase);

            case "PATCH" -> createPatchRequest(testCase);

            case "DELETE" -> createDeleteRequest(testCase);

            default -> throw new IllegalArgumentException(
                    "Unsupported HTTP method: "
                            + testCase.getMethod()
            );
        };
    }

    private GetRequestDto createGetRequest(
            TestCaseDto testCase) {

        GetRequestDto request =
                new GetRequestDto();

        copyBaseFields(testCase, request);

        return request;
    }

    private PostRequestDto createPostRequest(
            TestCaseDto testCase) {

        PostRequestDto request =
                new PostRequestDto();

        copyBaseFields(testCase, request);

        return request;
    }

    private PutRequestDto createPutRequest(
            TestCaseDto testCase) {

        PutRequestDto request =
                new PutRequestDto();

        copyBaseFields(testCase, request);

        return request;
    }

    private PatchRequestDto createPatchRequest(
            TestCaseDto testCase) {

        PatchRequestDto request =
                new PatchRequestDto();

        copyBaseFields(testCase, request);

        return request;
    }

    private DeleteRequestDto createDeleteRequest(
            TestCaseDto testCase) {

        DeleteRequestDto request =
                new DeleteRequestDto();

        copyBaseFields(testCase, request);

        return request;
    }

    private void copyBaseFields(
            TestCaseDto testCase,
            BaseRequestDto request) {

        if (testCase.getRequest() == null) {
            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        BaseRequestDto source =
                testCase.getRequest();

        request.setUrl(source.getUrl());

        request.setHeaders(
                source.getHeaders()
        );

        request.setQueryParams(
                source.getQueryParams()
        );

        request.setPathParams(
                source.getPathParams()
        );

        request.setBody(
                source.getBody()
        );
    }
}
