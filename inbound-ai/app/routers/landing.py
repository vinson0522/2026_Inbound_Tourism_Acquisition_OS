"""Landing page generation routes (EPIC-6 / FR-502~505)."""

from fastapi import APIRouter, Depends, Request

from app.deps import verify_internal_token
from app.models.common import ApiResponse
from app.models.landing import LandingGenerateData, LandingGenerateRequest
from app.services import landing_service

router = APIRouter(prefix="/ai", tags=["landing"])


@router.post(
    "/landing/generate",
    response_model=ApiResponse[LandingGenerateData],
    dependencies=[Depends(verify_internal_token)],
)
async def generate_landing(
    request: Request,
    payload: LandingGenerateRequest,
) -> ApiResponse[LandingGenerateData]:
    data = await landing_service.generate_landing(payload)
    return ApiResponse(data=data, trace_id=payload.trace_id or request.state.trace_id)
