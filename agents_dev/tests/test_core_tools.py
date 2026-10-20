import os
import sys
import unittest

# Asegurar que agents_dev esté en el path
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
        # 1. Crear nuevo archivo
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

        # 2. Leer rango de líneas
        read_res = read_file(self.test_rel_path, start_line=3, end_line=6)
        self.assertIn("class SampleCalculator", read_res)
        self.assertIn("fun add", read_res)
        self.assertNotIn("fun multiply", read_res)

        # 3. Parche quirúrgico válido (1 ocurrencia)
        search_block = """    fun add(a: Int, b: Int): Int {
        return a + b
    }"""
        replace_block = """    fun add(a: Int, b: Int): Int {
        // Optimized addition
        return a + b
    }"""
        res_patch = patch_file(self.test_rel_path, search_block, replace_block)
        self.assertIn("SUCCESS", res_patch)

        # Verificar que solo cambió el bloque add y multiply sigue intacto
        with open(self.test_full_path, "r", encoding="utf-8") as f:
            updated_content = f.read()
        self.assertIn("// Optimized addition", updated_content)
        self.assertIn("fun multiply", updated_content)

        # 4. Fallo por no encontrado (0 ocurrencias)
        res_not_found = patch_file(self.test_rel_path, "fun nonExistent(): Unit", "fun valid(): Unit")
        self.assertIn("ERROR: Target block not found", res_not_found)

        # 5. Fallo por ambigüedad (>1 ocurrencias)
        res_ambiguous = patch_file(self.test_rel_path, "Int", "Long")
        self.assertIn("ERROR: Target block found", res_ambiguous)

        # 6. Ejecución de comandos
        res_cmd = run_command("echo 'Agentic Engine Test'")
        self.assertIn("Agentic Engine Test", res_cmd)
        self.assertIn("SUCCESS", res_cmd)


if __name__ == "__main__":
    unittest.main()
