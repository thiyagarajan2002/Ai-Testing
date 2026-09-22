package org.ai.testing.TestData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ai.testing.TestData.dto.TestCaseDTO;
import org.ai.testing.TestData.dto.TestCaseResponseDTO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestCaseGen {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static final String OPENROUTER_URL =
            "https://openrouter.ai/api/v1/chat/completions";

    private static final String MODEL =
            "openai/gpt-4o";

    private static final int MAX_TOKENS = 2000;

    /*
     * Generate the final AI prompt using:
     *
     * Swagger.yml
     * +
     * TestDataGenerate.md
     */
    public static String prompt_Generate() throws IOException {

        Path swaggerPath =
                Path.of(
                        "src/main/java/org/ai/testing/TestData/Swagger.yml"
                );

        Path promptTemplatePath =
                Path.of(
                        "src/main/java/org/ai/testing/TestData/TestDataGenerate.md"
                );

        String sourceData =
                Files.readString(swaggerPath);

        String targetData =
                Files.readString(promptTemplatePath);

        targetData =
                targetData.replace(
                        "${Swagger.yml}",
                        sourceData
                );

        return targetData;
    }

    /*
     * Send the prompt to OpenRouter,
     * extract the AI JSON response,
     * convert it into DTO objects.
     */
    public static TestCaseResponseDTO generate_TestCase(String prompt)
            throws IOException, InterruptedException {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "Prompt cannot be null or empty."
            );
        }

        /*
         * Read API key from environment variable.
         */
        String apiKey =
                System.getenv("OPENROUTER_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {

            throw new RuntimeException(
                    "OPENROUTER_API_KEY environment variable is not set."
            );
        }

        /*
         * Escape prompt for JSON.
         */
        String escapedPrompt =
                escapeJson(prompt);

        /*
         * Build OpenRouter Chat Completions request.
         */
        String requestBody =
                """
                        {
                          "model": "%s",
                          "messages": [
                            {
                              "role": "user",
                              "content": "%s"
                            }
                          ],
                          "max_tokens": %d,
                          "stream": false
                        }
                        """.formatted(
                        MODEL,
                        escapedPrompt,
                        MAX_TOKENS
                );

        /*
         * Create HTTP request.
         */
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        OPENROUTER_URL
                                )
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "Authorization",
                                "Bearer " + apiKey
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(requestBody)
                        )
                        .build();

        System.out.println();
        System.out.println("======================================");
        System.out.println("Calling OpenRouter");
        System.out.println("======================================");

        System.out.println(
                "Model      : " + MODEL
        );

        System.out.println(
                "Max Tokens : " + MAX_TOKENS
        );

        /*
         * Send ONLY ONE API request.
         */
        HttpResponse<String> response =
                HTTP_CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        /*
         * Print HTTP status.
         */
        System.out.println(
                "Status     : " + response.statusCode()
        );

        /*
         * Check API response.
         */
        if (response.statusCode() != 200) {

            System.out.println();
            System.out.println("===== OPENROUTER ERROR =====");
            System.out.println(response.body());

            throw new RuntimeException(
                    "OpenRouter API call failed. "
                            + "Status: "
                            + response.statusCode()
                            + "\nResponse: "
                            + response.body()
            );
        }

        System.out.println(
                "OpenRouter API call successful."
        );

        /*
         * Parse OpenRouter response.
         */
        JsonNode openRouterResponse =
                OBJECT_MAPPER.readTree(
                        response.body()
                );

        /*
         * Validate choices.
         */
        JsonNode choices =
                openRouterResponse.path("choices");

        if (!choices.isArray()
                || choices.isEmpty()) {

            throw new RuntimeException(
                    "OpenRouter response does not contain choices."
            );
        }

        /*
         * Extract:
         *
         * choices[0]
         *      -> message
         *          -> content
         */
        JsonNode message =
                choices
                        .get(0)
                        .path("message");

        String aiContent =
                message
                        .path("content")
                        .asText(null);

        if (aiContent == null
                || aiContent.isBlank()) {

            throw new RuntimeException(
                    "OpenRouter returned empty AI content."
            );
        }

        System.out.println();
        System.out.println("======================================");
        System.out.println("AI CONTENT");
        System.out.println("======================================");

        System.out.println(aiContent);

        /*
         * Extract pure JSON from AI response.
         */
        String jsonContent =
                extractJson(aiContent);

        System.out.println();
        System.out.println("======================================");
        System.out.println("EXTRACTED JSON");
        System.out.println("======================================");

        System.out.println(jsonContent);

        /*
         * Validate that extracted content
         * is actually JSON.
         */
        JsonNode generatedJson =
                OBJECT_MAPPER.readTree(
                        jsonContent
                );

        if (!generatedJson.has("testCases")) {

            throw new RuntimeException(
                    "Generated JSON does not contain "
                            + "'testCases'."
            );
        }

        /*
         * Convert JSON into DTO.
         */
        TestCaseResponseDTO result =
                OBJECT_MAPPER.readValue(
                        jsonContent,
                        TestCaseResponseDTO.class
                );

        /*
         * Validate DTO.
         */
        if (result.getTestCases() == null
                || result.getTestCases().isEmpty()) {

            throw new RuntimeException(
                    "No test cases were generated."
            );
        }

        /*
         * Display generated DTOs.
         */
        printTestCases(result);

        System.out.println();
        System.out.println("======================================");
        System.out.println(
                "Generated Test Cases: "
                        + result.getTestCases().size()
        );
        System.out.println(
                "Test case generation completed successfully."
        );
        System.out.println("======================================");

        return result;
    }

    /*
     * Escape a Java String so that it can safely
     * be inserted into a JSON request body.
     */
    private static String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /*
     * Remove Markdown code fences from AI response.
     *
     * Handles:
     *
     * ```json
     * {...}
     * ```
     *
     * and:
     *
     * ```
     * {...}
     * ```
     */
    private static String extractJson(String content) {

        if (content == null
                || content.isBlank()) {

            throw new RuntimeException(
                    "AI response content is empty."
            );
        }

        String json =
                content.trim();

        /*
         * Remove opening fence.
         */
        if (json.startsWith("```json")) {

            json =
                    json.substring(
                            "```json".length()
                    );

        } else if (json.startsWith("```")) {

            json =
                    json.substring(
                            "```".length()
                    );
        }

        /*
         * Remove closing fence.
         */
        if (json.endsWith("```")) {

            json =
                    json.substring(
                            0,
                            json.length() - 3
                    );
        }

        return json.trim();
    }

    /*
     * Print DTO objects.
     */
    private static void printTestCases(
            TestCaseResponseDTO result) {

        System.out.println();
        System.out.println("======================================");
        System.out.println("GENERATED TEST CASE DTOs");
        System.out.println("======================================");

        for (TestCaseDTO testCase :
                result.getTestCases()) {

            System.out.println();
            System.out.println("--------------------------------------");

            System.out.println(
                    "Test Case ID : "
                            + testCase.getTestCaseId()
            );

            System.out.println(
                    "API          : "
                            + testCase.getApi()
            );

            System.out.println(
                    "Method       : "
                            + testCase.getMethod()
            );

            System.out.println(
                    "Category     : "
                            + testCase.getCategory()
            );

            System.out.println(
                    "Priority     : "
                            + testCase.getPriority()
            );

            System.out.println(
                    "Risk         : "
                            + testCase.getRiskRationale()
            );

            System.out.println(
                    "Scenario     : "
                            + testCase.getScenario()
            );

            System.out.println(
                    "Request Data : "
                            + testCase.getRequestData()
            );

            System.out.println(
                    "Expected Code: "
                            + testCase.getExpectedStatusCode()
            );

            System.out.println(
                    "Expected Resp: "
                            + testCase.getExpectedResponse()
            );
        }

        System.out.println("--------------------------------------");
    }

    /*
     * Main method.
     */
    public static void main(String[] args)
            throws IOException, InterruptedException {

        System.out.println();
        System.out.println("======================================");
        System.out.println("AI TEST CASE GENERATOR");
        System.out.println("======================================");

        /*
         * Step 1:
         * Generate prompt from Swagger.yml
         * and TestDataGenerate.md.
         */
        System.out.println();
        System.out.println(
                "Step 1: Generating prompt..."
        );

        String finalPrompt =
                prompt_Generate();

        System.out.println(
                "Prompt generated successfully."
        );

        /*
         * Step 2:
         * Send prompt to OpenRouter.
         */
        System.out.println();
        System.out.println(
                "Step 2: Calling OpenRouter..."
        );

        TestCaseResponseDTO result =
                generate_TestCase(
                        finalPrompt
                );

        /*
         * Step 3:
         * DTO result is now available.
         */
        System.out.println();
        System.out.println(
                "Step 3: DTO extraction completed."
        );

        System.out.println(
                "Total DTO test cases: "
                        + result.getTestCases().size()
        );

        System.out.println();
        System.out.println("======================================");
        System.out.println("PROCESS COMPLETED");
        System.out.println("======================================");
    }
}