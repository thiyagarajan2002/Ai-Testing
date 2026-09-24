package org.ai.testing.testcase.executor;

import org.ai.testing.auth.AuthApplicator;
import org.ai.testing.dto.common.AuthDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ResponseDto;
import org.ai.testing.env.VariableResolver;
import org.ai.testing.env.VariableStore;
import org.ai.testing.executor.ExecutorDispatcher;
import org.ai.testing.executor.common.RequestBuilder;
import org.ai.testing.executor.common.RequestNormalizer;
import org.ai.testing.extract.ResponseExtractor;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testcase.dto.TestCaseResultDto;
import org.ai.testing.testcase.dto.TestStatus;
import org.ai.testing.testcase.factory.TestCaseRequestFactory;
import org.ai.testing.util.CurlBuilder;
import org.ai.testing.util.Redaction;
import org.ai.testing.util.Strings;
import org.ai.testing.validation.ValidationEngine;
import org.ai.testing.validation.dto.ValidationSummaryDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Executes one test case end to end.
 *
 * <p>The pipeline, in order:</p>
 * <ol>
 *   <li>register the case's pre-request variables;</li>
 *   <li>build a private copy of the request;</li>
 *   <li>substitute <code>{{variables}}</code>;</li>
 *   <li>normalise headers, parameters and the body;</li>
 *   <li>resolve and apply credentials;</li>
 *   <li>snapshot the request exactly as it will be sent;</li>
 *   <li>send it, with retries;</li>
 *   <li>capture extracts into the variable store;</li>
 *   <li>validate every assertion.</li>
 * </ol>
 *
 * <p>Steps 1, 3, 5 and 8 existed as classes in the previous version but were
 * never invoked, so variables, authentication and response chaining had no
 * effect on a run.</p>
 */
public class TestCaseExecutor {

    private final TestCaseRequestFactory requestFactory = new TestCaseRequestFactory();
    private final RequestNormalizer normalizer = new RequestNormalizer();
    private final RequestBuilder urlBuilder = new RequestBuilder();
    private final VariableResolver variableResolver = new VariableResolver();
    private final AuthApplicator authApplicator = new AuthApplicator();
    private final ResponseExtractor responseExtractor = new ResponseExtractor();
    private final ValidationEngine validationEngine = new ValidationEngine();

    private final ExecutorDispatcher dispatcher;
    private final boolean redactSecrets;

    public TestCaseExecutor() {
        this(new ExecutorDispatcher(), true);
    }

    public TestCaseExecutor(ExecutorDispatcher dispatcher, boolean redactSecrets) {
        this.dispatcher = dispatcher == null ? new ExecutorDispatcher() : dispatcher;
        this.redactSecrets = redactSecrets;
    }

    /** Inherited credentials for the enclosing suite and run. */
    public record AuthScope(AuthDto suiteAuth, AuthDto runAuth) {

        public static AuthScope none() {
            return new AuthScope(null, null);
        }
    }

    public TestCaseResultDto execute(TestCaseDto testCase, VariableStore store) {
        return execute(testCase, store, AuthScope.none());
    }

    public TestCaseResultDto execute(TestCaseDto testCase,
                                     VariableStore store,
                                     AuthScope authScope) {

        if (testCase == null) {
            throw new IllegalArgumentException("Test case cannot be null");
        }

        VariableStore variables = store == null ? new VariableStore() : store;
        AuthScope scope = authScope == null ? AuthScope.none() : authScope;

        TestCaseResultDto result = new TestCaseResultDto();
        result.setTestCaseId(testCase.getTestCaseId());
        result.setTestCaseName(testCase.getTestCaseName());
        result.setDescription(testCase.getDescription());
        result.setMethod(Strings.upper(testCase.getMethod()));
        result.setTags(testCase.getTags());

        if (!testCase.isEnabled()) {
            result.setStatus(TestStatus.SKIPPED);
            result.setMessage("Test case is disabled");
            return result;
        }

        result.setStartedAt(LocalDateTime.now());
        long started = System.nanoTime();

        try {
            variables.putAllRuntime(
                    variableResolver.resolveMap(testCase.getPreRequestVariables(), variables));

            BaseRequestDto request = requestFactory.createRequest(testCase);
            variableResolver.resolveRequest(request, variables);
            normalizer.normalize(request);

            AuthDto effectiveAuth = authApplicator.resolve(
                    request.getAuth(), scope.suiteAuth(), scope.runAuth());
            if (effectiveAuth != null) {
                effectiveAuth = effectiveAuth.copy();
                variableResolver.resolveAuth(effectiveAuth, variables);
            }
            result.setAuthApplied(authApplicator.apply(request, effectiveAuth));

            String resolvedUrl = urlBuilder.buildUrl(request);
            result.setCurlCommand(CurlBuilder.build(
                    result.getMethod(), resolvedUrl, request, redactSecrets));
            result.setRequest(reportableRequest(request));

            warnAboutUnresolvedVariables(result, resolvedUrl);

            ResponseDto response = dispatcher.execute(testCase.getMethod(), request);
            result.setResponse(response);

            captureExtracts(testCase, response, variables, result);

            ValidationSummaryDto summary = validationEngine.validate(
                    response, testCase.getExpectedStatusCode(), testCase.getAssertions());
            result.setValidationSummary(summary);

            if (summary.getTotal() == 0) {
                result.setStatus(TestStatus.PASSED);
                result.setMessage("Request completed with status "
                        + response.getStatusCode() + "; no assertions were configured");
            } else if (summary.isPassed()) {
                result.setStatus(TestStatus.PASSED);
                result.setMessage(summary.getPassedCount() + " of "
                        + summary.getTotal() + " assertions passed");
            } else {
                result.setStatus(TestStatus.FAILED);
                result.setMessage(summary.getFailedCount() + " of "
                        + summary.getTotal() + " assertions failed: "
                        + summary.firstFailure().getMessage());
            }

        } catch (RuntimeException e) {
            result.setStatus(TestStatus.ERROR);
            result.setErrorType(e.getClass().getSimpleName());
            result.setErrorDetail(rootCauseMessage(e));
            result.setMessage("Test case could not be executed: " + rootCauseMessage(e));
        } finally {
            result.setExecutionTimeMs((System.nanoTime() - started) / 1_000_000L);
        }

        return result;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Snapshot of the sent request, with credentials masked when configured. */
    private BaseRequestDto reportableRequest(BaseRequestDto request) {
        BaseRequestDto copy = request.copy();
        copy.setHeaders(Redaction.maskHeaders(copy.getHeaders(), redactSecrets));
        copy.setAuth(null);
        return copy;
    }

    private void captureExtracts(TestCaseDto testCase, ResponseDto response,
                                 VariableStore variables, TestCaseResultDto result) {

        List<ResponseExtractor.Capture> captures =
                responseExtractor.extract(testCase.getExtracts(), response, variables);

        result.setCapturedVariables(responseExtractor.capturedValues(captures));
        for (ResponseExtractor.Capture capture : captures) {
            if (!capture.found()) {
                result.getFailedExtracts().add(
                        capture.variableName() + " <- " + capture.expression());
            }
        }
    }

    private void warnAboutUnresolvedVariables(TestCaseResultDto result, String url) {
        List<String> unresolved = variableResolver.unresolvedNames(url);
        if (!unresolved.isEmpty()) {
            result.setErrorDetail("Unresolved variables in URL: "
                    + String.join(", ", unresolved));
        }
    }

    private String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = Strings.defaultIfBlank(cause.getMessage(),
                cause.getClass().getSimpleName());
        return Strings.truncate(message, 400);
    }
}
