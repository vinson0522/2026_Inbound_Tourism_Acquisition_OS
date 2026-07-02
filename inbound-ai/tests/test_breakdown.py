"""Tests for POST /ai/breakdown/* (FR-402/403)."""

import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.models.breakdown import (
    DIMENSION_KEYS,
    MOCK_FRAME_COUNT,
    BreakdownAnalyzeRequest,
    BreakdownExtractRequest,
    BreakdownFrame,
)
from app.services import breakdown_service

TOKEN = "test_internal_token"
SOURCE_URL = "http://127.0.0.1:9000/ruoyi/1/1/materials/demo-viral.mp4"


@pytest.mark.asyncio
async def test_extract_frames_mock_returns_six_frames():
    settings = Settings(ai_service_internal_token=TOKEN, breakdown_mock_llm=True)
    req = BreakdownExtractRequest(sourceUrl=SOURCE_URL, tenantId=1, projectId=1)
    data = await breakdown_service.extract_frames(req, settings=settings)
    assert data.capture_method == "breakdown-mock"
    assert len(data.frames) == MOCK_FRAME_COUNT
    assert data.frames[0].timestamp == 0
    assert data.frames[0].timestamp_label == "0:00"
    assert data.frames[0].thumbnail_url
    assert data.frames[0].caption


@pytest.mark.asyncio
async def test_analyze_mock_returns_seven_dimensions():
    settings = Settings(ai_service_internal_token=TOKEN, breakdown_mock_llm=True)
    frames = [
        BreakdownFrame(
            timestamp=0,
            timestampLabel="0:00",
            thumbnailUrl="https://placehold.co/120x68/png?text=1",
            caption="Opening skyline hook",
        ),
        BreakdownFrame(
            timestamp=5,
            timestampLabel="0:05",
            thumbnailUrl="https://placehold.co/120x68/png?text=2",
            caption="Traveler POV street scene",
        ),
    ]
    req = BreakdownAnalyzeRequest(
        tenantId=1,
        projectId=1,
        title="China Highlights reel",
        frames=frames,
    )
    data = await breakdown_service.analyze(req, settings=settings)
    assert data.capture_method == "breakdown-mock"
    assert data.needs_human_review is True
    assert data.reusable_structure
    for key in DIMENSION_KEYS:
        assert key in data.dimensions
        assert data.dimensions[key]


def test_extract_endpoint_requires_token():
    app = create_app()
    client = TestClient(app)
    resp = client.post(
        "/ai/breakdown/extract-frames",
        json={"sourceUrl": SOURCE_URL},
    )
    assert resp.status_code == 401


def test_breakdown_endpoints_mock_success():
    app = create_app()
    client = TestClient(app)
    headers = {"Authorization": f"Bearer {TOKEN}"}

    extract_resp = client.post(
        "/ai/breakdown/extract-frames",
        json={"sourceUrl": SOURCE_URL, "tenantId": 1, "projectId": 1},
        headers=headers,
    )
    assert extract_resp.status_code == 200
    extract_body = extract_resp.json()
    assert extract_body["code"] == 0
    frames = extract_body["data"]["frames"]
    assert len(frames) == MOCK_FRAME_COUNT

    analyze_resp = client.post(
        "/ai/breakdown/analyze",
        json={
            "tenantId": 1,
            "projectId": 1,
            "title": "Smoke viral clip",
            "frames": frames[:2],
        },
        headers=headers,
    )
    assert analyze_resp.status_code == 200
    analyze_body = analyze_resp.json()
    assert analyze_body["code"] == 0
    data = analyze_body["data"]
    assert data["needs_human_review"] is True
    assert data["reusable_structure"]
    assert "theme" in data["dimensions"]
