import os
import sys
import unittest

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT_DIR not in sys.path:
    sys.path.insert(0, ROOT_DIR)
SKILLS_DIR = os.path.join(ROOT_DIR, "skills")
if SKILLS_DIR not in sys.path:
    sys.path.insert(0, SKILLS_DIR)

from context_engine import (
    classify_component,
    get_component_boundaries,
    get_related_project_context,
    COMPONENT_DAO,
    COMPONENT_ROOM_ENTITY,
    COMPONENT_USECASE,
    COMPONENT_REPO_IMPL,
    COMPONENT_VIEWMODEL
)


class TestContextEngine(unittest.TestCase):
    def test_classify_dao(self):
        c_type = classify_component("app/src/main/java/com/devsusana/hometutorpro/data/local/dao/ScheduleDao.kt")
        self.assertEqual(c_type, COMPONENT_DAO)

    def test_classify_room_entity(self):
        c_type = classify_component("app/src/main/java/com/devsusana/hometutorpro/data/local/entities/StudentEntity.kt")
        self.assertEqual(c_type, COMPONENT_ROOM_ENTITY)

    def test_classify_usecase(self):
        c_type = classify_component("app/src/main/java/com/devsusana/hometutorpro/domain/usecases/implementations/RegisterPaymentUseCase.kt")
        self.assertEqual(c_type, COMPONENT_USECASE)

    def test_classify_repo_impl(self):
        c_type = classify_component("app/src/main/java/com/devsusana/hometutorpro/data/repository/StudentRepositoryImpl.kt")
        self.assertEqual(c_type, COMPONENT_REPO_IMPL)

    def test_component_boundaries_anti_hallucination(self):
        dao_bounds = get_component_boundaries(COMPONENT_DAO)
        self.assertIn("in_scope", dao_bounds)
        self.assertIn("out_of_scope", dao_bounds)
        out_of_scope_text = " ".join(dao_bounds["out_of_scope"])
        self.assertIn("SQLCipher", out_of_scope_text)

    def test_related_project_context(self):
        ctx = get_related_project_context("app/src/main/java/com/devsusana/hometutorpro/data/local/dao/ScheduleDao.kt")
        self.assertIn("ScheduleExceptionDao.kt", ctx)
        self.assertIn("StudentDao.kt", ctx)


if __name__ == "__main__":
    unittest.main()
