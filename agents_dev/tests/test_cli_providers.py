import os
import sys
import unittest

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT_DIR not in sys.path:
    sys.path.insert(0, ROOT_DIR)

from cli import get_default_provider
from providers.gemini_provider import GeminiProvider
from providers.anthropic_provider import AnthropicProvider


class TestCliProviders(unittest.TestCase):
    def test_gemini_provider_default_model(self):
        prov = get_default_provider("gemini")
        self.assertIsInstance(prov, GeminiProvider)
        self.assertEqual(prov.model, "gemini-2.5-flash")

    def test_gemini_provider_custom_model(self):
        prov = get_default_provider("gemini", model="gemini-2.5-pro")
        self.assertIsInstance(prov, GeminiProvider)
        self.assertEqual(prov.model, "gemini-2.5-pro")

    def test_anthropic_provider_custom_model(self):
        prov = get_default_provider("claude", model="claude-3-7-sonnet")
        self.assertIsInstance(prov, AnthropicProvider)
        self.assertEqual(prov.model, "claude-3-7-sonnet")


if __name__ == "__main__":
    unittest.main()
