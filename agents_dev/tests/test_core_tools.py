import os
import sys
import unittest

# Ensure agents_dev is on sys.path
ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT_DIR not in sys.path:
    sys.path.insert(0, ROOT_DIR)

from core.system_tools import (
    write_new_file,
    read_file,
    patch_file,
    run_command,
    list_files,
    PROJECT_ROOT
)


class TestSystemTools(unittest.TestCase):
    def setUp(self):
        self.test_rel_path = "agents_dev/tests/sample_dummy.kt"
        self.test_full_path = os.path.join(PROJECT_ROOT, self.test_rel_path)
        if os.path.exists(self.test_full_path):
            os.remove(self.test_full_path)

    def tearDown(self):
        if os.path.exists(self.test_full_path):
            os.remove(self.test_full_path)

    def test_full_lifecycle_and_surgical_patching(self):
        # 1. Create new file
        initial_code = """package com.example.test

class SampleCalculator {
    fun add(a: Int, b: Int): Int {
        return a + b
    }

    fun multiply(a: Int, b: Int): Int {
        return a * b
    }
}
"""
        res_create = write_new_file(self.test_rel_path, initial_code)
        self.assertIn("SUCCESS", res_create)
        self.assertTrue(os.path.exists(self.test_full_path))

        # 2. Read line range
        read_res = read_file(self.test_rel_path, start_line=3, end_line=6)
        self.assertIn("class SampleCalculator", read_res)
        self.assertIn("fun add", read_res)
        self.assertNotIn("fun multiply", read_res)

        # 3. Valid surgical patch (1 occurrence)
        search_block = """    fun add(a: Int, b: Int): Int {
        return a + b
    }"""
        replace_block = """    fun add(a: Int, b: Int): Int {
        // Optimized addition
        return a + b
    }"""
        res_patch = patch_file(self.test_rel_path, search_block, replace_block)
        self.assertIn("SUCCESS", res_patch)

        # Verify only the add block changed and multiply remains intact
        with open(self.test_full_path, "r", encoding="utf-8") as f:
            updated_content = f.read()
        self.assertIn("// Optimized addition", updated_content)
        self.assertIn("fun multiply", updated_content)

        # 4. Failure on target not found (0 occurrences)
        res_not_found = patch_file(self.test_rel_path, "fun nonExistent(): Unit", "fun valid(): Unit")
        self.assertIn("ERROR: Target block not found", res_not_found)

        # 5. Failure on ambiguity (>1 occurrences)
        res_ambiguous = patch_file(self.test_rel_path, "Int", "Long")
        self.assertIn("ERROR: Target block found", res_ambiguous)

        # 6. Command execution
        res_cmd = run_command("echo 'Agentic Engine Test'")
        self.assertIn("Agentic Engine Test", res_cmd)
        self.assertIn("SUCCESS", res_cmd)


if __name__ == "__main__":
    unittest.main()
