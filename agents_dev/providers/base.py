from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import List, Dict, Any, Optional


@dataclass
class ToolCall:
    id: str
    name: str
    args: Dict[str, Any]


@dataclass
class LLMResponse:
    content: str = ""
    tool_calls: List[ToolCall] = field(default_factory=list)
    raw: Any = None

    @property
    def has_tool_calls(self) -> bool:
        return len(self.tool_calls) > 0


class BaseLLMProvider(ABC):
    """
    Abstract and universal interface to connect any LLM with Tool Calling support.
    Enables swapping between Claude, Gemini, or other providers without changing the core engine.
    """

    @abstractmethod
    def chat(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        system_prompt: str = ""
    ) -> LLMResponse:
        """
        Sends the message history, tool schemas, and system prompt to the model.
        Returns a unified LLMResponse containing text and/or tool calls.
        """
        pass
