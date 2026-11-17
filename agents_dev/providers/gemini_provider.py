import os
import time
import json
from typing import List, Dict, Any, Optional
from providers.base import BaseLLMProvider, LLMResponse, ToolCall


class GeminiProvider(BaseLLMProvider):
    """
    Native connector for the Google GenAI SDK.
    Includes exponential backoff retry handling for 429 (Resource Exhausted) and 503 (Unavailable) errors.
    """

    def __init__(self, api_key: str = None, model: str = None):
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY", "")
        self.model = model or os.environ.get("GEMINI_MODEL", "gemini-3.1-flash-lite")
        self._client = None

    def _get_client(self):
        if self._client is not None:
            return self._client

        try:
            from google import genai
        except ImportError:
            raise ImportError(
                "The 'google-genai' library is not installed. "
                "To install it, run:\n"
                "  pip3 install --break-system-packages google-genai\n"
                "or in your virtual environment:\n"
                "  pip install -r agents_dev/requirements.txt"
            )

        if not self.api_key:
            raise ValueError(
                "GEMINI_API_KEY is not configured. "
                "Run in your terminal: export GEMINI_API_KEY='your-api-key'"
            )

        from google import genai
        self._client = genai.Client(api_key=self.api_key)
        return self._client

    def _convert_tools_to_gemini(self, tools: List[Dict[str, Any]]):
        """Converts universal tool schemas to Gemini FunctionDeclaration."""
        from google.genai import types
        function_declarations = []
        for t in tools:
            decl = types.FunctionDeclaration(
                name=t["name"],
                description=t.get("description", ""),
                parameters=t.get("parameters", {"type": "object", "properties": {}})
            )
            function_declarations.append(decl)
        return [types.Tool(function_declarations=function_declarations)] if function_declarations else None

    def _convert_messages_to_contents(self, messages: List[Dict[str, Any]]):
        """Converts message history to Gemini contents structure, preserving function calls and responses."""
        from google.genai import types
        contents = []

        for msg in messages:
            role = msg.get("role")

            if role == "assistant":
                # If we have original Gemini Content (with intact thought_signature and thinking tokens), use directly
                raw_obj = msg.get("raw")
                if raw_obj is not None:
                    if hasattr(raw_obj, "parts"):
                        contents.append(raw_obj)
                        continue
                    elif isinstance(getattr(types, "Content", None), type) and isinstance(raw_obj, types.Content):
                        contents.append(raw_obj)
                        continue

                parts = []
                if msg.get("content"):
                    parts.append(types.Part.from_text(text=str(msg["content"])))
                if msg.get("tool_calls"):
                    for tc in msg["tool_calls"]:
                        parts.append(types.Part.from_function_call(name=tc["name"], args=tc.get("args", {})))
                if parts:
                    contents.append(types.Content(role="model", parts=parts))

            elif role == "tool_results":
                parts = []
                for tr in msg.get("tools", []):
                    parts.append(
                        types.Part.from_function_response(
                            name=tr["tool_name"],
                            response={"result": str(tr["content"])}
                        )
                    )
                if parts:
                    contents.append(types.Content(role="user", parts=parts))

            elif role == "tool":
                contents.append(
                    types.Content(
                        role="user",
                        parts=[
                            types.Part.from_function_response(
                                name=msg.get("tool_name", "unknown_tool"),
                                response={"result": str(msg.get("content", ""))}
                            )
                        ]
                    )
                )
            else:
                contents.append(
                    types.Content(
                        role="user",
                        parts=[types.Part.from_text(text=str(msg.get("content", "")))]
                    )
                )

        return contents

    def chat(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        system_prompt: str = ""
    ) -> LLMResponse:
        client = self._get_client()
        from google.genai import types

        gemini_tools = self._convert_tools_to_gemini(tools)
        contents = self._convert_messages_to_contents(messages)

        config_kwargs = {}
        if system_prompt:
            config_kwargs["system_instruction"] = system_prompt
        if gemini_tools:
            config_kwargs["tools"] = gemini_tools

        config = types.GenerateContentConfig(**config_kwargs)

        # Exponential backoff retry loop for Rate Limit 429 and high demand 503 Unavailable
        max_retries = 3
        delay = 6

        for attempt in range(max_retries + 1):
            try:
                response = client.models.generate_content(
                    model=self.model,
                    contents=contents,
                    config=config
                )
                break
            except Exception as e:
                err_str = str(e)
                is_transient = any(code in err_str for code in [
                    "429", "RESOURCE_EXHAUSTED", "503", "UNAVAILABLE", "high demand", "500", "INTERNAL"
                ])
                if is_transient and attempt < max_retries:
                    err_label = "503 High Demand on Google Servers" if any(k in err_str for k in ["503", "UNAVAILABLE", "high demand"]) else "429 Rate Limit"
                    print(f"⚠️ {err_label}. Waiting {delay}s before automatic retry (Attempt {attempt+1}/{max_retries})...")
                    time.sleep(delay)
                    delay *= 2
                    continue
                raise e

        # Extract text, function calls, and preserve raw Content with thought_signature intact
        raw_content = None
        text_content = ""
        tool_calls = []

        if hasattr(response, "candidates") and response.candidates:
            candidate = response.candidates[0]
            raw_content = candidate.content
            if candidate.content and candidate.content.parts:
                for idx, part in enumerate(candidate.content.parts):
                    # Extract text while avoiding SDK warnings
                    if getattr(part, "text", None):
                        if not getattr(part, "thought", False):
                            text_content += part.text
                    elif getattr(part, "function_call", None):
                        fc = part.function_call
                        tool_calls.append(
                            ToolCall(
                                id=getattr(fc, "id", None) or f"call_{fc.name}_{idx}",
                                name=fc.name,
                                args=dict(fc.args) if fc.args else {}
                            )
                        )

        # Fallback to auxiliary properties if not extracted from parts
        if not tool_calls and hasattr(response, "function_calls") and response.function_calls:
            for idx, fc in enumerate(response.function_calls):
                tool_calls.append(
                    ToolCall(
                        id=f"call_{fc.name}_{idx}",
                        name=fc.name,
                        args=dict(fc.args) if fc.args else {}
                    )
                )

        if not text_content and not tool_calls:
            try:
                if hasattr(response, "text") and response.text:
                    text_content = response.text
            except Exception:
                pass

        return LLMResponse(content=text_content.strip(), tool_calls=tool_calls, raw=raw_content)
