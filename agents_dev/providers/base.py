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
    Interfaz abstracta y universal para conectar cualquier LLM con soporte de Tool Calling.
    Permite cambiar de Claude a Gemini o GPT sin tocar una sola línea del motor agéntico.
    """

    @abstractmethod
    def chat(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        system_prompt: str = ""
    ) -> LLMResponse:
        """
        Envía el historial de mensajes, la lista de herramientas y el system prompt al modelo.
        Retorna un LLMResponse unificado que contiene texto y/o llamadas a herramientas.
        """
        pass
