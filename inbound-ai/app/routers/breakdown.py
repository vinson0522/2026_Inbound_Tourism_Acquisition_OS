"""Viral breakdown routes (EPIC-5 / FR-402/403)."""

from fastapi import APIRouter, Depends, Request

from app.deps import verify_internal_token
from app.models.breakdown import (
    BreakdownAnalyzeData,
    BreakdownAnalyzeRequest,
    BreakdownExtractData,
    BreakdownExtractRequest,
)
from app.models.common import ApiResponse
from app.services import breakdown_service

router = APIRouter(prefix="/ai/breakdown", tags=["breakdown"])


@router.post(
    "/extract-frames",
    response_model=ApiResponse[BreakdownExtractData],
    dependencies=[Depends(verify_internal_token)],
)
async def extract_frames(
    request: Request,
    payload: BreakdownExtractRequest,
) -> ApiResponse[BreakdownExtractData]:
    data = await breakdown_service.extract_frames(payload)
    return ApiResponse(data=data, trace_id=payload.trace_id or request.state.trace_id)


@router.post(
    "/analyze",
    response_model=ApiResponse[BreakdownAnalyzeData],
    dependencies=[Depends(verify_internal_token)],
)
async def analyze(
    request: Request,
    payload: BreakdownAnalyzeRequest,
) -> ApiResponse[BreakdownAnalyzeData]:
    data = await breakdown_service.analyze(payload)
    return ApiResponse(data=data, trace_id=payload.trace_id or request.state.trace_id)
