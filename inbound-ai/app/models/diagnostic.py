"""GEO diagnostic Pydantic models (EPIC-2 M1)."""

from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field

PRD_DEFAULT_METRIC_WEIGHTS: dict[str, float] = {
    "brand_mention_rate": 0.25,
    "top3_rate": 0.20,
    "competitor_suppression": 0.15,
    "citation_coverage": 0.15,
    "longtail_coverage": 0.15,
    "asset_completeness": 0.10,
}


class DiagnoseRequest(BaseModel):
    trace_id: str | None = None
    run_id: int
    question_id: int
    tenant_id: int
    project_id: int
    platform: str = "perplexity"
    probe_mode: Literal["grounded-api", "chat"] = "grounded-api"
    region: str
    locale: str
    question: str
    sample_index: int = 0
    model: str = "perplexity/sonar-pro"
    grounding_enabled: bool = True
    probe_task_id: int | None = None
    customer_brand: str | None = None
    competitor_brands: list[str] | None = None


class CitationItem(BaseModel):
    url: str
    title: str | None = None
    domain: str | None = None
    rank: int
    is_customer: bool = False
    is_competitor: bool = False


class DiagnoseResultData(BaseModel):
    answer_text: str
    model: str
    platform: str
    probe_mode: str
    mentioned_brands: list[str]
    competitors: list[str]
    citations: list[CitationItem]
    rank: int | None = None
    capture_method: str = "grounded-api"
    raw_response_json: dict = Field(default_factory=dict)
    sampled_at: datetime
    run_id: int | None = None
    question_id: int | None = None
    tenant_id: int | None = None
    project_id: int | None = None
    sample_index: int | None = None
    probe_task_id: int | None = None


class ParseCitationsRequest(BaseModel):
    platform: str
    raw_response_json: dict
    customer_brand: str | None = None
    competitor_brands: list[str] | None = None


class ParseCitationsResult(BaseModel):
    citations: list[CitationItem]
    mentioned_brands: list[str] = Field(default_factory=list)
    competitors: list[str] = Field(default_factory=list)
    rank: int | None = None
    answer_text: str = ""


class ResultMetricSummary(BaseModel):
    question_id: int
    brand_mentioned: bool = False
    in_top3: bool = False
    competitor_dominance: float = Field(default=0.0, ge=0.0, le=1.0)
    citation_hit: bool = False
    is_longtail: bool = False
    asset_complete: float = Field(default=1.0, ge=0.0, le=1.0)


class ScoreRequest(BaseModel):
    run_id: int
    metric_weights_json: dict[str, float] | None = None
    results: list[ResultMetricSummary]
    longtail_question_ids: list[int] | None = None


class ScoreResultData(BaseModel):
    geo_score: float
    metrics: dict[str, float]
    metric_weights: dict[str, float]
    uncovered_questions: list[int] = Field(default_factory=list)
