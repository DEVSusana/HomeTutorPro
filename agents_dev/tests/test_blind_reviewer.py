import unittest
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from core.blind_reviewer import BlindReviewer


class TestBlindReviewer(unittest.TestCase):
    def test_review_passes_with_isolated_fresh_context_and_no_tools(self):
        mock_provider = MagicMock()
        mock_provider.chat.return_value = SimpleNamespace(
            content="VERDICT: PASS\n- **Changed Files Check:** In scope\n- **Findings / Required Fixes:** None"
        )

        reviewer = BlindReviewer(provider=mock_provider)
        result = reviewer.review(
            goal="Add payment warning badge",
            spec_content="# SPEC: Badge\n- [ ] **AC1:** Show badge when pendingBalance > 0",
            git_diff="diff --git a/Foo.kt b/Foo.kt\n+val showBadge = pendingBalance > 0",
            changed_files=["app/src/main/java/Foo.kt"],
            verification_commands=[],
        )

        self.assertTrue(result.passed)
        self.assertEqual(result.verdict, "PASS")
        self.assertEqual(result.changed_files, ["app/src/main/java/Foo.kt"])

        mock_provider.chat.assert_called_once()
        call_kwargs = mock_provider.chat.call_args.kwargs
        self.assertEqual(call_kwargs["tools"], [])
        self.assertEqual(len(call_kwargs["messages"]), 1)
        self.assertEqual(call_kwargs["messages"][0]["role"], "user")

    def test_review_fails_when_verdict_is_fail(self):
        mock_provider = MagicMock()
        mock_provider.chat.return_value = SimpleNamespace(
            content="VERDICT: FAIL\n- **Findings / Required Fixes:** Missing unit test for AC2."
        )

        reviewer = BlindReviewer(provider=mock_provider)
        result = reviewer.review(
            goal="Add payment warning badge",
            spec_content="# SPEC: Badge",
            git_diff="diff --git a/Foo.kt b/Foo.kt\n+val x = 1",
            changed_files=["app/src/main/java/Foo.kt"],
            verification_commands=[],
        )

        self.assertFalse(result.passed)
        self.assertEqual(result.verdict, "FAIL")
        self.assertIn("Missing unit test", result.report)

    def test_review_fails_when_no_changes_detected(self):
        mock_provider = MagicMock()
        reviewer = BlindReviewer(provider=mock_provider)

        result = reviewer.review(
            goal="Add payment warning badge",
            spec_content="# SPEC: Badge",
            git_diff="",
            changed_files=[],
        )

        self.assertFalse(result.passed)
        self.assertEqual(result.verdict, "FAIL")
        mock_provider.chat.assert_not_called()

    def test_scope_violation_fails_without_calling_llm(self):
        mock_provider = MagicMock()
        reviewer = BlindReviewer(provider=mock_provider)
        spec = (
            "# SPEC: Badge\n\n"
            "## 2. Alcance (Scope) — Archivos permitidos\n"
            "- **Modificar:**\n"
            "  - `app/src/main/java/StudentCard.kt`\n"
            "- **PROHIBIDO modificar:**\n"
            "  - `AGENTS.md`\n"
        )

        result = reviewer.review(
            goal="Add badge",
            spec_content=spec,
            git_diff="diff --git a/AGENTS.md b/AGENTS.md",
            changed_files=["app/src/main/java/StudentCard.kt", "AGENTS.md"],
            verification_commands=[],
        )

        self.assertFalse(result.passed)
        self.assertIn("SCOPE VIOLATION", result.report)
        self.assertIn("AGENTS.md", result.report)
        mock_provider.chat.assert_not_called()

    @patch("core.blind_reviewer.subprocess.run")
    def test_verification_command_failure_fails_without_calling_llm(self, mock_run):
        mock_run.return_value = SimpleNamespace(returncode=1, stdout="", stderr="Compilation failed")
        mock_provider = MagicMock()
        reviewer = BlindReviewer(provider=mock_provider)
        spec = (
            "# SPEC: Badge\n\n"
            "## 6. Comandos de verificación obligatorios\n"
            "```bash\n"
            "./gradlew compileDebugKotlin\n"
            "```\n"
        )

        result = reviewer.review(
            goal="Add badge",
            spec_content=spec,
            git_diff="diff --git a/Foo.kt b/Foo.kt\n+val x = 1",
            changed_files=["Foo.kt"],
        )

        self.assertFalse(result.passed)
        self.assertIn("Deterministic checks FAILED", result.report)
        mock_run.assert_called_once()
        mock_provider.chat.assert_not_called()


if __name__ == "__main__":
    unittest.main()
