"""Unified citations parsing for GEO diagnostic platforms."""

from __future__ import annotations

import re
from urllib.parse import urlparse

from app.models.diagnostic import CitationItem, ParseCitationsRequest, ParseCitationsResult


def _domain(url: str) -> str:
    try:
        host = urlparse(url).netloc or urlparse(f"https://{url}").netloc
        return host.lower().removeprefix("www.")
    except Exception:
        return ""


def _match_brand(name: str, candidates: list[str] | None) -> bool:
    if not candidates:
        return False
    lower = name.lower()
    return any(c.lower() in lower or lower in c.lower() for c in candidates if c)


def parse_perplexity(
    raw: dict,
    customer_brand: str | None = None,
    competitor_brands: list[str] | None = None,
) -> ParseCitationsResult:
    """Parse Perplexity / LiteLLM response — citations[] + answer text."""
    answer = ""
    choices = raw.get("choices") or []
    if choices:
        answer = (choices[0].get("message") or {}).get("content") or ""

    urls: list[str] = list(raw.get("citations") or [])
    if not urls:
        hidden = raw.get("hidden_params") or {}
        urls = list(hidden.get("citations") or [])

    citations: list[CitationItem] = []
    for idx, url in enumerate(urls, start=1):
        domain = _domain(url)
        title = domain or url
        is_customer = _match_brand(title, [customer_brand] if customer_brand else None)
        is_competitor = _match_brand(title, competitor_brands)
        citations.append(
            CitationItem(
                url=url,
                title=title,
                domain=domain,
                rank=idx,
                is_customer=is_customer,
                is_competitor=is_competitor,
            )
        )

    mentioned = _extract_mentioned_brands(answer, customer_brand, competitor_brands)
    competitors = [b for b in mentioned if _match_brand(b, competitor_brands)]
    if customer_brand and customer_brand.lower() in answer.lower() and customer_brand not in mentioned:
        mentioned.insert(0, customer_brand)

    rank = _estimate_rank(answer, customer_brand, mentioned)

    return ParseCitationsResult(
        citations=citations,
        mentioned_brands=mentioned,
        competitors=competitors,
        rank=rank,
        answer_text=answer,
    )


def _extract_mentioned_brands(
    answer: str,
    customer_brand: str | None,
    competitor_brands: list[str] | None,
) -> list[str]:
    found: list[str] = []
    candidates = list(competitor_brands or [])
    if customer_brand:
        candidates.append(customer_brand)
    for name in candidates:
        if name and name.lower() in answer.lower() and name not in found:
            found.append(name)

    for match in re.findall(r"\b([A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)+)\b", answer):
        if match not in found and len(match) < 60:
            found.append(match)
    return found[:20]


def _estimate_rank(answer: str, customer_brand: str | None, mentioned: list[str]) -> int | None:
    if not customer_brand:
        return None
    lower_answer = answer.lower()
    brand_lower = customer_brand.lower()
    if brand_lower not in lower_answer:
        return None
    # Rank among mentioned brands by first occurrence in answer text
    positions = []
    for name in mentioned:
        idx = lower_answer.find(name.lower())
        if idx >= 0:
            positions.append((idx, name))
    if not positions:
        return 1
    positions.sort(key=lambda x: x[0])
    for rank, (_, name) in enumerate(positions, start=1):
        if name.lower() == brand_lower:
            return rank
    return 1


def parse_gemini(
    raw: dict,
    customer_brand: str | None = None,
    competitor_brands: list[str] | None = None,
) -> ParseCitationsResult:
    """Parse Gemini / LiteLLM response — groundingMetadata + answer text."""
    answer = ""
    choices = raw.get("choices") or []
    if choices:
        answer = (choices[0].get("message") or {}).get("content") or ""

    urls: list[str] = []
    grounding = raw.get("vertex_ai_grounding_metadata") or raw.get("grounding_metadata") or {}
    chunks = grounding.get("groundingChunks") or grounding.get("grounding_chunks") or []
    for chunk in chunks:
        web = chunk.get("web") or {}
        uri = web.get("uri") or web.get("url")
        if uri:
            urls.append(uri)

    if not urls and choices:
        provider_fields = (choices[0].get("message") or {}).get("provider_specific_fields") or {}
        meta = provider_fields.get("groundingMetadata") or provider_fields.get("grounding_metadata") or {}
        for chunk in meta.get("groundingChunks") or meta.get("grounding_chunks") or []:
            web = chunk.get("web") or {}
            uri = web.get("uri") or web.get("url")
            if uri:
                urls.append(uri)

    citations: list[CitationItem] = []
    for idx, url in enumerate(urls, start=1):
        domain = _domain(url)
        title = domain or url
        is_customer = _match_brand(title, [customer_brand] if customer_brand else None)
        is_competitor = _match_brand(title, competitor_brands)
        citations.append(
            CitationItem(
                url=url,
                title=title,
                domain=domain,
                rank=idx,
                is_customer=is_customer,
                is_competitor=is_competitor,
            )
        )

    mentioned = _extract_mentioned_brands(answer, customer_brand, competitor_brands)
    competitors = [b for b in mentioned if _match_brand(b, competitor_brands)]
    if customer_brand and customer_brand.lower() in answer.lower() and customer_brand not in mentioned:
        mentioned.insert(0, customer_brand)

    rank = _estimate_rank(answer, customer_brand, mentioned)

    return ParseCitationsResult(
        citations=citations,
        mentioned_brands=mentioned,
        competitors=competitors,
        rank=rank,
        answer_text=answer,
    )


def parse_citations(request: ParseCitationsRequest) -> ParseCitationsResult:
    platform = request.platform.lower()
    if platform in ("perplexity", "pplx"):
        return parse_perplexity(
            request.raw_response_json,
            customer_brand=request.customer_brand,
            competitor_brands=request.competitor_brands,
        )
    if platform in ("gemini", "google"):
        return parse_gemini(
            request.raw_response_json,
            customer_brand=request.customer_brand,
            competitor_brands=request.competitor_brands,
        )
    raise ValueError(f"Unsupported platform for citation parsing: {request.platform}")
