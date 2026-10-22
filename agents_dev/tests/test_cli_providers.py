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
        self.assertEqual(prov.model, "gemini-3.1-flash-lite")

    def test_gemini_provider_custom_model(self):
        prov = get_default_provider("gemini", model="gemini-2.5-pro")
        self.assertIsInstance(prov, GeminiProvider)
        self.assertEqual(prov.model, "gemini-2.5-pro")

    def test_anthropic_provider_custom_model(self):
        prov = get_default_provider("claude", model="claude-3-7-sonnet")
        self.assertIsInstance(prov, AnthropicProvider)
        self.assertEqual(prov.model, "claude-3-7-sonnet")

    def test_gemini_raw_content_in_convert_messages(self):
        from unittest.mock import MagicMock, patch
        prov = GeminiProvider(api_key="fake")
        
        mock_raw_content = MagicMock()
        mock_raw_content.role = "model"
        mock_raw_content.parts = [MagicMock()]
        
        messages = [
            {"role": "user", "content": "hello"},
            {"role": "assistant", "content": "calling tool", "tool_calls": [{"name": "test", "args": {}}], "raw": mock_raw_content}
        ]
        
        with patch.dict("sys.modules", {"google": MagicMock(), "google.genai": MagicMock(), "google.genai.types": MagicMock()}):
            import google.genai.types as types
            types.Content = MagicMock
            types.Part = MagicMock
            contents = prov._convert_messages_to_contents(messages)
            # The assistant message should directly be mock_raw_content
            self.assertEqual(len(contents), 2)
            self.assertEqual(contents[1], mock_raw_content)


if __name__ == "__main__":
    unittest.main()
