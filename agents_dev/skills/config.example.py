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
from google import genai

API_KEY = os.environ.get("GEMINI_API_KEY", "")
MODEL_ID = "gemini-3.1-flash-lite"


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
