import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from core.orchestrator import FeatureOrchestrator


class TestFeatureOrchestrator(unittest.TestCase):
    def test_is_spec_approved_and_derive_slug(self):
        orch = FeatureOrchestrator(provider=MagicMock())
        self.assertTrue(orch._is_spec_approved("# SPEC: Foo\n- **Estado:** APPROVED"))
        self.assertFalse(orch._is_spec_approved("# SPEC: Foo\n- **Estado:** DRAFT"))
        self.assertEqual(orch._derive_slug(Path("specs/Payment Badge!.md")), "payment-badge")

    def test_run_returns_false_when_spec_missing(self):
        orch = FeatureOrchestrator(provider=MagicMock())
        self.assertFalse(orch.run("specs/does_not_exist_123.md", use_worktree=False, auto_confirm=True))

    @patch("core.orchestrator.AgenticEngine")
    def test_run_executes_engine_without_worktree(self, mock_engine_cls):
        mock_engine = MagicMock()
        mock_engine.run.return_value = True
        mock_engine_cls.return_value = mock_engine

        with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False) as tmp:
            tmp.write("# SPEC: Test\n- **Estado:** APPROVED\n")
            tmp_path = tmp.name

        try:
            orch = FeatureOrchestrator(provider=MagicMock(), max_iterations=10)
            ok = orch.run(tmp_path, use_worktree=False, create_pr=False, auto_confirm=True)
            self.assertTrue(ok)
            mock_engine.run.assert_called_once()
        finally:
            Path(tmp_path).unlink(missing_ok=True)


if __name__ == "__main__":
    unittest.main()
