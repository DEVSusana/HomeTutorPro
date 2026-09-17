import json
import config

def run_security_scan(file_path, output_format="text"):
    client = config.get_client()

    with open(file_path, 'r') as f:
        code = f.read()

    if output_format == "json":
        prompt = f"""
        Act as an Android Security Expert.
        Scan this code for:
        1. Hardcoded strings/keys.
        2. Unsafe Coroutine usage.
        3. Proper English naming (as per AGENTS.md requirements).
        
        CODE:
        {code}
        
        Return a JSON object matching this schema:
        {{
          "findings": [
            {{
              "severity": "critical" | "warning" | "info",
              "category": "security",
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
        prompt = f"""
        Act as an Android Security Expert.
        Scan this code for:
        1. Hardcoded strings/keys.
        2. Unsafe Coroutine usage.
        3. Proper English naming (as per AGENTS.md requirements).
        
        CODE:
        {code}
        """
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt
        )
        return response.text