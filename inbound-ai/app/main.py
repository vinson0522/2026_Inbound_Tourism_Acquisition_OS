import logging
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.config import get_settings
from app.routers import diagnose, health, llm
from app.services.llm_gateway import ProbeConfigError, PROBE_CONFIG_ERROR
from app.workers.diagnose_worker import start_worker, stop_worker

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    worker = await start_worker(settings)
    if worker:
        logger.info("Diagnose MQ worker started")
    yield
    await stop_worker()


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(
        title="Inbound AI",
        description="TourGEO AI microservice (EPIC-10 / EPIC-2)",
        version=settings.app_version,
        lifespan=lifespan,
    )

    @app.middleware("http")
    async def trace_id_middleware(request: Request, call_next):
        trace_id = request.headers.get("X-Trace-Id") or str(uuid.uuid4())
        request.state.trace_id = trace_id
        response = await call_next(request)
        response.headers["X-Trace-Id"] = trace_id
        return response

    @app.exception_handler(ProbeConfigError)
    async def probe_config_handler(_request: Request, exc: ProbeConfigError):
        return JSONResponse(
            status_code=400,
            content={
                "code": 40001,
                "message": str(exc) or PROBE_CONFIG_ERROR,
                "data": None,
                "trace_id": getattr(_request.state, "trace_id", str(uuid.uuid4())),
            },
        )

    app.include_router(health.router)
    app.include_router(llm.router)
    app.include_router(diagnose.router)

    return app


app = create_app()
