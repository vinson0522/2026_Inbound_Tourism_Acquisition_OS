from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_version: str = "0.1.0"
    ai_service_internal_token: str = "dev_internal_token_change_me"

    database_url: str | None = None

    openai_api_key: str | None = None
    openai_api_base: str | None = None
    gemini_api_key: str | None = None
    perplexity_api_key: str | None = None

    langfuse_public_key: str | None = None
    langfuse_secret_key: str | None = None
    langfuse_host: str | None = None

    rabbitmq_url: str | None = None
    core_callback_base_url: str | None = None
    diagnose_worker_enabled: bool = False
    # Pipeline E2E when provider keys/quota unavailable (never use in production)
    diagnose_mock_llm: bool = False

    embedding_model: str = "openai/text-embedding-3-small"
    chunk_size: int = 512
    chunk_overlap: int = 64
    embed_worker_enabled: bool = False
    embed_mock: bool = False

    reranker_model: str = "BAAI/bge-reranker-v2-m3"
    reranker_mock: bool = False
    rag_vector_top_k: int = 20
    rag_rerank_top_k: int = 3

    minio_endpoint: str | None = None
    minio_access_key: str | None = None
    minio_secret_key: str | None = None
    minio_bucket: str = "inbound-growth"

    keywords_mock_llm: bool = False
    keywords_model: str = "openai/gpt-4o-mini"
    keywords_template_name: str = "keyword_generate_v1"
    keywords_prompt_fallback: str = (
        "Generate inbound tourism keyword opportunities as JSON only. "
        "Follow the user message schema; one entry per requested lifecycle stage."
    )

    content_mock_llm: bool = False
    content_model: str = "openai/gpt-4o-mini"
    content_template_name: str = "content_script_v1"
    content_prompt_fallback: str = (
        "You generate inbound tourism short-form video scripts as JSON only. "
        "Include hook, script, voiceover, on_screen_text, cta, and storyboard scenes "
        "with scene, duration, visual, note. Set needs_human_review true; cite chunk_id when using RAG."
    )

    landing_mock_llm: bool = False
    landing_model: str = "openai/gpt-4o-mini"
    landing_template_name: str = "landing_generate_v1"
    landing_prompt_fallback: str = (
        "You generate inbound tourism English landing pages as JSON only. "
        "Return title, content_json.modules (keys: hero, why_this_trip, itinerary, what_we_provide, "
        "traveler_reviews, faq, lead_form, whatsapp_cta), seo_meta_json, and form_config_json. "
        "Set needs_human_review true; cite chunk_ids when using RAG."
    )

    @property
    def has_embedding_key(self) -> bool:
        return bool(self.openai_api_key)

    @property
    def has_llm_key(self) -> bool:
        return bool(self.openai_api_key or self.gemini_api_key or self.perplexity_api_key)

    @property
    def litellm_status(self) -> str:
        return "ready" if self.has_llm_key else "no_key"


@lru_cache
def get_settings() -> Settings:
    return Settings()
