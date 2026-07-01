"""Content script generation routes (EPIC-4 / FR-301/302)."""

from fastapi import APIRouter, Depends, Request

from app.deps import verify_internal_token
from app.models.common import ApiResponse
from app.models.content import ContentGenerateData, ContentGenerateRequest
from app.services import content_service

router = APIRouter(prefix="/ai", tags=["content"])


@router.post(
    "/content/generate",
    response_model=ApiResponse[ContentGenerateData],
    dependencies=[Depends(verify_internal_token)],
)
async def generate_content(
    request: Request,
    payload: ContentGenerateRequest,
) -> ApiResponse[ContentGenerateData]:
    data = await content_service.generate_content(payload)
    return ApiResponse(data=data, trace_id=payload.trace_id or request.state.trace_id)
