from fastapi.testclient import TestClient

from app.main import create_app

TOKEN = "test_internal_token"


def test_public_health():
    client = TestClient(create_app())
    resp = client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert body["service"] == "inbound-ai"
    assert body["version"] == "0.1.0"


def test_ai_health_requires_token():
    client = TestClient(create_app())
    resp = client.get("/ai/health")
    assert resp.status_code == 401


def test_ai_health_with_bearer():
    client = TestClient(create_app())
    resp = client.get("/ai/health", headers={"Authorization": f"Bearer {TOKEN}"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 0
    assert body["data"]["status"] == "ok"
    assert body["data"]["litellm"] == "no_key"
    assert body["data"]["db"] == "skipped"
    assert body["trace_id"]


def test_ai_health_with_x_internal_token():
    client = TestClient(create_app())
    resp = client.get("/ai/health", headers={"X-Internal-Token": TOKEN})
    assert resp.status_code == 200
    assert resp.json()["code"] == 0
