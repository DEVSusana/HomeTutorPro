#!/usr/bin/env python3
"""
Helper for managing parallel Git worktrees in HomeTutorPro.
Creates sibling worktrees (../HomeTutorPro-wt-<slug>) and copies ignored local
config files (e.g. local.properties) required by Android/Gradle builds.
"""

import argparse
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
LOCAL_FILES_TO_COPY = [
    "local.properties",
]


def _worktree_path(slug: str) -> Path:
    safe_slug = slug.strip().replace("/", "-")
    return PROJECT_ROOT.parent / f"{PROJECT_ROOT.name}-wt-{safe_slug}"


def cmd_create(args: argparse.Namespace) -> int:
    wt_path = _worktree_path(args.slug)
    branch_name = f"feat/{args.slug}"

    if wt_path.exists():
        print(f"ERROR: Worktree directory already exists: {wt_path}")
        return 1

    git_cmd = ["git", "worktree", "add", "-b", branch_name, str(wt_path)]
    if args.base:
        git_cmd.append(args.base)

    result = subprocess.run(git_cmd, cwd=PROJECT_ROOT)
    if result.returncode != 0:
        return result.returncode

    copied = []
    for rel_file in LOCAL_FILES_TO_COPY:
        src = PROJECT_ROOT / rel_file
        dst = wt_path / rel_file
        if src.exists() and not dst.exists():
            shutil.copy2(src, dst)
            copied.append(rel_file)

    print(f"\nWorktree ready: {wt_path}")
    print(f"Branch:         {branch_name}")
    if copied:
        print(f"Copied local:   {', '.join(copied)}")
    print("\nNext steps inside the worktree:")
    print(f"  cd {wt_path}")
    print("  python3 -m agents_dev run --spec specs/<tu_spec>.md")
    return 0


def cmd_list(_: argparse.Namespace) -> int:
    return subprocess.run(["git", "worktree", "list"], cwd=PROJECT_ROOT).returncode


def cmd_remove(args: argparse.Namespace) -> int:
    wt_path = _worktree_path(args.slug)
    cmd = ["git", "worktree", "remove"]
    if args.force:
        cmd.append("--force")
    cmd.append(str(wt_path))

    result = subprocess.run(cmd, cwd=PROJECT_ROOT)
    if result.returncode != 0:
        return result.returncode

    print(f"Removed worktree: {wt_path}")
    if args.delete_branch:
        branch_name = f"feat/{args.slug}"
        subprocess.run(["git", "branch", "-d", branch_name], cwd=PROJECT_ROOT)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Manage HomeTutorPro git worktrees.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    p_create = subparsers.add_parser("create", help="Create a sibling worktree for a feature.")
    p_create.add_argument("slug", help="Short feature slug (e.g. payment-badge).")
    p_create.add_argument("--base", default=None, help="Base branch/commit (defaults to HEAD).")
    p_create.set_defaults(func=cmd_create)

    p_list = subparsers.add_parser("list", help="List active git worktrees.")
    p_list.set_defaults(func=cmd_list)

    p_remove = subparsers.add_parser("remove", help="Remove a sibling worktree.")
    p_remove.add_argument("slug", help="Feature slug used when creating the worktree.")
    p_remove.add_argument("--force", action="store_true", help="Force removal even with uncommitted changes.")
    p_remove.add_argument("--delete-branch", action="store_true", help="Also delete the feat/<slug> branch.")
    p_remove.set_defaults(func=cmd_remove)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
