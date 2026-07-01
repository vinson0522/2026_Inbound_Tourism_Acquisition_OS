"""Content script generation models (EPIC-4 / FR-301/302)."""

from pydantic import BaseModel, Field

SUPPORTED_PLATFORMS: tuple[str, ...] = (
    "tiktok",
    "youtube_shorts",
    "instagram",
    "youtube",
    "facebook",
    "x",
    "rednote",
)

SUPPORTED_DURATIONS: tuple[int, ...] = (15, 30, 60)


class ContentGenerateRequest(BaseModel):
    tenant_id: int = Field(..., alias="tenantId")
    project_id: int = Field(..., alias="projectId")
    keyword_id: int | None = Field(default=None, alias="keywordId")
    keyword_text: str = Field(..., min_length=1, max_length=500, alias="keywordText")
    platform: str = Field(..., min_length=1, max_length=64)
    duration_sec: int = Field(default=30, ge=5, le=180, alias="durationSec")
    tone: str = Field(default="friendly", max_length=64)
    language: str = Field(default="en", max_length=16)
    target_market: str = Field(default="US", max_length=16, alias="targetMarket")
    use_rag: bool = Field(default=True, alias="useRag")
    trace_id: str | None = Field(default=None, alias="traceId")

    model_config = {"populate_by_name": True}


class StoryboardScene(BaseModel):
    scene: int
    duration: int
    visual: str
    note: str | None = None


class ContentGenerateData(BaseModel):
    title: str | None = None
    hook: str
    script: str
    voiceover: str
    on_screen_text: str
    cta: str
    storyboard_json: list[StoryboardScene]
    needs_human_review: bool = True
    chunk_ids: list[int] | None = None
    target_audience: str | None = None
    hashtags: str | None = None
    landing_page_suggestion: str | None = None
    model: str | None = None
    capture_method: str | None = None
