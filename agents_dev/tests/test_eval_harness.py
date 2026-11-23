import unittest

from evals.eval_harness import run_eval_suite


class TestEvalHarness(unittest.TestCase):
    def test_all_harness_eval_cases_pass(self):
        scorecard = run_eval_suite()
        self.assertEqual(scorecard["total"], 5)
        self.assertEqual(scorecard["passed"], 5)
        self.assertEqual(scorecard["pass_rate"], 100.0)


if __name__ == "__main__":
    unittest.main()
