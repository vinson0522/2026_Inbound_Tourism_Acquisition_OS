"""Landing page generation models (EPIC-6 / FR-502~505)."""

from typing import Any

from pydantic import BaseModel, Field

SUPPORTED_TEMPLATE_TYPES: tuple[str, ...] = (
    "destination",
    "route",
    "theme",
    "visa",
    "event",
)

LANDING_MODULE_KEYS: tuple[str, ...] = (
    "hero",
    "why_this_trip",
    "itinerary",
    "what_we_provide",
    "traveler_reviews",
    "faq",
    "lead_form",
    "whatsapp_cta",
)


class LandingGenerateRequest(BaseModel):
    tenant_id: int = Field(..., alias="tenantId")
    project_id: int = Field(..., alias="projectId")
    keyword_id: int | None = Field(default=None, alias="keywordId")
    keyword_text: str = Field(..., min_length=1, max_length=500, alias="keywordText")
    template_type: str = Field(default="destination", max_length=64, alias="templateType")
    language: str = Field(default="en", max_length=16)
    target_market: str = Field(default="US", max_length=16, alias="targetMarket")
    use_rag: bool = Field(default=True, alias="useRag")
    trace_id: str | None = Field(default=None, alias="traceId")

    model_config = {"populate_by_name": True}


class LandingModule(BaseModel):
    key: str
    content: dict[str, Any] = Field(default_factory=dict)


class LandingContentJson(BaseModel):
    modules: list[LandingModule]


class SeoMetaJson(BaseModel):
    title: str
    description: str
    h1: str | None = None
    faq_schema: list[dict[str, Any]] | None = None


class FormConfigJson(BaseModel):
    fields: list[str]
    submit_label: str | None = None
    whatsapp_link: str | None = None
    whatsapp_label: str | None = None


class LandingGenerateData(BaseModel):
    title: str
    content_json: LandingContentJson
    seo_meta_json: SeoMetaJson
    form_config_json: FormConfigJson
    needs_human_review: bool = True
    chunk_ids: list[int] | None = None
    model: str | None = None
    capture_method: str | None = None
