import os
import json
import config

def run_kdoc_audit(file_path: str, output_format: str = "text") -> str:
    """
    Audits a Kotlin file for KDoc documentation completeness, style, and usefulness.
    Enforces meaningful documentation for public APIs, interfaces, UseCases, and Repositories
    while rejecting redundant, low-value comments.
    """
    client = config.get_client()

    rules = ""
    if os.path.exists("AGENTS.md"):
        with open("AGENTS.md", "r", encoding="utf-8") as f:
            rules = f.read()

    with open(file_path, "r", encoding="utf-8") as f:
        code = f.read()

    kdoc_guidelines = """
    Android Kotlin Documentation (KDoc) Quality Standards:
    
    1. Scope of Documentation:
       - Mandatory KDoc for all public classes, interfaces, UseCases, and Repository functions.
       - Internal helper functions should be self-documenting; avoid cluttering private code with unnecessary comments unless complex algorithms or business calculations are involved.
       
    2. Meaningful Documentation vs. Redundancy:
       - PROHIBITED: Tautological comments that merely repeat the identifier name (e.g., '/** Gets the student */ fun getStudent(...)').
       - REQUIRED: Meaningful descriptions explaining business purpose, caching behavior, offline-first semantics, or concurrency model.
       
    3. Tag Hygiene (@param, @return, @throws):
       - @param: Document constraints, units, or expectations for parameters (e.g., professorId, non-empty studentId).
       - @return: Clearly describe the return value on success and what domain errors can be encapsulated in Result.Error.
       - @throws / Coroutines: Document thread expectations (e.g. safe to call from any thread via withContext(Dispatchers.IO)) and flow emission guarantees.
       
    4. Language & Tone:
       - 100% technical English in professional, concise documentation style.
    """

    if output_format == "json":
        prompt = f"""
        Act as a Principal Kotlin Technical Writer and API Documentation Specialist.
        Audit the KDoc documentation of this Kotlin file:
        
        {kdoc_guidelines}
        
        CODING RULES (AGENTS.md):
        {rules}
        
        CODE TO AUDIT ({file_path}):
        {code}
        
        Return a strict JSON object matching this schema:
        {{
          "summary": {{
            "file": "{file_path}",
            "missing_kdoc_count": <int>
          }},
          "findings": [
            {{
              "severity": "info" | "warning",
              "category": "kdoc",
              "file": "{file_path}",
              "line": <line_number_or_0>,
              "title": "<Concise documentation issue title>",
              "message": "<Description of missing KDoc or redundant comment to remove/improve>",
              "suggestion": "<Complete, professional KDoc replacement block in Kotlin>",
              "remediation": "<Complete, professional KDoc replacement block in Kotlin>"
            }}
          ]
        }}
        If all public APIs already have high-quality, professional KDoc comments, return findings as an empty list [].
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
        Act as a Principal Kotlin Technical Writer and API Documentation Specialist.
        Audit the KDoc documentation of this Kotlin file:
        
        {kdoc_guidelines}
        
        CODING RULES (AGENTS.md):
        {rules}
        
        CODE TO AUDIT ({file_path}):
        {code}
        
        Provide a structured documentation report with:
        1. API Documentation Coverage Status.
        2. Missing or redundant KDocs with line numbers.
        3. Ready-to-paste KDoc suggestions with complete @param and @return tags.
        """
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt
        )
        return response.text
