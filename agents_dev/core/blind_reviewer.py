import re
import subprocess
from dataclasses import dataclass, field
from typing import List, Optional, Tuple

from core.system_tools import PROJECT_ROOT
from providers.base import BaseLLMProvider

MAX_DIFF_CHARS = 18000
MAX_CMD_OUTPUT_CHARS = 4000


@dataclass
class ReviewResult:
    """Structured verdict returned by the independent Blind Reviewer."""

    passed: bool
    verdict: str
    report: str
    changed_files: List[str] = field(default_factory=list)


class BlindReviewer:
    """
    Independent, read-only Blind Reviewer agent.
    Evaluates the Builder's git diff against the approved spec in a completely fresh
    context (no Builder reasoning history and no file-editing tools), after running
    deterministic scope and verification-command checks.
    """

    def __init__(self, provider: BaseLLMProvider):
        self.provider = provider

    def _get_changed_files(self) -> List[str]:
        """Returns modified, added, or untracked relative file paths from git status."""
        try:
            output = subprocess.check_output(
                ["git", "status", "--porcelain"],
                cwd=PROJECT_ROOT,
                stderr=subprocess.DEVNULL,
                text=True,
            )
            files = []
            for line in output.splitlines():
                if line.strip() and len(line) >= 4:
                    files.append(line[3:].strip().strip('"'))
            return files
        except Exception:
            return []

    def _get_git_diff(self) -> str:
        """Captures current working-tree and staged diff against HEAD."""
        try:
            diff = subprocess.check_output(
                ["git", "diff", "HEAD"],
                cwd=PROJECT_ROOT,
                stderr=subprocess.DEVNULL,
                text=True,
            )
            if len(diff) > MAX_DIFF_CHARS:
                return diff[:MAX_DIFF_CHARS] + "\n\n... [DIFF TRUNCATED DUE TO SIZE] ..."
            return diff
        except Exception as e:
            return f"ERROR capturing git diff: {e}"

    def _extract_allowed_scope_paths(self, spec_content: Optional[str]) -> List[str]:
        """
        Extracts allowed path prefixes/files from the 'Modificar:' and 'Crear:' subsections
        inside '## 2. Alcance (Scope)' of a spec contract.
        """
        if not spec_content:
            return []

        scope_match = re.search(
            r"##\s*2\.\s*Alcance.*?(?=\n##\s*\d+\.|\Z)",
            spec_content,
            re.DOTALL | re.IGNORECASE,
        )
        if not scope_match:
            return []

        scope_block = scope_match.group(0)
        # Stop before 'PROHIBIDO' so forbidden paths are never treated as allowed
        allowed_block = re.split(r"PROHIBIDO", scope_block, maxsplit=1, flags=re.IGNORECASE)[0]
        raw_paths = re.findall(r"`([^`]+)`", allowed_block)

        cleaned = []
        for path in raw_paths:
            p = path.strip()
            if p and "..." not in p and "<" not in p:
                cleaned.append(p.rstrip("*"))
        return cleaned

    def _check_scope_violations(self, changed_files: List[str], allowed_paths: List[str]) -> List[str]:
        """Returns changed files that fall outside the spec's allowed path prefixes."""
        if not allowed_paths:
            return []

        violations = []
        for changed in changed_files:
            if any(changed == allowed or changed.startswith(allowed) for allowed in allowed_paths):
                continue
            violations.append(changed)
        return violations

    def _extract_verification_commands(self, spec_content: Optional[str]) -> List[str]:
        """Extracts shell commands from the '## 6. Comandos de verificación' code block."""
        if not spec_content:
            return []

        section_match = re.search(
            r"##\s*6\.\s*Comandos de verificaci[oó]n.*?(?=\n##\s*\d+\.|\Z)",
            spec_content,
            re.DOTALL | re.IGNORECASE,
        )
        if not section_match:
            return []

        code_match = re.search(r"```(?:bash|sh)?\n(.*?)```", section_match.group(0), re.DOTALL)
        if not code_match:
            return []

        commands = []
        for line in code_match.group(1).splitlines():
            cmd = line.strip()
            if cmd and not cmd.startswith("#") and "<" not in cmd:
                commands.append(cmd)
        return commands

    def _run_verification_commands(self, commands: List[str]) -> Tuple[bool, str]:
        """Runs verification commands deterministically and returns (all_passed, formatted_log)."""
        if not commands:
            return True, "No deterministic verification commands specified in spec."

        logs = []
        for cmd in commands:
            try:
                proc = subprocess.run(
                    cmd,
                    shell=True,
                    cwd=PROJECT_ROOT,
                    capture_output=True,
                    text=True,
                    timeout=300,
                )
                combined = ((proc.stdout or "") + "\n" + (proc.stderr or "")).strip()
                if len(combined) > MAX_CMD_OUTPUT_CHARS:
                    combined = "... [OUTPUT TRUNCATED] ...\n" + combined[-MAX_CMD_OUTPUT_CHARS:]
                logs.append(f"$ {cmd} -> exit {proc.returncode}\n{combined}")
                if proc.returncode != 0:
                    return False, "\n\n".join(logs)
            except Exception as e:
                logs.append(f"$ {cmd} -> ERROR: {e}")
                return False, "\n\n".join(logs)

        return True, "\n\n".join(logs)

    def _build_system_prompt(self) -> str:
        return """You are an independent, strict Staff Android Reviewer (Blind Reviewer).
You did NOT write this code and you have NO access to the Builder's reasoning.
Your ONLY job is to verify whether the actual git diff strictly satisfies the specification and engineering standards.

### REVIEW RULES (NON-NEGOTIABLE):
1. Check every Acceptance Criterion (AC) in the spec against the actual diff. If an AC is missing or only partially implemented, verdict MUST be FAIL.
2. Check Scope compliance: if the spec lists allowed/forbidden files, flag any unauthorized file modifications as FAIL.
3. Check Clean Architecture & Safety: flag Android/AndroidX imports in `domain/`, `GlobalScope`, hardcoded secrets, or missing unit tests for new logic.
4. Do NOT be lenient. Never assume something works if it is not visible in the diff.

### MANDATORY OUTPUT FORMAT:
Start your response with EXACTLY one of these two lines:
VERDICT: PASS
or
VERDICT: FAIL

Then provide:
- **Changed Files Check:** (in scope / out of scope)
- **Acceptance Criteria Check:** (status per AC)
- **Findings / Required Fixes:** (concrete list if FAIL, or "None" if PASS)
"""

    def review(
        self,
        goal: str,
        spec_content: Optional[str] = None,
        git_diff: Optional[str] = None,
        changed_files: Optional[List[str]] = None,
        verification_commands: Optional[List[str]] = None,
    ) -> ReviewResult:
        """
        Runs deterministic scope & verification checks, then an isolated zero-tool LLM review.
        """
        files = changed_files if changed_files is not None else self._get_changed_files()
        diff = git_diff if git_diff is not None else self._get_git_diff()

        if not files and not diff.strip():
            return ReviewResult(
                passed=False,
                verdict="FAIL",
                report="VERDICT: FAIL\n\nNo modified or added files were detected in the workspace.",
                changed_files=[],
            )

        # 1. Deterministic Scope Check
        allowed_paths = self._extract_allowed_scope_paths(spec_content)
        violations = self._check_scope_violations(files, allowed_paths)
        if violations:
            violation_list = "\n".join(f"  - {v}" for v in violations)
            return ReviewResult(
                passed=False,
                verdict="FAIL",
                report=(
                    "VERDICT: FAIL\n\n"
                    "- **Changed Files Check:** SCOPE VIOLATION detected outside spec.\n"
                    f"- **Out-of-scope files:**\n{violation_list}\n"
                    "- **Findings / Required Fixes:** Revert or remove changes outside the approved Scope."
                ),
                changed_files=files,
            )

        # 2. Deterministic Verification Commands
        cmds = (
            verification_commands
            if verification_commands is not None
            else self._extract_verification_commands(spec_content)
        )
        cmds_passed, cmds_log = self._run_verification_commands(cmds)
        if not cmds_passed:
            return ReviewResult(
                passed=False,
                verdict="FAIL",
                report=(
                    "VERDICT: FAIL\n\n"
                    "- **Verification Commands:** Deterministic checks FAILED.\n"
                    f"```\n{cmds_log}\n```"
                ),
                changed_files=files,
            )

        spec_section = spec_content if spec_content else f"Direct Goal (no formal spec file): {goal}"
        files_list = "\n".join(f"- {f}" for f in files) if files else "- (none detected)"

        user_prompt = (
            f"### SPECIFICATION / GOAL:\n{spec_section}\n\n"
            f"### CHANGED FILES ({len(files)}):\n{files_list}\n\n"
            f"### DETERMINISTIC VERIFICATION OUTPUT:\n{cmds_log}\n\n"
            f"### GIT DIFF:\n```diff\n{diff}\n```\n"
        )

        response = self.provider.chat(
            messages=[{"role": "user", "content": user_prompt}],
            tools=[],
            system_prompt=self._build_system_prompt(),
        )

        report = (response.content or "").strip()
        first_line = report.splitlines()[0].strip().upper() if report else ""
        passed = "VERDICT: PASS" in first_line

        return ReviewResult(
            passed=passed,
            verdict="PASS" if passed else "FAIL",
            report=report,
            changed_files=files,
        )
