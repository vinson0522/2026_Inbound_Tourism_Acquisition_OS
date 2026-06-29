"""GEO score aggregation — weights from request, not hardcoded."""

from __future__ import annotations

from app.models.diagnostic import (
    PRD_DEFAULT_METRIC_WEIGHTS,
    ResultMetricSummary,
    ScoreRequest,
    ScoreResultData,
)

METRIC_KEYS = (
    "brand_mention_rate",
    "top3_rate",
    "competitor_suppression",
    "citation_coverage",
    "longtail_coverage",
    "asset_completeness",
)


def normalize_weights(metric_weights_json: dict[str, float] | None) -> dict[str, float]:
    source = metric_weights_json or PRD_DEFAULT_METRIC_WEIGHTS
    weights = {k: float(source.get(k, PRD_DEFAULT_METRIC_WEIGHTS[k])) for k in METRIC_KEYS}
    total = sum(weights.values())
    if total <= 0:
        return dict(PRD_DEFAULT_METRIC_WEIGHTS)
    return {k: v / total for k, v in weights.items()}


def aggregate_metrics(results: list[ResultMetricSummary]) -> dict[str, float]:
    if not results:
        return dict.fromkeys(METRIC_KEYS, 0.0)

    n = len(results)
    return {
        "brand_mention_rate": sum(1.0 if r.brand_mentioned else 0.0 for r in results) / n,
        "top3_rate": sum(1.0 if r.in_top3 else 0.0 for r in results) / n,
        "competitor_suppression": sum(r.competitor_dominance for r in results) / n,
        "citation_coverage": sum(1.0 if r.citation_hit else 0.0 for r in results) / n,
        "longtail_coverage": sum(1.0 if r.is_longtail else 0.0 for r in results) / n,
        "asset_completeness": sum(r.asset_complete for r in results) / n,
    }


def compute_geo_score(request: ScoreRequest) -> ScoreResultData:
    weights = normalize_weights(request.metric_weights_json)
    metrics = aggregate_metrics(request.results)

    weighted = (
        weights["brand_mention_rate"] * metrics["brand_mention_rate"]
        + weights["top3_rate"] * metrics["top3_rate"]
        + weights["competitor_suppression"] * (1.0 - min(max(metrics["competitor_suppression"], 0.0), 1.0))
        + weights["citation_coverage"] * metrics["citation_coverage"]
        + weights["longtail_coverage"] * metrics["longtail_coverage"]
        + weights["asset_completeness"] * metrics["asset_completeness"]
    )
    geo_score = round(100.0 * weighted, 2)

    uncovered = request.longtail_question_ids or [
        r.question_id for r in request.results if r.is_longtail and not r.brand_mentioned
    ]

    return ScoreResultData(
        geo_score=geo_score,
        metrics=metrics,
        metric_weights=weights,
        uncovered_questions=uncovered,
    )
