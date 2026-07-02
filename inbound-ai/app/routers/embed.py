from fastapi import APIRouter, Depends, HTTPException, Request, status

from app.deps import verify_internal_token
from app.models.common import ApiResponse
from app.models.embed import EmbedRequest, EmbedResultData, RagSearchData, RagSearchRequest
from app.services import embed_service, rag_service
from app.services.embed_service import EmbedPipelineError
from app.services.embedding_service import EmbeddingError
from app.services.reranker_service import RerankerError

router = APIRouter(prefix="/ai", tags=["embed"])


def _service_unavailable(detail: str) -> HTTPException:
    return HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=detail)


@router.post(
    "/embed",
    response_model=ApiResponse[EmbedResultData],
    dependencies=[Depends(verify_internal_token)],
)
async def embed_asset(request: Request, payload: EmbedRequest) -> ApiResponse[EmbedResultData]:
    try:
        data = await embed_service.run_embed(payload)
    except EmbedPipelineError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except EmbeddingError as exc:
        raise _service_unavailable(str(exc)) from exc
    except RuntimeError as exc:
        if "DATABASE_URL" in str(exc):
            raise _service_unavailable(str(exc)) from exc
        raise
    return ApiResponse(data=data, trace_id=request.state.trace_id)


@router.post(
    "/rag/search",
    response_model=ApiResponse[RagSearchData],
    dependencies=[Depends(verify_internal_token)],
)
async def rag_search(request: Request, payload: RagSearchRequest) -> ApiResponse[RagSearchData]:
    try:
        data = await rag_service.search(payload)
    except EmbeddingError as exc:
        raise _service_unavailable(str(exc)) from exc
    except RerankerError as exc:
        raise _service_unavailable(str(exc)) from exc
    except RuntimeError as exc:
        if "DATABASE_URL" in str(exc):
            raise _service_unavailable(str(exc)) from exc
        raise
    return ApiResponse(data=data, trace_id=request.state.trace_id)
