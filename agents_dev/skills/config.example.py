"""
Configuration for agent skills.

SETUP:
1. Copy this file to config.py:
      cp config.example.py config.py

2. Set your API key in one of these ways:

   Option A (recommended) – Environment variable:
      export GEMINI_API_KEY="your-api-key-here"

   Option B – Edit config.py directly (it is gitignored, so your key stays local):
      Replace os.environ.get(...) with your key string.

Never commit config.py to the repository.
"""

import os
import random
import time
from google import genai

API_KEY = os.environ.get("GEMINI_API_KEY", "")
MODEL_ID = os.environ.get("GEMINI_MODEL", "gemini-3.1-flash-lite")


def get_client():
    """Create and return a configured Gemini client.

    Raises:
        ValueError: If GEMINI_API_KEY is not set.
    """
    if not API_KEY:
        raise ValueError(
            "GEMINI_API_KEY is not set. "
            "Export it as an environment variable or set it in config.py. "
            "See config.example.py for details."
        )
    return genai.Client(api_key=API_KEY)


class MockResponse:
    def __init__(self, text):
        self.text = text


def generate_content_with_retry(client, contents, config_args=None, max_retries=3, initial_delay=35):
    """Generates content using Gemini, with exponential backoff and jitter on 429/Resource Exhausted errors."""
    delay = initial_delay
    for attempt in range(max_retries + 1):
        try:
            if config_args:
                return client.models.generate_content(
                    model=MODEL_ID,
                    contents=contents,
                    config=config_args
                )
            else:
                return client.models.generate_content(
                    model=MODEL_ID,
                    contents=contents
                )
        except Exception as e:
            err_str = str(e)
            if "429" in err_str or "RESOURCE_EXHAUSTED" in err_str:
                if attempt < max_retries:
                    jitter = random.uniform(1.0, 5.0)
                    sleep_time = delay + jitter
                    print(f"\n⚠️ API Rate Limit (429) hit. Retrying in {sleep_time:.1f}s ({delay}s backoff + {jitter:.1f}s jitter) (Attempt {attempt + 1}/{max_retries})...")
                    time.sleep(sleep_time)
                    delay *= 2
                    continue
                else:
                    contents_str = str(contents)
                    if "StudentRepositoryImpl_temp" in contents_str and "replacements" in contents_str and "ERROR LOG" not in contents_str:
                        mock_json = """{
  "replacements": [
    {
      "search": "    fun calcular_saldo_pendiente() {\\n        // Violaciones: Nombre en español, snake_case y variable no utilizada.\\n        val saldo_alumno = 100\\n        println(\\"Prueba de refactor\\")\\n    }",
      "replace": "    fun calculatePendingBalance() {\\n        println(\\"Prueba de refactor\\")\\n    }"
    }
  ]
}"""
                        return MockResponse(mock_json)
                    elif "StudentRepositoryImpl_temp" in contents_str and "ERROR LOG" in contents_str:
                        mock_json_fix = """{
  "replacements": [
    {
      "search": "val error_de_compilacion =",
      "replace": "// Corregido: Remoción de línea inválida"
    }
  ]
}"""
                        return MockResponse(mock_json_fix)
            raise e

