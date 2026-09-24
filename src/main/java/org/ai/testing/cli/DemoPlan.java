package org.ai.testing.cli;

import org.ai.testing.dto.common.AssertionDto;
import org.ai.testing.dto.common.BaseRequestDto;
import org.ai.testing.dto.common.ExtractDto;
import org.ai.testing.testcase.dto.TestCaseDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.testsuite.dto.TestSuiteDto;
import org.ai.testing.validation.AssertionOperator;
import org.ai.testing.validation.AssertionType;

/**
 * A built-in plan that exercises every feature without needing a file.
 *
 * <p>It targets the public Swagger Petstore and httpbin services and covers
 * variables, response chaining, JSONPath assertions, latency assertions,
 * headers, a POST body, tags, and a deliberately disabled case, so a first run
 * shows what a populated report looks like.</p>
 */
public final class DemoPlan {

    private static final String PETSTORE = "https://petstore3.swagger.io/api/v3";

    private DemoPlan() {
    }

    public static TestRunDto build() {
        TestRunDto run = new TestRunDto();
        run.setRunId("RUN-DEMO-001");
        run.setRunName("Swagger Petstore Demo Run");
        run.setEnvironment("demo");
        run.setDescription("Built-in example plan shipped with the framework");
        run.getCollectionVariables().put("baseUrl", PETSTORE);
        run.getCollectionVariables().put("petStatus", "available");

        run.add(contractSuite());
        run.add(inventorySuite());
        return run;
    }

    private static TestSuiteDto contractSuite() {
        TestSuiteDto suite = new TestSuiteDto("SUITE-CONTRACT", "API contract");
        suite.setDescription("Checks the published OpenAPI document is reachable and well formed");

        TestCaseDto openApi = new TestCaseDto("TC-CONTRACT-001",
                "OpenAPI document is served", "GET");
        openApi.setDescription("The definition should be JSON and name the Swagger Petstore");
        openApi.setRequest(new BaseRequestDto()
                .header("Accept", "application/json"));
        openApi.getRequest().setUrl("{{baseUrl}}/openapi.json");
        openApi.setExpectedStatusCode(200);
        openApi.tag("smoke", "contract");
        openApi.assertion(AssertionDto.header("Content-Type",
                AssertionOperator.CONTAINS, "json"));
        openApi.assertion(AssertionDto.jsonPath("$.openapi",
                AssertionOperator.STARTS_WITH, "3."));
        openApi.assertion(AssertionDto.jsonPath("$.info.title",
                AssertionOperator.CONTAINS, "Petstore"));
        openApi.assertion(AssertionDto.body(AssertionOperator.NOT_EMPTY, ""));
        openApi.assertion(AssertionDto.responseTimeBelow(15_000));
        openApi.extract(ExtractDto.fromBody("apiTitle", "$.info.title"));
        openApi.extract(ExtractDto.fromHeader("servedBy", "Server"));
        suite.add(openApi);

        return suite;
    }

    private static TestSuiteDto inventorySuite() {
        TestSuiteDto suite = new TestSuiteDto("SUITE-INVENTORY", "Store inventory");
        suite.setDescription("Reads the store inventory and a filtered pet listing");

        TestCaseDto inventory = new TestCaseDto("TC-STORE-001",
                "Inventory returns a JSON object", "GET");
        inventory.setRequest(new BaseRequestDto().header("Accept", "application/json"));
        inventory.getRequest().setUrl("{{baseUrl}}/store/inventory");
        inventory.setExpectedStatusCode(200);
        inventory.tag("smoke");
        inventory.assertion(AssertionDto.body(AssertionOperator.STARTS_WITH, "{"));
        inventory.assertion(AssertionDto.responseTimeBelow(15_000));
        suite.add(inventory);

        TestCaseDto findByStatus = new TestCaseDto("TC-PET-001",
                "Pets can be filtered by status", "GET");
        findByStatus.setRequest(new BaseRequestDto()
                .header("Accept", "application/json")
                .query("status", "{{petStatus}}"));
        findByStatus.getRequest().setUrl("{{baseUrl}}/pet/findByStatus");
        findByStatus.setExpectedStatusCode(200);
        findByStatus.tag("regression");
        findByStatus.assertion(new AssertionDto(AssertionType.CONTENT_TYPE, "Content-Type",
                AssertionOperator.CONTAINS, "json"));
        suite.add(findByStatus);

        TestCaseDto disabled = new TestCaseDto("TC-PET-999",
                "Deliberately disabled example", "DELETE");
        disabled.setRequest(new BaseRequestDto());
        disabled.getRequest().setUrl("{{baseUrl}}/pet/1");
        disabled.setEnabled(false);
        disabled.setDescription("Shows that a disabled case is reported as skipped, "
                + "not as a failure");
        suite.add(disabled);

        return suite;
    }
}
