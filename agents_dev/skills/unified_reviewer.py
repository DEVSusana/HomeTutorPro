"""
Universal Unified Code Reviewer
Performs an all-in-one comprehensive inspection (Architecture, Security, KDoc, Quality)
in a single API call per file, using the Context Engine to prevent hallucinations.
"""
import os
import json
import config
from context_engine import (
    classify_component,
    get_component_boundaries,
    get_related_project_context
)


def run_unified_review(file_path: str) -> dict:
    """
    Executes a comprehensive, context-grounded code review on a single Kotlin file.
    Returns a dictionary with summary counts and actionable findings (critical & warning).
    """
    client = config.get_client()

    if not os.path.exists(file_path):
        return {"summary": {"file": file_path, "critical_count": 0, "warning_count": 0}, "findings": []}

    with open(file_path, "r", encoding="utf-8") as f:
        code = f.read()

    comp_type = classify_component(file_path, code)
    boundaries = get_component_boundaries(comp_type)
    sibling_context = get_related_project_context(file_path)

    standards_doc = ""
    standards_path = os.path.join(os.path.dirname(__file__), "PROJECT_STANDARDS.md")
    if os.path.exists(standards_path):
        with open(standards_path, "r", encoding="utf-8") as f:
            standards_doc = f.read()

    in_scope_list = "\n".join(f"- {item}" for item in boundaries["in_scope"])
    out_of_scope_list = "\n".join(f"- {item}" for item in boundaries["out_of_scope"])

    prompt = f"""
Act as a Principal Android Software Engineer & Security Auditor.
Perform a surgical code review on this Kotlin file.

TARGET COMPONENT: {boundaries['title']} ({comp_type})
FILE: {file_path}

VERIFICATION CHECKLIST (IN-SCOPE FOR THIS COMPONENT):
{in_scope_list}

STRICT ANTI-HALLUCINATION BOUNDARIES (DO NOT REPORT AS VIOLATIONS):
{out_of_scope_list}
- NEVER report a missing responsibility if it belongs to another layer or a sibling class.
- NEVER flag Room DAOs for lacking database-level encryption (SQLCipher) or for querying PII entities.
- Parameterized Room queries (:param) are completely safe against SQL injection.
- Do NOT generate informational test cases or stylistic nitpicks. Report ONLY actionable defects.

PROJECT ARCHITECTURAL CONTEXT:
{sibling_context}

ENGINEERING STANDARDS REFERENCE:
{standards_doc[:2500]}

CODE TO REVIEW:
{code}

OUTPUT INSTRUCTIONS:
Return a strict JSON object matching this schema. Report ONLY findings with severity "critical" or "warning".
If the code is sound and compliant, return "findings": [].

{{
  "summary": {{
    "file": "{file_path}",
    "component": "{comp_type}",
    "critical_count": <int>,
    "warning_count": <int>
  }},
  "findings": [
    {{
      "severity": "critical" | "warning",
      "category": "Architecture" | "Security" | "KDoc" | "Quality",
      "file": "{file_path}",
      "line": <line_number_or_0>,
      "title": "<Concise issue title in English>",
      "message": "<Detailed technical defect description in English>",
      "suggestion": "<Concrete Kotlin fix or recommendation in English>"
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

    clean_text = response.text.strip()
    if clean_text.startswith("```json"):
        clean_text = clean_text[7:]
    if clean_text.endswith("```"):
        clean_text = clean_text[:-3]

    try:
        data = json.loads(clean_text.strip())
        return data
    except Exception as e:
        print(f"⚠️ JSON parsing error in unified_reviewer for {file_path}: {e}")
        return {"summary": {"file": file_path, "critical_count": 0, "warning_count": 0}, "findings": []}
