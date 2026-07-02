"""RabbitMQ consumer for ai.breakdown queue (EPIC-5 M1)."""

from __future__ import annotations

import json
import logging
from typing import Any

import aio_pika
import httpx

from app.config import Settings, get_settings
from app.services import breakdown_service

logger = logging.getLogger(__name__)

QUEUE_NAME = "ai.breakdown"
DLQ_NAME = "ai.breakdown.dlq"
MAX_RETRIES = 3
RETRY_HEADER = "x-retry-count"


class BreakdownWorker:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self._connection: aio_pika.RobustConnection | None = None

    @property
    def callback_url(self) -> str:
        base = (self.settings.core_callback_base_url or "").rstrip("/")
        return f"{base}/api/v1/internal/materials/breakdown-callback"

    async def start(self) -> None:
        if not self.settings.rabbitmq_url:
            logger.warning("RABBITMQ_URL not set — breakdown worker disabled")
            return
        self._connection = await aio_pika.connect_robust(self.settings.rabbitmq_url)
        channel = await self._connection.channel()
        await channel.set_qos(prefetch_count=1)
        await channel.declare_queue(DLQ_NAME, durable=True)
        queue = await channel.declare_queue(QUEUE_NAME, durable=True)
        await queue.consume(self._on_message)
        logger.info("Breakdown worker consuming queue=%s callback=%s", QUEUE_NAME, self.callback_url)

    async def stop(self) -> None:
        if self._connection and not self._connection.is_closed:
            await self._connection.close()
        self._connection = None

    async def _on_message(self, message: aio_pika.IncomingMessage) -> None:
        retry_count = int(message.headers.get(RETRY_HEADER, 0) if message.headers else 0)
        payload = json.loads(message.body.decode())
        breakdown_id = int(payload["breakdownId"])
        trace_id = payload.get("trace_id", "")
        try:
            logger.info(
                "breakdown worker breakdown_id=%s material_id=%s retry=%s",
                breakdown_id,
                payload.get("materialId"),
                retry_count,
            )
            frames, analyze = await breakdown_service.run_breakdown_job(
                source_url=str(payload["sourceUrl"]),
                title=payload.get("title"),
                tenant_id=_optional_int(payload.get("tenantId")),
                project_id=_optional_int(payload.get("projectId")),
                material_id=_optional_int(payload.get("materialId")),
                settings=self.settings,
            )
            await self._post_callback(
                trace_id=trace_id,
                breakdown_id=breakdown_id,
                status="SUCCESS",
                frames=[frame.model_dump(by_alias=True) for frame in frames],
                dimensions=analyze.dimensions,
                reusable_structure=analyze.reusable_structure,
                needs_human_review=analyze.needs_human_review,
            )
            await message.ack()
        except Exception as exc:
            logger.exception("breakdown worker failed breakdown_id=%s retry=%s: %s", breakdown_id, retry_count, exc)
            if retry_count >= MAX_RETRIES:
                await self._post_callback(
                    trace_id=trace_id,
                    breakdown_id=breakdown_id,
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
        breakdown_id: int,
        status: str,
        frames: list[dict[str, Any]] | None = None,
        dimensions: dict[str, str] | None = None,
        reusable_structure: str | None = None,
        needs_human_review: bool | None = None,
        error_message: str | None = None,
    ) -> None:
        if not self.settings.core_callback_base_url:
            logger.warning("CORE_CALLBACK_BASE_URL not set — skipping breakdown callback")
            return
        body: dict[str, Any] = {
            "traceId": trace_id,
            "breakdownId": breakdown_id,
            "status": status,
        }
        if status == "SUCCESS":
            body["frames"] = frames or []
            body["dimensions"] = dimensions or {}
            body["reusableStructure"] = reusable_structure or ""
            body["needsHumanReview"] = True if needs_human_review is None else needs_human_review
        else:
            body["errorMessage"] = error_message

        headers = {
            "Authorization": f"Bearer {self.settings.ai_service_internal_token}",
            "Content-Type": "application/json",
        }
        if trace_id:
            headers["X-Trace-Id"] = trace_id
        async with httpx.AsyncClient(timeout=60.0) as client:
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
        logger.error("breakdown message moved to DLQ after %s retries", retry_count)


def _optional_int(value: Any) -> int | None:
    if value is None:
        return None
    return int(value)


_worker: BreakdownWorker | None = None


async def start_worker(settings: Settings | None = None) -> BreakdownWorker | None:
    global _worker
    cfg = settings or get_settings()
    if not cfg.breakdown_worker_enabled or not cfg.rabbitmq_url:
        return None
    _worker = BreakdownWorker(cfg)
    await _worker.start()
    return _worker


async def stop_worker() -> None:
    global _worker
    if _worker:
        await _worker.stop()
        _worker = None
