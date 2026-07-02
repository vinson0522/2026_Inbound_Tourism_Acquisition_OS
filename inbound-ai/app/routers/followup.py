"""Lead follow-up suggestion routes (EPIC-7 / FR-603)."""

from fastapi import APIRouter, Depends, Request

from app.deps import verify_internal_token
from app.models.common import ApiResponse
from app.models.followup import FollowupGenerateData, FollowupGenerateRequest
from app.services import followup_service

router = APIRouter(prefix="/ai", tags=["followup"])


@router.post(
    "/followup/generate",
    response_model=ApiResponse[FollowupGenerateData],
    dependencies=[Depends(verify_internal_token)],
)
async def generate_followup(
    request: Request,
    payload: FollowupGenerateRequest,
) -> ApiResponse[FollowupGenerateData]:
    data = await followup_service.generate_followup(payload)
    return ApiResponse(data=data, trace_id=payload.trace_id or request.state.trace_id)
