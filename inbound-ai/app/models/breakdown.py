"""Viral video breakdown models (EPIC-5 / FR-402/403)."""

from pydantic import BaseModel, Field

DIMENSION_KEYS: tuple[str, ...] = (
    "theme",
    "hook",
    "shot",
    "subtitle",
    "emotion",
    "psychology",
    "reusable",
)

MOCK_FRAME_COUNT = 6


class BreakdownFrame(BaseModel):
    timestamp: int = Field(..., ge=0, description="Seconds from start")
    timestamp_label: str = Field(..., alias="timestampLabel")
    thumbnail_url: str = Field(..., alias="thumbnailUrl")
    caption: str

    model_config = {"populate_by_name": True}


class BreakdownExtractRequest(BaseModel):
    tenant_id: int | None = Field(default=None, alias="tenantId")
    project_id: int | None = Field(default=None, alias="projectId")
    material_id: int | None = Field(default=None, alias="materialId")
    source_url: str = Field(..., min_length=1, alias="sourceUrl")
    title: str | None = None
    trace_id: str | None = Field(default=None, alias="traceId")

    model_config = {"populate_by_name": True}


class BreakdownExtractData(BaseModel):
    frames: list[BreakdownFrame]
    capture_method: str | None = None


class BreakdownAnalyzeRequest(BaseModel):
    tenant_id: int | None = Field(default=None, alias="tenantId")
    project_id: int | None = Field(default=None, alias="projectId")
    source_url: str | None = Field(default=None, alias="sourceUrl")
    title: str | None = None
    frames: list[BreakdownFrame] = Field(..., min_length=1)
    trace_id: str | None = Field(default=None, alias="traceId")

    model_config = {"populate_by_name": True}


class BreakdownAnalyzeData(BaseModel):
    dimensions: dict[str, str]
    reusable_structure: str
    needs_human_review: bool = True
    model: str | None = None
    capture_method: str | None = None
