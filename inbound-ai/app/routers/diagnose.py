"""GEO diagnostic HTTP routes (EPIC-2 M1)."""

from fastapi import APIRouter, Depends, HTTPException, Request, status

from app.deps import verify_internal_token
from app.models.common import ApiResponse
from app.models.diagnostic import (
    DiagnoseRequest,
    DiagnoseResultData,
    ParseCitationsRequest,
    ParseCitationsResult,
    ScoreRequest,
    ScoreResultData,
)
from app.services import citation_parser, diagnose_service, scorer

router = APIRouter(prefix="/ai", tags=["diagnostic"])


@router.post(
    "/diagnose",
    response_model=ApiResponse[DiagnoseResultData],
    dependencies=[Depends(verify_internal_token)],
)
async def diagnose(
    request: Request,
    payload: DiagnoseRequest,
) -> ApiResponse[DiagnoseResultData]:
    data = await diagnose_service.run_diagnose(payload)
    return ApiResponse(data=data, trace_id=payload.trace_id or request.state.trace_id)


@router.post(
    "/parse-citations",
    response_model=ApiResponse[ParseCitationsResult],
    dependencies=[Depends(verify_internal_token)],
)
async def parse_citations(
    request: Request,
    payload: ParseCitationsRequest,
) -> ApiResponse[ParseCitationsResult]:
    try:
        data = citation_parser.parse_citations(payload)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
    return ApiResponse(data=data, trace_id=request.state.trace_id)


@router.post(
    "/score",
    response_model=ApiResponse[ScoreResultData],
    dependencies=[Depends(verify_internal_token)],
)
async def score(
    request: Request,
    payload: ScoreRequest,
) -> ApiResponse[ScoreResultData]:
    data = scorer.compute_geo_score(payload)
    return ApiResponse(data=data, trace_id=request.state.trace_id)
