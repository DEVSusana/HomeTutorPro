import os
from typing import List, Dict, Any
from providers.base import BaseLLMProvider, LLMResponse, ToolCall


class AnthropicProvider(BaseLLMProvider):
    """
    Native connector for the Anthropic Claude API (e.g. Claude 3.5 Sonnet).
    Optimized for structured tool use and coding workflows.
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
                "The 'anthropic' library is not installed. "
                "To install it, run:\n"
                "  pip3 install --break-system-packages anthropic\n"
                "or in your virtual environment:\n"
                "  pip install -r agents_dev/requirements.txt"
            )

        if not self.api_key:
            raise ValueError(
                "ANTHROPIC_API_KEY is not configured. "
                "Run in your terminal: export ANTHROPIC_API_KEY='your-api-key'"
            )

        self._client = anthropic.Anthropic(api_key=self.api_key)
        return self._client

    def _convert_tools(self, tools: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Converts universal tool schemas to Anthropic input_schema format."""
        anthropic_tools = []
        for t in tools:
            anthropic_tools.append({
                "name": t["name"],
                "description": t.get("description", ""),
                "input_schema": t.get("parameters", {"type": "object", "properties": {}})
            })
        return anthropic_tools

    def _convert_messages(self, messages: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Converts universal messages to Anthropic's structured content block format."""
        anthropic_messages = []
        for msg in messages:
            role = msg.get("role")
            if role == "assistant":
                content_blocks = []
                if msg.get("content"):
                    content_blocks.append({"type": "text", "text": str(msg["content"])})
                if msg.get("tool_calls"):
                    for tc in msg["tool_calls"]:
                        content_blocks.append({
                            "type": "tool_use",
                            "id": tc["id"],
                            "name": tc["name"],
                            "input": tc.get("args", {})
                        })
                if content_blocks:
                    anthropic_messages.append({"role": "assistant", "content": content_blocks})

            elif role == "tool_results":
                content_blocks = []
                for tr in msg.get("tools", []):
                    content_blocks.append({
                        "type": "tool_result",
                        "tool_use_id": tr["tool_call_id"],
                        "content": str(tr["content"])
                    })
                if content_blocks:
                    anthropic_messages.append({"role": "user", "content": content_blocks})

            elif role == "tool":
                anthropic_messages.append({
                    "role": "user",
                    "content": [{
                        "type": "tool_result",
                        "tool_use_id": msg.get("tool_call_id", "unknown_call"),
                        "content": str(msg.get("content", ""))
                    }]
                })
            else:
                anthropic_messages.append({
                    "role": "user",
                    "content": str(msg.get("content", ""))
                })
        return anthropic_messages

    def chat(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        system_prompt: str = ""
    ) -> LLMResponse:
        client = self._get_client()
        anthropic_tools = self._convert_tools(tools) if tools else []
        anthropic_messages = self._convert_messages(messages)

        # Preparar llamadas a Anthropic Messages API
        kwargs = {
            "model": self.model,
            "max_tokens": 4096,
            "messages": anthropic_messages
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
