from fastapi import APIRouter, Depends, Request

from app.deps import verify_internal_token
from app.models.common import ApiResponse
from app.models.llm import LlmCompleteData, LlmCompleteRequest
from app.services import llm_gateway

router = APIRouter(prefix="/ai", tags=["llm"])


@router.post(
    "/llm/complete",
    response_model=ApiResponse[LlmCompleteData],
    dependencies=[Depends(verify_internal_token)],
)
async def llm_complete(
    request: Request,
    payload: LlmCompleteRequest,
) -> ApiResponse[LlmCompleteData]:
    data = await llm_gateway.complete(payload)
    return ApiResponse(data=data, trace_id=request.state.trace_id)
