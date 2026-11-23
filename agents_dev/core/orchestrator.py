import os
import re
import shutil
import subprocess
from pathlib import Path
from typing import Optional

from core.engine import AgenticEngine
from core.system_tools import PROJECT_ROOT
from providers.base import BaseLLMProvider


class FeatureOrchestrator:
    """
    End-to-end feature orchestrator (Bloque 6):
    spec (APPROVED) -> isolated git worktree -> AgenticEngine (Builder + BlindReviewer)
    -> optional human-approved GitHub PR creation.
    """

    def __init__(self, provider: BaseLLMProvider, max_iterations: int = 25):
        self.provider = provider
        self.max_iterations = max_iterations

    def _derive_slug(self, spec_path: Path) -> str:
        stem = spec_path.stem.lower()
        return re.sub(r"[^a-z0-9]+", "-", stem).strip("-")[:32] or "feature"

    def _is_spec_approved(self, spec_content: str) -> bool:
        return bool(re.search(r"\*\*Estado:\*\*\s*APPROVED\b", spec_content, re.IGNORECASE))

    def _prepare_worktree(self, slug: str, auto_confirm: bool = False) -> Optional[Path]:
        wt_path = Path(PROJECT_ROOT).parent / f"{Path(PROJECT_ROOT).name}-wt-{slug}"
        if wt_path.exists():
            print(f"ℹ️ Reusing existing worktree: {wt_path}")
            return wt_path

        if not auto_confirm:
            ans = input(f"👉 Create isolated worktree '{wt_path}' on branch 'feat/{slug}'? (y/n) [y]: ").strip().lower()
            if ans not in ("", "y", "yes", "s", "si", "sí"):
                return None

        worktree_script = Path(PROJECT_ROOT) / "scripts" / "worktree.py"
        if not worktree_script.exists():
            print("❌ scripts/worktree.py not found.")
            return None

        proc = subprocess.run(
            ["python3", str(worktree_script), "create", slug],
            cwd=PROJECT_ROOT,
        )
        return wt_path if proc.returncode == 0 else None

    def _create_pr_prompt(self, spec_rel: str, slug: str, wt_path: Path, auto_confirm: bool = False) -> bool:
        if not shutil.which("gh"):
            print("ℹ️ GitHub CLI ('gh') is not installed. Skipping PR creation.")
            return False

        if not auto_confirm:
            ans = input(f"👉 Push branch 'feat/{slug}' and open a GitHub PR with 'gh pr create'? (y/n) [n]: ").strip().lower()
            if ans not in ("y", "yes", "s", "si", "sí"):
                print("ℹ️ PR creation skipped by user.")
                return False

        push_proc = subprocess.run(["git", "push", "-u", "origin", f"feat/{slug}"], cwd=wt_path)
        if push_proc.returncode != 0:
            print("❌ git push failed. Resolve remote/auth issues and retry.")
            return False

        pr_proc = subprocess.run(
            [
                "gh",
                "pr",
                "create",
                "--title",
                f"feat: {slug}",
                "--body",
                f"Automated implementation for `{spec_rel}` verified by Builder + BlindReviewer.",
            ],
            cwd=wt_path,
        )
        return pr_proc.returncode == 0

    def run(
        self,
        spec_file: str,
        slug: Optional[str] = None,
        use_worktree: bool = True,
        create_pr: bool = False,
        auto_confirm: bool = False,
    ) -> bool:
        """Executes the full supervised pipeline from approved spec to optional PR."""
        spec_path = Path(spec_file) if os.path.isabs(spec_file) else Path(PROJECT_ROOT) / spec_file
        if not spec_path.exists():
            print(f"❌ Spec file not found: {spec_path}")
            return False

        spec_content = spec_path.read_text(encoding="utf-8")
        if not self._is_spec_approved(spec_content):
            print("⚠️ Spec is not marked 'Estado: APPROVED'.")
            if not auto_confirm:
                ans = input("👉 Proceed anyway? (y/n) [n]: ").strip().lower()
                if ans not in ("y", "yes", "s", "si", "sí"):
                    return False

        resolved_slug = slug or self._derive_slug(spec_path)
        work_dir = Path(PROJECT_ROOT)

        if use_worktree:
            wt_path = self._prepare_worktree(resolved_slug, auto_confirm=auto_confirm)
            if wt_path is None:
                print("ℹ️ Worktree step cancelled.")
                return False
            work_dir = wt_path

        spec_rel = os.path.relpath(spec_path, PROJECT_ROOT)
        goal = f"Fully implement and verify the specification in '{spec_rel}'."
        engine = AgenticEngine(provider=self.provider, max_iterations=self.max_iterations)

        prev_cwd = os.getcwd()
        try:
            os.chdir(work_dir)
            success = engine.run(goal=goal, spec_file=str(spec_path))
        finally:
            os.chdir(prev_cwd)

        if not success:
            print("❌ Orchestration stopped: implementation was not approved.")
            return False

        if create_pr:
            self._create_pr_prompt(spec_rel, resolved_slug, work_dir, auto_confirm=auto_confirm)

        return True
