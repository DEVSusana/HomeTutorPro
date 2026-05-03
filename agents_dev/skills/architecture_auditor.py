import json
import config

def run_architecture_audit(file_path, output_format="text"):
    client = config.get_client()

    with open('AGENTS.md', 'r') as f:
        rules = f.read()
    with open(file_path, 'r') as f:
        code = f.read()

    if output_format == "json":
        prompt = f"""
        Act as an Android Architecture Expert.
        Review this Kotlin code based on these rules: {rules}
        
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
        response = client.models.generate_content(
            model=config.MODEL_ID,
            contents=prompt,
            config=types.GenerateContentConfig(response_mime_type="application/json")
        )
        return response.text
    else:
        prompt = f"Review this Kotlin code based on these rules: {rules}\n\nCODE:\n{code}"
        response = client.models.generate_content(
            model=config.MODEL_ID,
            contents=prompt
        )
        return response.text