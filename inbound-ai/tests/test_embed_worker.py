import json

import pytest

from app.workers.embed_worker import (
    DLQ_NAME,
    MAX_RETRIES,
    QUEUE_NAME,
    RETRY_HEADER,
    _payload_to_embed_request,
)


def test_embed_queue_constants():
    assert QUEUE_NAME == "ai.embed"
    assert DLQ_NAME == "ai.embed.dlq"
    assert MAX_RETRIES == 3


def test_payload_to_embed_request_camel_case():
    payload = {
        "trace_id": "t1",
        "assetId": 10,
        "tenantId": 1,
        "projectId": 2,
        "fileUrl": "minio://inbound-growth/1/2/doc.pdf",
    }
    req = _payload_to_embed_request(payload)
    assert req.asset_id == 10
    assert req.tenant_id == 1
    assert req.project_id == 2
    assert req.file_url == "minio://inbound-growth/1/2/doc.pdf"


def test_payload_to_embed_request_snake_case():
    payload = {
        "asset_id": 3,
        "tenant_id": 1,
        "project_id": 1,
        "file_url": "/data/sample.pdf",
    }
    req = _payload_to_embed_request(payload)
    assert req.asset_id == 3
    assert req.file_url == "/data/sample.pdf"


def test_dlq_envelope_shape():
    """Document expected DLQ message envelope written by EmbedWorker._publish_dlq."""
    body = json.dumps({"assetId": 1, "tenantId": 1, "projectId": 1}).encode()
    envelope = {
        "original_body": json.loads(body.decode()),
        "retry_count": MAX_RETRIES,
        "error": "docling failed",
    }
    assert envelope["retry_count"] >= MAX_RETRIES
    assert RETRY_HEADER == "x-retry-count"
