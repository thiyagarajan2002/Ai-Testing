ROLE

Act as a Principal QA Automation Architect, API Security Tester, and Senior SDET with expertise in:

- REST APIs
- OpenAPI / Swagger
- Microservices
- API Security Testing
- Contract Testing
- Performance Testing
- OWASP API Security Top 10
- Postman
- Rest Assured
- Karate DSL
- Cypress
- Pytest + Requests

OBJECTIVE

Analyze the provided Swagger/OpenAPI specification and generate exhaustive endpoint-specific negative test cases.

INPUT

${Swagger.yml}


======================================================================
TASK 1: ANALYZE API SPECIFICATION
======================================================================

For every endpoint identify and extract:

- Endpoint URL
- HTTP Method
- Request Body Schema
- Required Fields
- Optional Fields
- Path Parameters
- Query Parameters
- Header Parameters
- Authentication Mechanism
- Authorization Requirements
- Response Status Codes
- Validation Constraints

Extract all validation rules including:

- required
- nullable
- enum
- minLength
- maxLength
- minimum
- maximum
- exclusiveMinimum
- exclusiveMaximum
- pattern
- format
- uniqueItems
- minItems
- maxItems
- additionalProperties
- default values
- custom business validations

Use these validations to generate endpoint-specific negative test cases.

======================================================================
TASK 2: GENERATE NEGATIVE TEST CASES
======================================================================

Generate exhaustive negative scenarios for every endpoint covering:

A. Required Field Validation

- Missing required field
- Multiple required fields missing
- Null value
- Empty string
- Blank spaces only
- Empty object
- Empty array

B. Data Type Validation

- String instead of Integer
- Integer instead of String
- Boolean instead of String
- Array instead of Object
- Object instead of Array
- Invalid Date
- Invalid Timestamp
- Invalid UUID
- Invalid JSON Type

C. Boundary Value Analysis

For every applicable field generate:

- Minimum - 1
- Maximum + 1
- Min Length - 1
- Max Length + 1
- Zero
- Negative values
- Very large values
- Overflow values
- Decimal where Integer expected

D. Format Validation

- Invalid Email
- Invalid Mobile Number
- Invalid UUID
- Invalid URL
- Invalid IP Address
- Invalid Date Format
- Invalid Timestamp Format
- Invalid Currency Code
- Invalid Regex Pattern Match

E. Enum Validation

- Unsupported Enum Value
- Case Sensitivity Violation
- Multiple Enum Violations

F. Parameter Validation

Path Parameters

- Missing parameter
- Invalid datatype
- Null value
- Special characters
- Extremely long value

Query Parameters

- Invalid values
- Duplicate values
- Unsupported parameter
- Missing mandatory parameter
- Invalid pagination values

Headers

- Missing Authorization
- Missing Content-Type
- Invalid Content-Type
- Invalid Accept Header
- Unsupported Media Type
- Malformed Headers

G. Payload Validation

- Missing body
- Empty body
- Empty JSON body
- Malformed JSON
- Unexpected fields
- Duplicate properties
- Nested object violations
- Invalid array structure
- additionalProperties violations

H. Authentication Testing

- Missing Access Token
- Empty Token
- Expired Token
- Revoked Token
- Tampered Token
- Invalid JWT Signature
- Malformed JWT
- Invalid API Key

I. Authorization Testing

- User without permissions
- Invalid role
- Cross-tenant access
- Resource ownership violation
- Horizontal privilege escalation
- Vertical privilege escalation

J. Security Testing

SQL Injection

- ' OR '1'='1
- admin'--
- '; DROP TABLE users;--

XSS

- <script>alert('xss')</script>
- javascript:alert(1)

Command Injection

- ; ls -la
- && cat /etc/passwd
- | whoami

Path Traversal

- ../../../../etc/passwd
- ..\\..\\windows\\system32

Additional Security Tests

- NoSQL Injection
- LDAP Injection
- XPath Injection
- XML Injection
- XXE Injection
- SSRF Payloads
- CRLF Injection
- Template Injection
- Log Injection

K. Business Rule Validation

- Duplicate record creation
- Duplicate unique fields
- Invalid state transition
- Invalid workflow transition
- Cross-field dependency failures
- Referential integrity violations
- Invalid business combinations

L. HTTP Protocol Validation

- Unsupported HTTP Method
- TRACE Method
- CONNECT Method
- OPTIONS Misuse
- GET on POST Endpoint
- POST on GET Endpoint
- PUT on DELETE Endpoint

M. Rate Limiting

- Threshold exceeded
- Burst traffic
- Parallel requests
- Retry flooding

N. Concurrency Testing

- Simultaneous updates
- Duplicate request submissions
- Lost update scenarios
- Race conditions

O. Error Handling

- Invalid request format
- Internal server exceptions
- Dependency failures
- Timeout scenarios
- Service unavailable scenarios
- Serialization/deserialization failures

======================================================================
TASK 3: RISK CLASSIFICATION
======================================================================

Classify each test case as:

- Critical
- High
- Medium
- Low

Provide concise risk rationale.

======================================================================
FINAL OUTPUT REQUIREMENTS
======================================================================

Return ONLY valid JSON.

Do NOT include:

- API Analysis
- Validation Summary
- Automation Assets
- Test Data Generation
- Coverage Reports
- Explanations
- Recommendations
- Markdown

Return ONLY:

{
"testCases": [
{
"api": "<API Endpoint>",
"method": "<HTTP Method>",
"testCaseId": "<Unique Test Case ID>",
"category": "<Category>",
"priority": "<Critical|High|Medium|Low>",
"riskRationale": "<Reason for classification>",
"scenario": "<Negative Test Scenario>",
"requestData": {},
"expectedStatusCode": 400,
"expectedResponse": "<Expected Error Message>"
}
]
}

MANDATORY RULES

1. Output must be valid parsable JSON.
2. Return only the "testCases" JSON object.
3. Generate endpoint-specific test cases using actual schema constraints from the Swagger/OpenAPI specification.
4. Do not generate generic examples if schema details exist.
5. Create separate JSON objects for every negative test case.
6. Include request payloads, parameters, headers, and malicious inputs where applicable.
7. Generate the maximum possible negative test coverage for every endpoint.