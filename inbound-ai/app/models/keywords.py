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


SCORE_DIMENSIONS: tuple[str, ...] = (
    "relevance",
    "long_tail_value",
    "producibility",
    "landing_value",
    "competitive_pressure",
)


class KeywordScoreRequest(BaseModel):
    tenant_id: int = Field(..., alias="tenantId")
    project_id: int = Field(..., alias="projectId")
    keyword_id: int = Field(..., alias="keywordId")
    keyword: str = Field(..., min_length=1, max_length=512)
    keyword_en: str | None = Field(default=None, alias="keywordEn", max_length=512)
    stage: str = Field(..., min_length=1, max_length=32)
    market: str = Field(..., min_length=1, max_length=16)
    brand_name: str | None = Field(default=None, alias="brandName", max_length=256)
    competitors: list[str] | None = None
    geo_score: float | None = Field(default=None, alias="geoScore", ge=0, le=100)
    use_rag: bool = Field(default=True, alias="useRag")
    trace_id: str | None = Field(default=None, alias="traceId")

    model_config = {"populate_by_name": True}


class KeywordScoreDetail(BaseModel):
    relevance: float = Field(..., ge=0, le=100)
    long_tail_value: float = Field(..., ge=0, le=100)
    producibility: float = Field(..., ge=0, le=100)
    landing_value: float = Field(..., ge=0, le=100)
    competitive_pressure: float = Field(..., ge=0, le=100)
    geo_score_input: float | None = Field(default=None, alias="geoScoreInput")
    weights_version: str
    computed_at: str

    model_config = {"populate_by_name": True}


class KeywordScoreData(BaseModel):
    score: float = Field(..., ge=0, le=100)
    score_detail: KeywordScoreDetail
    needs_human_review: bool = False
    model: str | None = None
    capture_method: str | None = None
