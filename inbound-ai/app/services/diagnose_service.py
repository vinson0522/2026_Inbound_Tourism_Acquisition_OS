"""GEO grounded-api diagnose orchestration."""

from __future__ import annotations

import logging
from datetime import UTC, datetime

from fastapi import HTTPException, status

from app.config import Settings, get_settings
from app.models.diagnostic import CitationItem, DiagnoseRequest, DiagnoseResultData
from app.services import citation_parser, llm_gateway
from app.services.llm_provider import api_key_for_model, resolve_provider

logger = logging.getLogger(__name__)


def _mock_diagnose_result(request: DiagnoseRequest) -> DiagnoseResultData:
    brand = request.customer_brand or "Dragon Journey Travel"
    answer = (
        f"For first-time visitors to China, {brand} offers well-reviewed private tours. "
        "Other options include China Highlights and WildChina."
    )
    return DiagnoseResultData(
        answer_text=answer,
        model=request.model,
        platform=request.platform,
        probe_mode=request.probe_mode,
        mentioned_brands=[brand, "China Highlights"],
        competitors=["WildChina"],
        citations=[
            CitationItem(
                url="https://demo-dragonjourney.com/tours",
                title=f"{brand} Private Tours",
                domain="demo-dragonjourney.com",
                rank=1,
                is_customer=True,
                is_competitor=False,
            ),
            CitationItem(
                url="https://www.chinahighlights.com/",
                title="China Highlights",
                domain="chinahighlights.com",
                rank=2,
                is_customer=False,
                is_competitor=True,
            ),
        ],
        rank=1,
        capture_method="grounded-api-mock",
        raw_response_json={"mock": True, "model": request.model},
        sampled_at=datetime.now(UTC),
        run_id=request.run_id,
        question_id=request.question_id,
        tenant_id=request.tenant_id,
        project_id=request.project_id,
        sample_index=request.sample_index,
        probe_task_id=request.probe_task_id,
    )


async def run_diagnose(
    request: DiagnoseRequest,
    settings: Settings | None = None,
) -> DiagnoseResultData:
    cfg = settings or get_settings()
    llm_gateway.validate_probe_config(request.probe_mode, request.grounding_enabled)

    if cfg.diagnose_mock_llm:
        logger.warning("DIAGNOSE_MOCK_LLM enabled — returning canned grounded-api result")
        return _mock_diagnose_result(request)

    if not api_key_for_model(request.model, cfg):
        provider = resolve_provider(request.model)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"API key required for {provider} model {request.model}",
        )

    messages = [{"role": "user", "content": request.question}]
    extra: dict | None = None
    if resolve_provider(request.model) == "gemini" and request.grounding_enabled:
        extra = {"tools": [{"googleSearch": {}}]}
    logger.info(
        "diagnose run_id=%s question_id=%s platform=%s model=%s sample=%s",
        request.run_id,
        request.question_id,
        request.platform,
        request.model,
        request.sample_index,
    )

    response = await llm_gateway.acompletion(
        request.model,
        messages,
        cfg,
        max_tokens=2048,
        temperature=0.2,
        extra=extra,
    )
    raw = llm_gateway.response_to_dict(response)
    answer = response.choices[0].message.content or ""

    platform = request.platform.lower()
    if platform in ("gemini", "google"):
        parsed = citation_parser.parse_gemini(
            raw,
            customer_brand=request.customer_brand,
            competitor_brands=request.competitor_brands,
        )
    else:
        parsed = citation_parser.parse_perplexity(
            raw,
            customer_brand=request.customer_brand,
            competitor_brands=request.competitor_brands,
        )

    return DiagnoseResultData(
        answer_text=answer or parsed.answer_text,
        model=response.model or request.model,
        platform=request.platform,
        probe_mode=request.probe_mode,
        mentioned_brands=parsed.mentioned_brands,
        competitors=parsed.competitors,
        citations=parsed.citations,
        rank=parsed.rank,
        capture_method="grounded-api",
        raw_response_json=raw,
        sampled_at=datetime.now(UTC),
        run_id=request.run_id,
        question_id=request.question_id,
        tenant_id=request.tenant_id,
        project_id=request.project_id,
        sample_index=request.sample_index,
        probe_task_id=request.probe_task_id,
    )
