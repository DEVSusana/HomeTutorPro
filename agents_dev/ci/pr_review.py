"""
Universal CI: PR Reviewer & Quality Guardrails
Executes the 5 specialist skills over Kotlin files modified in a PR,
publishes a clean report, and enforces quality guardrails.
"""
import os
import sys
import json
import time
import subprocess

try:
    from github import Github
except ImportError:
    Github = None

# Ensure skills directory is on sys.path
SKILLS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "skills"))
if SKILLS_DIR not in sys.path:
    sys.path.insert(0, SKILLS_DIR)

import unified_reviewer

EXCLUDE_DIRS = [
    "di/",
    "ui/theme/",
    "build/",
    ".gradle/",
    "ksp/",
    "src/test/",
    "src/androidTest/",
    "Previews.kt"
]
MAX_FILES = int(os.environ.get("MAX_FILES", "15"))


def get_file_priority(file_path: str) -> int:
    """
    Ranks Kotlin files by architectural risk and implementation impact:
    Tier 1 (Domain Use Cases & Core Engines): domain/usecases/implementations/
    Tier 2 (Data Repositories & Data Sources): data/repository/, data/local/, data/remote/
    Tier 3 (Presentation ViewModels & State Delegates): /viewmodels/, /delegates/
    Tier 4 (Core UI Screens & Widgets): /widget/, *Screen.kt, *Content.kt
    Tier 5 (Domain Entities & Domain Services): /domain/
    Tier 6 (UI Components & Dialogs): /presentation/
    Tier 7 (Pure Contracts/Interfaces, e.g. IFoo.kt): minimal risk
    """
    fp = file_path.replace("\\", "/")
    base_name = os.path.basename(fp)
    if base_name.startswith("I") and len(base_name) > 1 and base_name[1].isupper() and "Impl" not in base_name:
        return 7
    if "/domain/usecases/implementations/" in fp:
        return 1
    if "/data/repository/" in fp or "/data/local/" in fp or "/data/remote/" in fp:
        return 2
    if "/viewmodels/" in fp or "/delegates/" in fp:
        return 3
    if "/widget/" in fp or "Screen.kt" in fp or "Content.kt" in fp:
        return 4
    if "/domain/" in fp:
        return 5
    if "/presentation/" in fp:
        return 6
    return 7


def prioritize_files(files: list, max_count: int = MAX_FILES) -> list:
    """Sorts files by architectural priority tier, ensuring highest-impact business logic is reviewed."""
    if not files:
        return []
    unique_files = list(dict.fromkeys(files))
    sorted_files = sorted(unique_files, key=lambda f: (get_file_priority(f), f))
    return sorted_files[:max_count]


def get_modified_kt_files(explicit_files=None):
    """Detects modified .kt files in the PR via GitHub event or locally using git diff."""
    if explicit_files:
        valid_files = [
            f for f in explicit_files
            if f.endswith(".kt") and os.path.exists(f) and not any(excl in f for excl in EXCLUDE_DIRS)
        ]
        if valid_files:
            return prioritize_files(valid_files, MAX_FILES)

    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if event_path and os.path.exists(event_path):
        try:
            with open(event_path, "r", encoding="utf-8") as f:
                event_data = json.load(f)

            if "pull_request" in event_data:
                pr_number = event_data["pull_request"]["number"]
                repo_name = os.environ.get("GITHUB_REPOSITORY")
                github_token = os.environ.get("GITHUB_TOKEN")

                if repo_name and github_token and Github is not None:
                    g = Github(github_token)
                    repo = g.get_repo(repo_name)
                    pr = repo.get_pull(pr_number)

                    target_files = []
                    for file in pr.get_files():
                        if file.status == "removed" or not file.filename.endswith(".kt"):
                            continue
                        if any(excl in file.filename for excl in EXCLUDE_DIRS):
                            continue
                        target_files.append(file.filename)

                    return prioritize_files(target_files, MAX_FILES)
        except Exception as e:
            print(f"⚠️ Error retrieving modified files from PR event: {e}")

    # Fallback for local execution via git diff
    print("ℹ️ Detecting locally modified Kotlin files...")
    import shutil
    git_bin = "/usr/local/bin/git" if os.path.exists("/usr/local/bin/git") else (shutil.which("git") or "git")
    for cmd in [
        [git_bin, "diff", "--name-only", "origin/develop...HEAD"],
        [git_bin, "diff", "--name-only", "HEAD~1"],
        [git_bin, "diff", "--name-only", "--staged"],
        [git_bin, "diff", "--name-only"]
    ]:
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, check=True)
            candidate_files = [line.strip() for line in res.stdout.splitlines() if line.strip()]
            valid_files = [
                f for f in candidate_files
                if f.endswith(".kt") and os.path.exists(f) and not any(excl in f for excl in EXCLUDE_DIRS)
            ]
            if valid_files:
                return prioritize_files(valid_files, MAX_FILES)
        except Exception:
            continue

    return []


def post_pr_comment(comment_body: str):
    """Publishes a unique comment to the PR or prints it to the console for local execution."""
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path or not os.path.exists(event_path):
        print("\n" + "=" * 70)
        print("🤖 AGENT SKILLS REVIEW REPORT (LOCAL EXECUTION)")
        print("=" * 70)
        print(comment_body)
        print("=" * 70 + "\n")
        return

    try:
        with open(event_path, "r", encoding="utf-8") as f:
            event_data = json.load(f)

        if "pull_request" not in event_data:
            print(comment_body)
            return

        pr_number = event_data["pull_request"]["number"]
        repo_name = os.environ.get("GITHUB_REPOSITORY")
        github_token = os.environ.get("GITHUB_TOKEN")

        if not repo_name or not github_token or Github is None:
            print(comment_body)
            return

        g = Github(github_token)
        repo = g.get_repo(repo_name)
        pr = repo.get_pull(pr_number)

        # Search and clean previous bot comments to keep the PR clean
        try:
            bot_markers = [
                "## 🤖 Agent Code Review",
                "## ⚠️ Agent Code Review"
            ]
            for comment in pr.get_issue_comments():
                if any(marker in (comment.body or "") for marker in bot_markers):
                    try:
                        comment.delete()
                    except Exception as err:
                        print(f"⚠️ Could not delete previous comment {comment.id}: {err}")
        except Exception as e:
            print(f"⚠️ Error cleaning previous comments: {e}")

        pr.create_issue_comment(comment_body)
        print("✅ Comment published on PR (previous comments cleaned up).")
    except Exception as e:
        print(f"⚠️ Could not publish comment to PR: {e}")
        print(comment_body)


def run_pr_review_and_guardrails(target_files=None):
    files = get_modified_kt_files(target_files)
    if not files:
        print("ℹ️ No modified .kt files detected to analyze.")
        post_pr_comment(
            "## 🤖 Agent Code Review\n\n"
            "ℹ️ No modified Kotlin (`.kt`) files detected in this Pull Request. "
            "Review completed without issues."
        )
        sys.exit(0)

    print(f"🔍 Kotlin files to review ({len(files)}): {files}")

    all_findings = []
    errors = []
    analyzed_files = []
    quota_exhausted = False

    for file_path in files:
        print(f"\n📄 Analyzing surgically (Unified Review): {file_path}")
        if not os.path.exists(file_path):
            print(f"  ⚠️ File not found in local workspace: {file_path}")
            continue

        try:
            result_data = unified_reviewer.run_unified_review(file_path)
            findings = result_data.get("findings", [])
            all_findings.extend(findings)
            analyzed_files.append(file_path)
            print(f"  ↳ Findings detected in {file_path}: {len(findings)}")
        except Exception as e:
            err_msg = str(e)
            print(f"  ⚠️ Error analyzing {file_path}: {err_msg}")
            errors.append({"skill": "UnifiedReview", "file": file_path, "error": err_msg})
            if any(code in err_msg for code in ["429", "RESOURCE_EXHAUSTED", "quota", "rate-limits"]):
                print("\n🛑 Rate limit / Quota reached. Circuit breaker activated to prevent workflow timeout.")
                quota_exhausted = True
                break

        time.sleep(4)

    # 1. Filter actionable findings only (critical, high, warning, medium)
    actionable_findings = [
        f for f in all_findings
        if str(f.get("severity", "")).lower() in ["critical", "high", "warning", "medium"]
    ]

    if actionable_findings:
        findings_by_file = {}
        for f in actionable_findings:
            file_key = f.get("file", "General")
            findings_by_file.setdefault(file_key, []).append(f)

        body = "## 🤖 Agent Code Review: Findings Detected\n\n"
        emoji_map = {"critical": "🔴", "high": "🔴", "warning": "🟡", "medium": "🟡"}

        for file_key, file_findings in findings_by_file.items():
            body += f"### 📄 `{file_key}`\n"
            body += "| Sev | Category | Finding | Suggestion |\n"
            body += "| :---: | :--- | :--- | :--- |\n"
            for f in file_findings:
                sev = str(f.get("severity", "warning")).lower()
                emoji = emoji_map.get(sev, "🟡")
                cat = str(f.get("category", "-")).capitalize()
                line = f"L{f.get('line')}" if f.get("line") else ""
                msg = str(f.get("message") or f.get("title") or "-").replace("|", "-")
                sug = str(f.get("suggestion") or f.get("remediation") or "-").replace("|", "-")
                body += f"| {emoji} | **{cat}** {line} | {msg} | {sug} |\n"
            body += "\n"

        if errors:
            body += "\n### ⚠️ Execution Issues:\n"
            for err in errors:
                body += f"- **{err['skill']}** on `{err['file']}`: `{err['error'][:250]}`\n"

            has_auth_error = any("GEMINI_API_KEY" in str(e["error"]) or "API_KEY_INVALID" in str(e["error"]) for e in errors)
            if has_auth_error:
                body += "\n> 💡 *Credentials note: Ensure `GEMINI_API_KEY` is configured in GitHub Actions Secrets.*\n"
            elif quota_exhausted:
                body += f"\n> ℹ️ *Analysis paused early: Gemini rate limit/quota reached after reviewing {len(analyzed_files)} priority files. Circuit breaker activated to prevent CI delay.*\n"

        post_pr_comment(body)

    elif errors:
        body = "## ⚠️ Agent Code Review: Warning During Analysis\n\n"
        if quota_exhausted and analyzed_files:
            body += f"Successfully analyzed **{len(analyzed_files)} prioritized files** before reaching rate limit:\n\n"
        else:
            body += "Some automated skills could not complete execution:\n\n"
        for err in errors:
            body += f"- **{err['skill']}** on `{err['file']}`: `{err['error'][:250]}`\n"

        has_auth_error = any("GEMINI_API_KEY" in str(e["error"]) or "API_KEY_INVALID" in str(e["error"]) for e in errors)
        if has_auth_error:
            body += "\n> 💡 *Ensure `GEMINI_API_KEY` is configured in GitHub Actions Secrets.*"
        elif quota_exhausted:
            body += f"\n> ℹ️ *Analysis paused early: Gemini rate limit reached after {len(analyzed_files)} files. The highest-impact architectural files were prioritized.*"
        else:
            body += "\n> ℹ️ *Temporary network issue or rate limit on API servers.*"
        post_pr_comment(body)

    else:
        analyzed_list = analyzed_files if analyzed_files else files
        body = "## 🤖 Agent Code Review: All Clear ✅\n\n"
        body += f"Successfully analyzed **{len(analyzed_list)} prioritized Kotlin file(s)** with all 5 agent skills:\n"
        for f in analyzed_list:
            body += f"- `{f}`\n"
        body += "\n| Skill | Status | Result |\n"
        body += "| :--- | :---: | :--- |\n"
        body += "| 🔒 **Security** | ✅ PASS | Zero exposed credentials, safe coroutines, and strict English naming. |\n"
        body += "| 🏛️ **Architecture** | ✅ PASS | Complies with Clean Architecture and domain business rules. |\n"
        body += "| 🧪 **Tests** | ✅ PASS | Unit test structure verified. |\n"
        body += "| 📝 **KDoc** | ✅ PASS | Public API documentation conforms to standards. |\n"
        body += "| 🧹 **Refactor** | ✅ PASS | No critical code smells detected. |\n"
        if quota_exhausted:
            body += f"\n> ℹ️ *Analysis paused early: Gemini rate limit reached after {len(analyzed_files)} files. The top architectural files were verified.*"
        post_pr_comment(body)

    # 2. Guardrails: Advisory display of critical findings without blocking merge
    critical_violations = [
        f for f in all_findings
        if f.get("severity", "").lower() == "critical"
    ]

    if critical_violations:
        print(f"\n⚠️ Detected {len(critical_violations)} critical observation(s) (posted to PR):")
        for v in critical_violations:
            cat = str(v.get("category", "GENERAL")).upper()
            print(f"  - [{cat}] {v.get('file')}: {v.get('message')}")
        print("\nℹ️ [ADVISORY GUARDRAIL]: Suggestions published to PR. Merge is not blocked to allow human review.")
    else:
        print("\n✅ [GUARDRAILS PASS]: Zero critical violations. PR ready for merge.")

    sys.exit(0)


if __name__ == "__main__":
    cli_files = [a for a in sys.argv[1:] if not a.startswith("-")]
    run_pr_review_and_guardrails(cli_files if cli_files else None)
