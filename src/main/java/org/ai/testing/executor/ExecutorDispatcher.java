package org.ai.testing.executor;

import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.dto.delete.DeleteRequestDto;
import org.ai.testing.dto.get.GetRequestDto;
import org.ai.testing.dto.patch.PatchRequestDto;
import org.ai.testing.dto.post.PostRequestDto;
import org.ai.testing.dto.put.PutRequestDto;
import org.ai.testing.executor.common.ExecutionOptions;
import org.ai.testing.util.Strings;

import java.util.List;
import java.util.Locale;

/**
 * Routes a request to the executor for its verb.
 *
 * <p>All five executors share one {@link ExecutionOptions} and therefore one
 * connection pool, and the dispatcher itself is stateless after construction so
 * a single instance can be reused by parallel suites.</p>
 */
public class ExecutorDispatcher {

    private static final List<String> SUPPORTED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final GetExecutor getExecutor;
    private final PostExecutor postExecutor;
    private final PutExecutor putExecutor;
    private final PatchExecutor patchExecutor;
    private final DeleteExecutor deleteExecutor;

    public ExecutorDispatcher() {
        this(ExecutionOptions.defaults());
    }

    public ExecutorDispatcher(ExecutionOptions options) {
        ExecutionOptions effective = options == null ? ExecutionOptions.defaults() : options;
        this.getExecutor = new GetExecutor(effective);
        this.postExecutor = new PostExecutor(effective);
        this.putExecutor = new PutExecutor(effective);
        this.patchExecutor = new PatchExecutor(effective);
        this.deleteExecutor = new DeleteExecutor(effective);
    }

    public static List<String> supportedMethods() {
        return SUPPORTED_METHODS;
    }

    public static boolean supports(String method) {
        return method != null
                && SUPPORTED_METHODS.contains(method.trim().toUpperCase(Locale.ROOT));
    }

    public ResponseDto execute(String method, BaseRequestDto request) {

        if (Strings.isBlank(method)) {
            throw new IllegalArgumentException("HTTP method cannot be null or empty");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        return switch (method.trim().toUpperCase(Locale.ROOT)) {
            case "GET" -> getExecutor.execute(GetRequestDto.from(request));
            case "POST" -> postExecutor.execute(PostRequestDto.from(request));
            case "PUT" -> putExecutor.execute(PutRequestDto.from(request));
            case "PATCH" -> patchExecutor.execute(PatchRequestDto.from(request));
            case "DELETE" -> deleteExecutor.execute(DeleteRequestDto.from(request));
            default -> throw new IllegalArgumentException(
                    "Unsupported HTTP method: " + method
                            + ". Supported methods: " + String.join(", ", SUPPORTED_METHODS));
        };
    }
}
