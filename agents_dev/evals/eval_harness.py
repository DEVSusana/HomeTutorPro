#!/usr/bin/env python3
"""
Deterministic Eval Runner for HomeTutorPro's agentic harness.
Measures whether BlindReviewer + Scope Enforcement + Verification Gates
produce the expected PASS/FAIL and LLM-call behavior across frozen benchmark cases.
"""

import json
import sys
from pathlib import Path
from types import SimpleNamespace
from typing import Any, Dict, List
from unittest.mock import MagicMock, patch

AGENTS_DEV_DIR = Path(__file__).resolve().parent.parent
if str(AGENTS_DEV_DIR) not in sys.path:
    sys.path.insert(0, str(AGENTS_DEV_DIR))

from core.blind_reviewer import BlindReviewer  # noqa: E402

DEFAULT_CASES_PATH = Path(__file__).resolve().parent / "cases.json"


def run_eval_suite(cases_path: Path = DEFAULT_CASES_PATH) -> Dict[str, Any]:
    """Executes all eval cases and returns a structured scorecard."""
    cases: List[Dict[str, Any]] = json.loads(cases_path.read_text(encoding="utf-8"))
    results = []

    for case in cases:
        mock_provider = MagicMock()
        mock_provider.chat.return_value = SimpleNamespace(content=case["mock_llm_verdict"])
        reviewer = BlindReviewer(provider=mock_provider)

        exit_code = case.get("mock_cmd_exit_code", 0)
        with patch("core.blind_reviewer.subprocess.run") as mock_run:
            mock_run.return_value = SimpleNamespace(
                returncode=exit_code,
                stdout="BUILD SUCCESSFUL" if exit_code == 0 else "",
                stderr="" if exit_code == 0 else "Compilation failed",
            )
            review_result = reviewer.review(
                goal=case["goal"],
                spec_content=case["spec_content"],
                git_diff=case["git_diff"],
                changed_files=case["changed_files"],
            )

        llm_called = mock_provider.chat.called
        passed_ok = review_result.passed == case["expected_passed"]
        llm_ok = llm_called == case["expected_llm_called"]
        case_ok = passed_ok and llm_ok

        results.append(
            {
                "id": case["id"],
                "ok": case_ok,
                "expected_passed": case["expected_passed"],
                "actual_passed": review_result.passed,
                "expected_llm_called": case["expected_llm_called"],
                "actual_llm_called": llm_called,
            }
        )

    passed_count = sum(1 for r in results if r["ok"])
    total = len(results)
    return {
        "passed": passed_count,
        "total": total,
        "pass_rate": round((passed_count / total) * 100, 1) if total else 0.0,
        "results": results,
    }


def main() -> int:
    scorecard = run_eval_suite()
    print(f"\n=== Harness Eval Scorecard: {scorecard['passed']}/{scorecard['total']} ({scorecard['pass_rate']}%) ===")
    for item in scorecard["results"]:
        status = "PASS" if item["ok"] else "FAIL"
        print(
            f"  [{status}] {item['id']} "
            f"(passed={item['actual_passed']}/{item['expected_passed']}, "
            f"llm_called={item['actual_llm_called']}/{item['expected_llm_called']})"
        )
    return 0 if scorecard["passed"] == scorecard["total"] else 1


if __name__ == "__main__":
    sys.exit(main())
