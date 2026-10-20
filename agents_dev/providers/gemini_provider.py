import os
import time
import json
from typing import List, Dict, Any, Optional
from providers.base import BaseLLMProvider, LLMResponse, ToolCall


class GeminiProvider(BaseLLMProvider):
    """
    Conector nativo con Google GenAI SDK (Gemini 2.5 / 2.0 Flash).
    Incluye manejo de reintentos exponenciales ante error 429 (Resource Exhausted).
    """

    def __init__(self, api_key: str = None, model: str = None):
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY", "")
        self.model = model or os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")
        self._client = None

    def _get_client(self):
        if self._client is not None:
            return self._client

        try:
            from google import genai
        except ImportError:
            raise ImportError(
                "La biblioteca 'google-genai' no está instalada. "
                "Para usar Gemini, ejecuta: pip install google-genai"
            )

        if not self.api_key:
            raise ValueError(
                "GEMINI_API_KEY no está configurada. "
                "Ejecuta en tu terminal: export GEMINI_API_KEY='tu-api-key'"
            )

        from google import genai
        self._client = genai.Client(api_key=self.api_key)
        return self._client

    def _convert_tools_to_gemini(self, tools: List[Dict[str, Any]]):
        """Convierte los esquemas universales de tools a FunctionDeclaration de Gemini."""
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
        """Convierte el historial a la estructura de contents de Gemini."""
        from google.genai import types
        contents = []

        for msg in messages:
            role = "user" if msg["role"] == "user" else "model"
            content_text = msg.get("content", "")

            # Si es respuesta de herramienta
            if msg.get("role") == "tool":
                tool_name = msg.get("tool_name", "unknown_tool")
                contents.append(
                    types.Content(
                        role="user",
                        parts=[
                            types.Part.from_function_response(
                                name=tool_name,
                                response={"result": str(content_text)}
                            )
                        ]
                    )
                )
            else:
                contents.append(
                    types.Content(
                        role=role,
                        parts=[types.Part.from_text(text=str(content_text))]
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

        # Bucle de reintento exponencial ante Rate Limit 429
        max_retries = 3
        delay = 15

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
                if ("429" in err_str or "RESOURCE_EXHAUSTED" in err_str) and attempt < max_retries:
                    print(f"⚠️ API Rate Limit (429) alcanzado. Esperando {delay}s antes de reintentar (Intento {attempt+1}/{max_retries})...")
                    time.sleep(delay)
                    delay *= 2
                    continue
                raise e

        # Extraer texto y function calls
        text_content = ""
        tool_calls = []

        if hasattr(response, "text") and response.text:
            text_content = response.text

        if hasattr(response, "function_calls") and response.function_calls:
            for idx, fc in enumerate(response.function_calls):
                tool_calls.append(
                    ToolCall(
                        id=f"call_{fc.name}_{idx}",
                        name=fc.name,
                        args=dict(fc.args) if fc.args else {}
                    )
                )

        return LLMResponse(content=text_content.strip(), tool_calls=tool_calls)
