"""RabbitMQ consumer for ai.embed queue."""

from __future__ import annotations

import asyncio
import json
import logging
from typing import Any

import aio_pika

from app.config import Settings, get_settings
from app.models.embed import EmbedRequest
from app.services import embed_service

logger = logging.getLogger(__name__)

QUEUE_NAME = "ai.embed"
DLQ_NAME = "ai.embed.dlq"
MAX_RETRIES = 3
RETRY_HEADER = "x-retry-count"


class EmbedWorker:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self._connection: aio_pika.RobustConnection | None = None

    async def start(self) -> None:
        if not self.settings.rabbitmq_url:
            logger.warning("RABBITMQ_URL not set — embed worker disabled")
            return
        if not self.settings.database_url:
            logger.warning("DATABASE_URL not set — embed worker disabled")
            return
        self._connection = await aio_pika.connect_robust(self.settings.rabbitmq_url)
        channel = await self._connection.channel()
        await channel.set_qos(prefetch_count=1)
        await channel.declare_queue(DLQ_NAME, durable=True)
        queue = await channel.declare_queue(QUEUE_NAME, durable=True)
        await queue.consume(self._on_message)
        logger.info("Embed worker consuming queue=%s", QUEUE_NAME)

    async def stop(self) -> None:
        if self._connection and not self._connection.is_closed:
            await self._connection.close()
        self._connection = None

    async def _on_message(self, message: aio_pika.IncomingMessage) -> None:
        retry_count = int(message.headers.get(RETRY_HEADER, 0) if message.headers else 0)
        try:
            payload = json.loads(message.body.decode())
            req = _payload_to_embed_request(payload)
            logger.info(
                "embed worker asset_id=%s tenant_id=%s project_id=%s retry=%s",
                req.asset_id,
                req.tenant_id,
                req.project_id,
                retry_count,
            )
            await embed_service.run_embed(req, settings=self.settings)
            await message.ack()
        except Exception as exc:
            logger.exception("embed worker failed retry=%s: %s", retry_count, exc)
            if retry_count >= MAX_RETRIES:
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
        logger.error("embed message moved to DLQ after %s retries", retry_count)


def _payload_to_embed_request(payload: dict[str, Any]) -> EmbedRequest:
    return EmbedRequest(
        trace_id=payload.get("trace_id"),
        asset_id=int(payload.get("assetId") or payload["asset_id"]),
        tenant_id=int(payload.get("tenantId") or payload["tenant_id"]),
        project_id=int(payload.get("projectId") or payload["project_id"]),
        file_url=payload.get("fileUrl") or payload.get("file_url"),
    )


_worker: EmbedWorker | None = None


async def start_worker(settings: Settings | None = None) -> EmbedWorker | None:
    global _worker
    cfg = settings or get_settings()
    if not cfg.embed_worker_enabled or not cfg.rabbitmq_url:
        return None
    _worker = EmbedWorker(cfg)
    await _worker.start()
    return _worker


async def stop_worker() -> None:
    global _worker
    if _worker:
        await _worker.stop()
        _worker = None
