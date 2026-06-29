"""Keyword opportunity generation models (EPIC-3 / FR-201)."""

from pydantic import BaseModel, Field

LIFECYCLE_STAGES: tuple[str, ...] = (
    "inspiration",
    "planting",
    "comparison",
    "visa",
    "planning",
    "trust",
    "decision",
    "repurchase",
)


class KeywordGenerateRequest(BaseModel):
    tenant_id: int = Field(..., alias="tenantId")
    project_id: int = Field(..., alias="projectId")
    market: str = Field(..., min_length=1, max_length=16)
    locale: str = Field(default="en", max_length=16)
    stages: list[str] | None = None
    words_per_stage: int = Field(default=5, ge=1, le=20, alias="wordsPerStage")
    use_rag: bool = Field(default=True, alias="useRag")
    trace_id: str | None = Field(default=None, alias="traceId")

    model_config = {"populate_by_name": True}


class GeneratedKeyword(BaseModel):
    text: str
    rationale: str | None = None
    chunk_ids: list[int] | None = None
    suggested_score: float | None = None
    needs_human_review: bool = True


class StageKeywords(BaseModel):
    stage: str
    keywords: list[GeneratedKeyword]


class KeywordGenerateData(BaseModel):
    needs_human_review: bool = True
    stages: list[StageKeywords]
    model: str | None = None
    capture_method: str | None = None
