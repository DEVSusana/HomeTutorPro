import os
import sys
import unittest

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT_DIR not in sys.path:
    sys.path.insert(0, ROOT_DIR)

from core.skill_tools import check_hilt_bindings, execute_skill_tool, SKILL_TOOLS


class TestSkillTools(unittest.TestCase):
    def test_skill_tools_schemas_defined(self):
        tool_names = [t["name"] for t in SKILL_TOOLS]
        self.assertIn("check_hilt_bindings", tool_names)
        self.assertIn("audit_architecture", tool_names)
        self.assertIn("audit_security", tool_names)
        self.assertIn("audit_refactoring", tool_names)
        self.assertIn("design_tests", tool_names)
        self.assertIn("scaffold_test", tool_names)
        self.assertIn("audit_kdoc", tool_names)

    def test_check_hilt_bindings_existing(self):
        res = check_hilt_bindings("app/src/main/java/com/devsusana/hometutorpro/domain/repository/StudentRepository.kt")
        self.assertIn("PASS", res)
        self.assertIn("RepositoryModule.kt", res)

    def test_check_hilt_bindings_non_existent(self):
        res = check_hilt_bindings("non_existent_file.kt")
        self.assertIn("ERROR", res)

    def test_execute_skill_tool_dispatcher(self):
        res = execute_skill_tool("check_hilt_bindings", {"file_path": "non_existent_file.kt"})
        self.assertIn("ERROR", res)

        unknown_res = execute_skill_tool("unknown_tool", {})
        self.assertIn("ERROR", unknown_res)


if __name__ == "__main__":
    unittest.main()
