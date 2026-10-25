import os
import json
import config

def run_test_design(file_path: str, output_format: str = "text") -> str:
    """
    Designs a comprehensive Unit Test plan and test cases for a production Kotlin class.
    Adheres strictly to the project's testing stack: JUnit, MockK, Turbine for Flows,
    and Kotlinx Coroutines Test (runTest).
    """
    client = config.get_client()

    rules = ""
    if os.path.exists("AGENTS.md"):
        with open("AGENTS.md", "r", encoding="utf-8") as f:
            rules = f.read()

    with open(file_path, "r", encoding="utf-8") as f:
        code = f.read()

    testing_standards = """
    Android Unit Testing Standards (JUnit + MockK + Turbine + Coroutines Test):
    
    1. Testing Stack & Tools:
       - Mocking: Exclusively use MockK (`mockk<T>()`, `coEvery { ... } returns ...`, `coVerify(exactly = 1) { ... }`).
       - Asynchronous Flows: Exclusively use Turbine for Flow / StateFlow testing (`flow.test { assertThat(awaitItem()).isEqualTo(...); cancelAndIgnoreRemainingEvents() }`).
       - Coroutines: Use `kotlinx.coroutines.test.runTest` for all suspend functions and flow tests.
       
    2. Comprehensive Scenario Coverage (Happy Path + Edge Cases):
       - Happy Path: Verify expected output, entity mapping, and state transition.
       - Business Error Cases: Verify appropriate `Result.Error(DomainError.*)` returns when inputs are invalid (negative numbers, empty IDs).
       - Boundary / Not Found Cases: Verify handling of empty collections, missing IDs, or null lookups.
       - Exception Handling: Verify proper behavior on unexpected repository or DataSource exceptions without swallowing CancellationException.
       
    3. Test Organization & Naming:
       - Follow Gherkin-style naming: `methodName_givenCondition_thenExpectedResult()`
       - Structure tests using the standard Given / When / Then (Arrange / Act / Assert) pattern.
       - Mock dependencies strictly at the layer interface level (mock Repositories for UseCases, mock UseCases for ViewModels).
    """

    if output_format == "json":
        prompt = f"""
        Act as a Principal Android QA & Test Automation Specialist.
        Design a complete, production-grade Unit Test plan for this Kotlin class:
        
        {testing_standards}
        
        CODING RULES (AGENTS.md):
        {rules}
        
        PRODUCTION CODE TO TEST ({file_path}):
        {code}
        
        Return a strict JSON object matching this schema:
        {{
          "summary": {{
            "file": "{file_path}",
            "suggested_test_file": "{file_path.replace('app/src/main/java/', 'app/src/test/java/').replace('.kt', 'Test.kt')}",
            "recommended_test_cases_count": <int>
          }},
          "findings": [
            {{
              "severity": "info",
              "category": "testing",
              "file": "{file_path}",
              "line": <line_number_or_0>,
              "title": "<Test case scenario title>",
              "message": "<Description of the scenario to verify (Given / When / Then)>",
              "suggestion": "<MockK / Turbine / runTest Kotlin test implementation snippet>",
              "remediation": "<MockK / Turbine / runTest Kotlin test implementation snippet>"
            }}
          ]
        }}
        """
        from google.genai import types
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt,
            config_args=types.GenerateContentConfig(response_mime_type="application/json")
        )
        return response.text
    else:
        prompt = f"""
        Act as a Principal Android QA & Test Automation Specialist.
        Design a complete, production-grade Unit Test plan for this Kotlin class:
        
        {testing_standards}
        
        CODING RULES (AGENTS.md):
        {rules}
        
        PRODUCTION CODE TO TEST ({file_path}):
        {code}
        
        Provide a structured testing specification with:
        1. Test Strategy Overview (dependencies to mock, test dispatchers, flows to collect).
        2. Test Cases Matrix (Happy path, error cases, edge conditions).
        3. Ready-to-use Kotlin code snippets using MockK and Turbine.
        """
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt
        )
        return response.text
