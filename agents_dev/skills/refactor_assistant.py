import os
import json
import config

def run_refactor_analysis(file_path: str, output_format: str = "text") -> str:
    """
    Analyzes Kotlin code for refactoring opportunities, code smells, and complexity.
    Strictly enforces API grounding to prevent hallucinating non-existent methods on external dependencies.
    """
    client = config.get_client()

    rules = ""
    if os.path.exists("AGENTS.md"):
        with open("AGENTS.md", "r", encoding="utf-8") as f:
            rules = f.read()

    with open(file_path, "r", encoding="utf-8") as f:
        code = f.read()

    refactor_checklist = """
    Kotlin & Clean Architecture Refactoring Directives:
    
    1. STRICT ANTI-HALLUCINATION & CONTRACT PRESERVATION RULE:
       - NEVER suggest or invent non-existent methods, suspend extensions, or new signatures on imported DAOs, Repositories, or Android SDK classes.
       - Any proposed refactoring MUST compile against existing imported APIs and class interfaces.
       
    2. Code Smells & Complexity Reduction:
       - Deconstruct functions with high cyclomatic complexity (> 10 conditional branches).
       - Break down large functions (> 30 lines) violating Single Responsibility into smaller private helper functions.
       - Flatten deeply nested logic (> 3 levels of nested if/when/try blocks) using early returns and guard clauses.
       
    3. DRY (Don't Repeat Yourself) & Constant Hygiene:
       - Extract duplicate hardcoded string literals into companion object constants (SCREAMING_SNAKE_CASE).
       - Eliminate redundant error mapping or data transformation boilerplate.
       
    4. Kotlin Idioms & Immutability:
       - Replace imperative for-loops with idiomatic functional operations (map, filter, fold) when clearer.
       - Enforce immutability: prefer 'val' over 'var' and immutable collections.
       - Never swallow coroutine CancellationException in catch blocks.
       
    5. Clean Architecture Cohesion:
       - Keep presentation state immutable (StateFlow).
       - Maintain clear separation between domain models and persistence entities.
    """

    if output_format == "json":
        prompt = f"""
        Act as a Principal Kotlin Software Engineer and Refactoring Specialist.
        Analyze the following Kotlin code for refactoring opportunities:
        
        {refactor_checklist}
        
        CODING RULES (AGENTS.md):
        {rules}
        
        CODE TO ANALYZE ({file_path}):
        {code}
        
        Return a strict JSON object matching this schema:
        {{
          "summary": {{
            "file": "{file_path}",
            "refactor_opportunity_count": <int>
          }},
          "findings": [
            {{
              "severity": "warning" | "info",
              "category": "refactor",
              "file": "{file_path}",
              "line": <line_number_or_0>,
              "title": "<Concise code smell title>",
              "message": "<Description of the code smell, high complexity, or duplication>",
              "suggestion": "<Concrete surgical refactored code without breaking existing API contracts>",
              "remediation": "<Concrete surgical refactored code without breaking existing API contracts>"
            }}
          ]
        }}
        If the code is already concise, clean, well-structured, and idiomatic, return findings as an empty list [].
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
        Act as a Principal Kotlin Software Engineer and Refactoring Specialist.
        Analyze the following Kotlin code for refactoring opportunities:
        
        {refactor_checklist}
        
        CODING RULES (AGENTS.md):
        {rules}
        
        CODE TO ANALYZE ({file_path}):
        {code}
        
        Provide a structured refactoring report with:
        1. Code Quality Overview (Complexity, Maintainability, Readability).
        2. Identified Code Smells with exact line numbers and explanations.
        3. Surgical Kotlin code refactoring recommendations (respecting existing API contracts).
        """
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt
        )
        return response.text
