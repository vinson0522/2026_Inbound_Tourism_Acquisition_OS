from pydantic import BaseModel, Field


class EmbedRequest(BaseModel):
    trace_id: str | None = None
    asset_id: int = Field(..., alias="assetId")
    tenant_id: int = Field(..., alias="tenantId")
    project_id: int = Field(..., alias="projectId")
    file_url: str | None = Field(default=None, alias="fileUrl")

    model_config = {"populate_by_name": True}


class EmbedResultData(BaseModel):
    asset_id: int
    chunk_count: int
    vector_status: str


class RagSearchRequest(BaseModel):
    tenant_id: int = Field(..., alias="tenantId")
    project_id: int = Field(..., alias="projectId")
    query: str = Field(..., min_length=1, max_length=4000)
    top_k: int = Field(default=3, ge=1, le=10, alias="topK")

    model_config = {"populate_by_name": True}


class RagChunkHit(BaseModel):
    chunk_id: int
    asset_id: int
    chunk_index: int
    chunk_text: str
    score: float


class RagSearchData(BaseModel):
    hits: list[RagChunkHit]
