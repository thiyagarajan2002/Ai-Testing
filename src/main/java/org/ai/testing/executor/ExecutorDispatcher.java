package org.ai.testing.executor;


import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.dto.delete.DeleteRequestDto;
import org.ai.testing.dto.get.GetRequestDto;
import org.ai.testing.dto.patch.PatchRequestDto;
import org.ai.testing.dto.post.PostRequestDto;
import org.ai.testing.dto.put.PutRequestDto;

public class ExecutorDispatcher {

    private final GetExecutor getExecutor;
    private final PostExecutor postExecutor;
    private final PutExecutor putExecutor;
    private final PatchExecutor patchExecutor;
    private final DeleteExecutor deleteExecutor;

    public ExecutorDispatcher() {
        this.getExecutor = new GetExecutor();
        this.postExecutor = new PostExecutor();
        this.putExecutor = new PutExecutor();
        this.patchExecutor = new PatchExecutor();
        this.deleteExecutor = new DeleteExecutor();
    }

    public ResponseDto execute(
            String method,
            BaseRequestDto request) {

        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException(
                    "HTTP method cannot be null or empty"
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        return switch (method.toUpperCase()) {

            case "GET" -> getExecutor.execute(
                    convertToGetRequest(request)
            );

            case "POST" -> postExecutor.execute(
                    convertToPostRequest(request)
            );

            case "PUT" -> putExecutor.execute(
                    convertToPutRequest(request)
            );

            case "PATCH" -> patchExecutor.execute(
                    convertToPatchRequest(request)
            );

            case "DELETE" -> deleteExecutor.execute(
                    convertToDeleteRequest(request)
            );

            default -> throw new IllegalArgumentException(
                    "Unsupported HTTP method: " + method
            );
        };
    }

    private GetRequestDto convertToGetRequest(
            BaseRequestDto source) {

        GetRequestDto request =
                new GetRequestDto();

        copyFields(source, request);

        return request;
    }

    private PostRequestDto convertToPostRequest(
            BaseRequestDto source) {

        PostRequestDto request =
                new PostRequestDto();

        copyFields(source, request);

        return request;
    }

    private PutRequestDto convertToPutRequest(
            BaseRequestDto source) {

        PutRequestDto request =
                new PutRequestDto();

        copyFields(source, request);

        return request;
    }

    private PatchRequestDto convertToPatchRequest(
            BaseRequestDto source) {

        PatchRequestDto request =
                new PatchRequestDto();

        copyFields(source, request);

        return request;
    }

    private DeleteRequestDto convertToDeleteRequest(
            BaseRequestDto source) {

        DeleteRequestDto request =
                new DeleteRequestDto();

        copyFields(source, request);

        return request;
    }

    private void copyFields(
            BaseRequestDto source,
            BaseRequestDto target) {

        target.setUrl(source.getUrl());

        target.setHeaders(
                source.getHeaders()
        );

        target.setQueryParams(
                source.getQueryParams()
        );

        target.setPathParams(
                source.getPathParams()
        );

        target.setBody(
                source.getBody()
        );
    }
}