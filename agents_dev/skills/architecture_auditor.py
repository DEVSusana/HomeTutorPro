import os
import json
import config

def run_architecture_audit(file_path, output_format="text"):
    client = config.get_client()

    with open('AGENTS.md', 'r') as f:
        rules = f.read()

    business_rules = ""
    try:
        if os.path.exists('BUSINESS_RULES.md'):
            with open('BUSINESS_RULES.md', 'r') as f:
                business_rules = f.read()
    except Exception:
        pass

    with open(file_path, 'r') as f:
        code = f.read()

    if output_format == "json":
        prompt = f"""
        Act as an Android Architecture Expert.
        Review this Kotlin code based on these architectural rules: {rules}
        
        Additionally, verify if the code complies with these business rules if applicable: {business_rules}
        
        CODE:
        {code}
        
        Return a JSON object matching this schema:
        {{
          "findings": [
            {{
              "severity": "critical" | "warning" | "info",
              "category": "architecture",
              "file": "{file_path}",
              "line": <line_number_if_applicable_or_0>,
              "message": "<description of the issue>",
              "suggestion": "<how to fix it>"
            }}
          ]
        }}
        If there are no issues, return an empty list for findings.
        """
        from google.genai import types
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt,
            config_args=types.GenerateContentConfig(response_mime_type="application/json")
        )
        return response.text
    else:
        rules_context = rules
        if business_rules:
            rules_context += f"\n\nBUSINESS RULES:\n{business_rules}"
        prompt = f"Review this Kotlin code based on these rules: {rules_context}\n\nCODE:\n{code}"
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt
        )
        return response.text