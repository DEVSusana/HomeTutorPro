#!/usr/bin/env python3
"""
HomeTutorPro - Code Size and Complexity Auditor
Checks Kotlin files against AGENTS.md limits:
- Maximum 300 lines per file (excluding comments/blanks if desired, or total lines)
- Highlights top oversized files and functions
"""

import os
import sys
from pathlib import Path

MAX_FILE_LINES = 300
MAX_FUNCTION_LINES = 60

def audit_file_sizes(src_dir: str):
    oversized_files = []
    total_files = 0

    for root, _, files in os.walk(src_dir):
        for f in files:
            if f.endswith(".kt") and not f.endswith("Test.kt"):
                total_files += 1
                full_path = os.path.join(root, f)
                with open(full_path, "r", encoding="utf-8", errors="ignore") as file_obj:
                    lines = file_obj.readlines()
                    line_count = len(lines)
                    if line_count > MAX_FILE_LINES:
                        oversized_files.append((line_count, full_path))

    oversized_files.sort(key=lambda x: x[0], reverse=True)

    print(f"\n=======================================================")
    print(f"📊 HomeTutorPro Code Metrics Audit")
    print(f"   Scanned: {total_files} Kotlin files in {src_dir}")
    print(f"   Threshold: Max {MAX_FILE_LINES} lines per file")
    print(f"=======================================================\n")

    if not oversized_files:
        print("✅ All files comply with the max 300 lines rule!")
        return 0

    print(f"⚠️ Found {len(oversized_files)} files exceeding {MAX_FILE_LINES} lines:\n")
    for count, path in oversized_files:
        rel_path = os.path.relpath(path, start=os.getcwd())
        print(f" - [{count:4d} lines] {rel_path}")

    print(f"\n💡 Refer to AGENTS.md (Section 3) to refactor these into smaller components.")
    return len(oversized_files)

if __name__ == "__main__":
    target_dir = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/java"
    audit_file_sizes(target_dir)
