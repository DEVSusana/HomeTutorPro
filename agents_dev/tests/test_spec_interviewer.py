import unittest
from unittest.mock import MagicMock

from planner.spec_interviewer import SpecInterviewer


class TestSpecInterviewerSlug(unittest.TestCase):
    def test_slug_strips_accents_and_uses_branch_slug_when_present(self):
        interviewer = SpecInterviewer(provider=MagicMock())
        spec = "# SPEC: Añadir badge de aviso\n- **Rama / Worktree:** `feat/payment-warning-badge`\n"
        self.assertEqual(
            interviewer._build_spec_slug(spec, "Añadir un badge de aviso en los alumnos"),
            "payment_warning_badge",
        )

    def test_slug_handles_spanish_accents_and_word_boundary_truncation(self):
        interviewer = SpecInterviewer(provider=MagicMock())
        slug = interviewer._build_spec_slug(
            "",
            "Añadir un badge de aviso en los alumnos con pagos pendientes",
        )
        self.assertTrue(slug.startswith("anadir_un_badge"))
        self.assertFalse(slug.endswith("_"))
        self.assertLessEqual(len(slug), 45)


if __name__ == "__main__":
    unittest.main()
