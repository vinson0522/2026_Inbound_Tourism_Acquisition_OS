"""RabbitMQ consumer for diag.grounded-api queue."""

from __future__ import annotations

import asyncio
import json
import logging
from typing import Any

import aio_pika
import httpx

from app.config import Settings, get_settings
from app.models.diagnostic import DiagnoseRequest
from app.services import diagnose_service

logger = logging.getLogger(__name__)

QUEUE_NAME = "diag.grounded-api"
DLQ_NAME = "diag.grounded-api.dlq"
MAX_RETRIES = 3
RETRY_HEADER = "x-retry-count"


class DiagnoseWorker:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self._connection: aio_pika.RobustConnection | None = None
        self._task: asyncio.Task[None] | None = None

    @property
    def callback_url(self) -> str:
        base = (self.settings.core_callback_base_url or "").rstrip("/")
        return f"{base}/api/v1/internal/diagnostics/probe-callback"

    async def start(self) -> None:
        if not self.settings.rabbitmq_url:
            logger.warning("RABBITMQ_URL not set — diagnose worker disabled")
            return
        self._connection = await aio_pika.connect_robust(self.settings.rabbitmq_url)
        channel = await self._connection.channel()
        await channel.set_qos(prefetch_count=1)
        await channel.declare_queue(DLQ_NAME, durable=True)
        queue = await channel.declare_queue(QUEUE_NAME, durable=True)
        await queue.consume(self._on_message)
        logger.info("Diagnose worker consuming queue=%s callback=%s", QUEUE_NAME, self.callback_url)

    async def stop(self) -> None:
        if self._connection and not self._connection.is_closed:
            await self._connection.close()
        self._connection = None

    async def _on_message(self, message: aio_pika.IncomingMessage) -> None:
        retry_count = int(message.headers.get(RETRY_HEADER, 0) if message.headers else 0)
        try:
            payload = json.loads(message.body.decode())
            trace_id = payload.get("trace_id", "")
            logger.info(
                "worker received probe_task_id=%s run_id=%s retry=%s",
                payload.get("probeTaskId") or payload.get("probe_task_id"),
                payload.get("runId") or payload.get("run_id"),
                retry_count,
            )
            req = _payload_to_diagnose_request(payload)
            result = await diagnose_service.run_diagnose(req, settings=self.settings)
            await self._post_callback(
                trace_id=trace_id,
                probe_task_id=req.probe_task_id,
                status="SUCCESS",
                result=result.model_dump(mode="json"),
            )
            await message.ack()
        except Exception as exc:
            logger.exception("worker message failed retry=%s: %s", retry_count, exc)
            if retry_count >= MAX_RETRIES:
                payload = json.loads(message.body.decode())
                probe_task_id = payload.get("probeTaskId") or payload.get("probe_task_id")
                await self._post_callback(
                    trace_id=payload.get("trace_id", ""),
                    probe_task_id=int(probe_task_id) if probe_task_id else None,
                    status="FAILED",
                    error_message=str(exc)[:2000],
                )
                await self._publish_dlq(message.body, retry_count, str(exc))
                await message.ack()
            else:
                await self._republish_with_retry(message, retry_count + 1)
                await message.ack()

    async def _republish_with_retry(self, message: aio_pika.IncomingMessage, retry_count: int) -> None:
        if not self._connection:
            return
        channel = await self._connection.channel()
        await channel.declare_queue(QUEUE_NAME, durable=True)
        headers = dict(message.headers or {})
        headers[RETRY_HEADER] = retry_count
        await channel.default_exchange.publish(
            aio_pika.Message(
                body=message.body,
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                headers=headers,
            ),
            routing_key=QUEUE_NAME,
        )

    async def _post_callback(
        self,
        *,
        trace_id: str,
        probe_task_id: int | None,
        status: str,
        result: dict[str, Any] | None = None,
        error_message: str | None = None,
    ) -> None:
        if not self.settings.core_callback_base_url:
            logger.warning("CORE_CALLBACK_BASE_URL not set — skipping callback")
            return
        body = {
            "traceId": trace_id,
            "probeTaskId": probe_task_id,
            "status": status,
            "result": result,
            "errorMessage": error_message,
        }
        headers = {
            "Authorization": f"Bearer {self.settings.ai_service_internal_token}",
            "Content-Type": "application/json",
        }
        if trace_id:
            headers["X-Trace-Id"] = trace_id
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(self.callback_url, json=body, headers=headers)
            resp.raise_for_status()

    async def _publish_dlq(self, body: bytes, retry_count: int, error: str) -> None:
        if not self._connection:
            return
        channel = await self._connection.channel()
        await channel.declare_queue(DLQ_NAME, durable=True)
        envelope = {"original_body": json.loads(body.decode()), "retry_count": retry_count, "error": error}
        await channel.default_exchange.publish(
            aio_pika.Message(body=json.dumps(envelope).encode(), delivery_mode=aio_pika.DeliveryMode.PERSISTENT),
            routing_key=DLQ_NAME,
        )
        logger.error("message moved to DLQ after %s retries", retry_count)


def _payload_to_diagnose_request(payload: dict[str, Any]) -> DiagnoseRequest:
    """Map Java MQ camelCase payload to DiagnoseRequest."""
    return DiagnoseRequest(
        trace_id=payload.get("trace_id"),
        run_id=int(payload["runId"] if "runId" in payload else payload["run_id"]),
        question_id=int(payload["questionId"] if "questionId" in payload else payload["question_id"]),
        tenant_id=int(payload["tenantId"] if "tenantId" in payload else payload["tenant_id"]),
        project_id=int(payload["projectId"] if "projectId" in payload else payload["project_id"]),
        platform=payload.get("platform", "perplexity"),
        probe_mode=payload.get("probe_mode", payload.get("probeMode", "grounded-api")),
        region=payload["region"],
        locale=payload.get("locale", "en-US"),
        question=payload["question"],
        sample_index=int(payload.get("sampleIndex", payload.get("sample_index", 0))),
        model=payload.get("model", "perplexity/sonar-pro"),
        grounding_enabled=bool(payload.get("grounding_enabled", payload.get("groundingEnabled", True))),
        probe_task_id=int(payload["probeTaskId"]) if payload.get("probeTaskId") else payload.get("probe_task_id"),
        customer_brand=payload.get("customer_brand") or payload.get("customerBrand"),
        competitor_brands=payload.get("competitor_brands") or payload.get("competitorBrands"),
    )


_worker: DiagnoseWorker | None = None


async def start_worker(settings: Settings | None = None) -> DiagnoseWorker | None:
    global _worker
    cfg = settings or get_settings()
    if not cfg.diagnose_worker_enabled or not cfg.rabbitmq_url:
        return None
    _worker = DiagnoseWorker(cfg)
    await _worker.start()
    return _worker


async def stop_worker() -> None:
    global _worker
    if _worker:
        await _worker.stop()
        _worker = None
