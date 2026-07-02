"""Keyword opportunity routes (EPIC-3 / FR-201)."""

from fastapi import APIRouter, Depends, Request

from app.deps import verify_internal_token
from app.models.common import ApiResponse
from app.models.keywords import KeywordGenerateData, KeywordGenerateRequest, KeywordScoreData, KeywordScoreRequest
from app.services import keywords_service

router = APIRouter(prefix="/ai", tags=["keywords"])


@router.post(
    "/keywords/generate",
    response_model=ApiResponse[KeywordGenerateData],
    dependencies=[Depends(verify_internal_token)],
)
async def generate_keywords(
    request: Request,
    payload: KeywordGenerateRequest,
) -> ApiResponse[KeywordGenerateData]:
    data = await keywords_service.generate_keywords(payload)
    return ApiResponse(data=data, trace_id=payload.trace_id or request.state.trace_id)


@router.post(
    "/keywords/score",
    response_model=ApiResponse[KeywordScoreData],
    dependencies=[Depends(verify_internal_token)],
)
async def score_keyword(
    request: Request,
    payload: KeywordScoreRequest,
) -> ApiResponse[KeywordScoreData]:
    data = await keywords_service.score_keyword(payload)
    return ApiResponse(data=data, trace_id=payload.trace_id or request.state.trace_id)
