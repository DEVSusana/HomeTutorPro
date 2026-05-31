"""
Fase 3: Fix Suggester (read-only mode)
Generates fix suggestions for auto-fixable findings and posts them as a PR comment.
Does NOT modify any source files — the developer reviews and applies the suggestions manually.
"""
import json
import sys
import os
import time
from github import Github
import agents_dev_config as config

FIXABLE_CATEGORIES = ["kdoc", "refactor", "testing"]


def create_fix_suggestions():
    if not os.path.exists("agent_findings.json"):
        print("No agent_findings.json found. Nothing to suggest.")
        return

    with open("agent_findings.json", "r") as f:
        findings = json.load(f)

    # Filter only auto-fixable findings
    fixable_findings = [f for f in findings if f.get("category", "").lower() in FIXABLE_CATEGORIES]

    if not fixable_findings:
        print("No auto-fixable findings to suggest.")
        return

    # Group by file
    findings_by_file = {}
    for f in fixable_findings:
        file = f.get("file")
        if file and os.path.exists(file):
            if file not in findings_by_file:
                findings_by_file[file] = []
            findings_by_file[file].append(f)

    if not findings_by_file:
        print("No valid files for fix suggestions.")
        return

    client = config.get_client()
    with open("AGENTS.md", "r") as f:
        rules = f.read()

    suggestions_by_file = {}

    for file_path, file_findings in findings_by_file.items():
        print(f"Generating fix suggestion for {file_path}...")

        with open(file_path, "r") as f:
            code = f.read()

        findings_text = json.dumps(file_findings, indent=2)

        prompt = f"""
        Act as an Expert Kotlin Android Developer.
        Based on the following issues and the project rules, suggest the MINIMAL code changes needed.
        
        RULES:
        {rules}
        
        ISSUES TO FIX:
        {findings_text}
        
        ORIGINAL CODE:
        ```kotlin
        {code}
        ```
        
        IMPORTANT INSTRUCTIONS:
        1. Return a JSON object with a "diff_summary" field (plain text explanation of changes) 
           and a "suggested_code" field with the complete corrected Kotlin code.
        2. The suggested_code must be valid Kotlin — do not include markdown fences.
        3. Keep changes minimal. Only fix the reported issues, do not refactor unrelated code.
        
        Return JSON: {{"diff_summary": "...", "suggested_code": "..."}}
        """

        try:
            from google.genai import types
            response = client.models.generate_content(
                model=config.MODEL_ID,
                contents=prompt,
                config=types.GenerateContentConfig(response_mime_type="application/json")
            )

            raw = response.text.strip()
            suggestion_data = json.loads(raw)
            suggestions_by_file[file_path] = {
                "findings": file_findings,
                "diff_summary": suggestion_data.get("diff_summary", ""),
                "suggested_code": suggestion_data.get("suggested_code", ""),
            }
            print(f"  Suggestion generated for {file_path}")

        except Exception as e:
            print(f"  Error generating suggestion for {file_path}: {e}")

        time.sleep(5)

    if suggestions_by_file:
        post_suggestions_comment(suggestions_by_file)


def post_suggestions_comment(suggestions_by_file: dict):
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path:
        print("GITHUB_EVENT_PATH not set. Cannot post comment.")
        return

    with open(event_path, "r") as f:
        event_data = json.load(f)

    if "pull_request" not in event_data:
        return

    pr_number = event_data["pull_request"]["number"]
    repo_name = os.environ.get("GITHUB_REPOSITORY")
    github_token = os.environ.get("GITHUB_TOKEN")

    g = Github(github_token)
    repo = g.get_repo(repo_name)
    pr = repo.get_pull(pr_number)

    comment_body = "## 🤖 Agent Fix Suggestions\n\n"
    comment_body += "> ℹ️ These are **suggestions only** — no files were modified automatically.\n"
    comment_body += "> Review each suggestion and apply manually if it looks correct.\n\n"

    for file_path, data in suggestions_by_file.items():
        file_name = os.path.basename(file_path)
        comment_body += f"### 📄 `{file_name}`\n\n"

        comment_body += "**Issues addressed:**\n"
        for finding in data["findings"]:
            sev = finding.get("severity", "info")
            emoji = {"critical": "🔴", "warning": "🟡", "info": "🟢"}.get(sev, "🟢")
            comment_body += f"- {emoji} {finding.get('message', '')}\n"

        comment_body += f"\n**What to change:** {data['diff_summary']}\n\n"

        if data["suggested_code"]:
            comment_body += "<details>\n<summary>📋 Suggested code (click to expand)</summary>\n\n"
            comment_body += f"```kotlin\n{data['suggested_code']}\n```\n\n"
            comment_body += "</details>\n\n"

    pr.create_issue_comment(comment_body)
    print("Fix suggestions comment posted to PR.")


if __name__ == "__main__":
    create_fix_suggestions()
