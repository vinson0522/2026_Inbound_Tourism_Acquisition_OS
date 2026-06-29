import pytest

from app.config import get_settings


@pytest.fixture(autouse=True)
def reset_settings(monkeypatch):
    monkeypatch.setenv("AI_SERVICE_INTERNAL_TOKEN", "test_internal_token")
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    monkeypatch.delenv("PERPLEXITY_API_KEY", raising=False)
    get_settings.cache_clear()
    yield
    get_settings.cache_clear()
