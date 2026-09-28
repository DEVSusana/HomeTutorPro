import json
import config

def run_test_design(file_path, output_format="text"):
    client = config.get_client()
    with open('AGENTS.md', 'r') as f: rules = f.read()
    with open(file_path, 'r') as f: code = f.read()

    if output_format == "json":
        prompt = f"""
        Act as a Senior QA Engineer. 
        Based on the project rules in AGENTS.md, create a Unit Test plan.
        
        RULES:
        {rules}
        
        CODE TO TEST:
        {code}
        
        Return a JSON object matching this schema:
        {{
          "findings": [
            {{
              "severity": "critical" | "warning" | "info",
              "category": "testing",
              "file": "{file_path}",
              "line": 0,
              "message": "<description of missing tests or test improvements>",
              "suggestion": "<test plan or test case suggestion>"
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
        Act as a Senior QA Engineer. 
        Based on the project rules in AGENTS.md, create a Unit Test plan.
        
        RULES:
        {rules}
        
        CODE TO TEST:
        {code}
        """
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt
        )
        return response.text