from typing import Any, Literal

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str


class LlmCompleteRequest(BaseModel):
    model: str = Field(..., examples=["openai/gpt-4o-mini"])
    messages: list[ChatMessage]
    probe_mode: Literal["chat", "grounded-api"] = "chat"
    grounding_enabled: bool = False
    max_tokens: int = Field(default=256, ge=1, le=4096)
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)


class LlmCompleteData(BaseModel):
    model: str
    content: str
    usage: dict[str, Any] | None = None
    raw: dict[str, Any] | None = None
