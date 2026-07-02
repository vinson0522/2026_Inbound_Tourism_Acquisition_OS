"""Lead follow-up suggestion models (EPIC-7 / FR-603)."""

from datetime import date

from pydantic import BaseModel, Field


class FollowupGenerateRequest(BaseModel):
    tenant_id: int = Field(..., alias="tenantId")
    project_id: int = Field(..., alias="projectId")
    lead_id: int = Field(..., alias="leadId")
    name: str | None = Field(default=None, max_length=200)
    message: str | None = None
    budget: str | None = Field(default=None, max_length=100)
    travel_date: date | None = Field(default=None, alias="travelDate")
    source: str | None = Field(default=None, max_length=64)
    keyword_text: str | None = Field(default=None, max_length=500, alias="keywordText")
    trace_id: str | None = Field(default=None, alias="traceId")

    model_config = {"populate_by_name": True}


class FollowupGenerateData(BaseModel):
    suggestion_en: str = Field(..., alias="suggestionEn")
    suggestion_zh: str = Field(..., alias="suggestionZh")
    needs_human_review: bool = Field(default=True, alias="needsHumanReview")
    model: str | None = None
    capture_method: str | None = Field(default=None, alias="captureMethod")

    model_config = {"populate_by_name": True}
