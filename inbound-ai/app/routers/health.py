from fastapi import APIRouter, Depends, Request

from app.config import Settings, get_settings
from app.deps import verify_internal_token
from app.models.common import ApiResponse

router = APIRouter(tags=["health"])


@router.get("/health")
async def public_health(settings: Settings = Depends(get_settings)) -> dict:
    return {
        "status": "ok",
        "service": "inbound-ai",
        "version": settings.app_version,
    }


@router.get("/ai/health", dependencies=[Depends(verify_internal_token)])
async def ai_health(request: Request, settings: Settings = Depends(get_settings)) -> ApiResponse[dict]:
    db_status = "skipped" if not settings.database_url else "configured"
    return ApiResponse(
        data={
            "status": "ok",
            "litellm": settings.litellm_status,
            "db": db_status,
        },
        trace_id=request.state.trace_id,
    )
