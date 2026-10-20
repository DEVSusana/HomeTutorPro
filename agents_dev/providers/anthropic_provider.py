import os
from typing import List, Dict, Any
from providers.base import BaseLLMProvider, LLMResponse, ToolCall


class AnthropicProvider(BaseLLMProvider):
    """
    Conector nativo con la API de Anthropic (Claude 3.5 Sonnet).
    Líder en programación y razonamiento agéntico estructurado.
    """

    def __init__(self, api_key: str = None, model: str = None):
        self.api_key = api_key or os.environ.get("ANTHROPIC_API_KEY", "")
        self.model = model or os.environ.get("ANTHROPIC_MODEL", "claude-3-5-sonnet-20241022")
        self._client = None

    def _get_client(self):
        if self._client is not None:
            return self._client

        try:
            import anthropic
        except ImportError:
            raise ImportError(
                "La biblioteca 'anthropic' no está instalada. "
                "Para usar Claude, instala la dependencia ejecutando: pip install anthropic"
            )

        if not self.api_key:
            raise ValueError(
                "ANTHROPIC_API_KEY no está configurada. "
                "Ejecuta en tu terminal: export ANTHROPIC_API_KEY='tu-api-key'"
            )

        self._client = anthropic.Anthropic(api_key=self.api_key)
        return self._client

    def _convert_tools(self, tools: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Convierte los esquemas universales de tools al formato input_schema de Anthropic."""
        anthropic_tools = []
        for t in tools:
            anthropic_tools.append({
                "name": t["name"],
                "description": t.get("description", ""),
                "input_schema": t.get("parameters", {"type": "object", "properties": {}})
            })
        return anthropic_tools

    def chat(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        system_prompt: str = ""
    ) -> LLMResponse:
        client = self._get_client()
        anthropic_tools = self._convert_tools(tools) if tools else []

        # Preparar llamadas a Anthropic Messages API
        kwargs = {
            "model": self.model,
            "max_tokens": 4096,
            "messages": messages
        }
        if system_prompt:
            kwargs["system"] = system_prompt
        if anthropic_tools:
            kwargs["tools"] = anthropic_tools

        response = client.messages.create(**kwargs)

        text_content = ""
        tool_calls = []

        for block in response.content:
            if block.type == "text":
                text_content += block.text
            elif block.type == "tool_use":
                tool_calls.append(
                    ToolCall(
                        id=block.id,
                        name=block.name,
                        args=block.input
                    )
                )

        return LLMResponse(content=text_content.strip(), tool_calls=tool_calls)
