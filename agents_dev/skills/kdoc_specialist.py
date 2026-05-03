import json
import config

def run_kdoc_audit(file_path, output_format="text"):
    client = config.get_client()
    with open('AGENTS.md', 'r') as f: rules = f.read()
    with open(file_path, 'r') as f: code = f.read()

    if output_format == "json":
        prompt = f"""
        Act as a Technical Writer. 
        Review the documentation style required in AGENTS.md and identify missing KDocs.
        
        RULES:
        {rules}
        
        CODE:
        {code}
        
        Return a JSON object matching this schema:
        {{
          "findings": [
            {{
              "severity": "critical" | "warning" | "info",
              "category": "kdoc",
              "file": "{file_path}",
              "line": <line_number_if_applicable_or_0>,
              "message": "<description of missing KDoc or bad documentation>",
              "suggestion": "<the generated KDoc or suggestion>"
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
        prompt = f"""
        Act as a Technical Writer. 
        Review the documentation style required in AGENTS.md and identify missing KDocs.
        
        RULES:
        {rules}
        
        CODE:
        {code}
        """
        response = client.models.generate_content(model=config.MODEL_ID, contents=prompt)
        return response.text