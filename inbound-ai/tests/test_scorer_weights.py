"""Tests for GEO score aggregation with configurable weights."""

from app.models.diagnostic import ResultMetricSummary, ScoreRequest
from app.services.scorer import compute_geo_score, normalize_weights

PRD_WEIGHTS = {
    "brand_mention_rate": 0.25,
    "top3_rate": 0.20,
    "competitor_suppression": 0.15,
    "citation_coverage": 0.15,
    "longtail_coverage": 0.15,
    "asset_completeness": 0.10,
}


def test_normalize_weights_from_custom_json():
    custom = {
        "brand_mention_rate": 0.5,
        "top3_rate": 0.5,
        "competitor_suppression": 0.0,
        "citation_coverage": 0.0,
        "longtail_coverage": 0.0,
        "asset_completeness": 0.0,
    }
    weights = normalize_weights(custom)
    assert abs(weights["brand_mention_rate"] - 0.5) < 0.001
    assert abs(weights["top3_rate"] - 0.5) < 0.001


def test_compute_geo_score_uses_request_weights_not_hardcoded():
    results = [
        ResultMetricSummary(
            question_id=1,
            brand_mentioned=True,
            in_top3=True,
            competitor_dominance=0.0,
            citation_hit=True,
            is_longtail=False,
            asset_complete=1.0,
        ),
        ResultMetricSummary(
            question_id=2,
            brand_mentioned=False,
            in_top3=False,
            competitor_dominance=1.0,
            citation_hit=False,
            is_longtail=True,
            asset_complete=0.5,
        ),
    ]
    req = ScoreRequest(run_id=1, metric_weights_json=PRD_WEIGHTS, results=results)
    score = compute_geo_score(req)
    assert 0 <= score.geo_score <= 100
    assert score.metric_weights == normalize_weights(PRD_WEIGHTS)
    assert "brand_mention_rate" in score.metrics


def test_perfect_score_when_all_metrics_max():
    results = [
        ResultMetricSummary(
            question_id=1,
            brand_mentioned=True,
            in_top3=True,
            competitor_dominance=0.0,
            citation_hit=True,
            is_longtail=True,
            asset_complete=1.0,
        ),
    ]
    req = ScoreRequest(run_id=1, metric_weights_json=PRD_WEIGHTS, results=results)
    score = compute_geo_score(req)
    assert score.geo_score == 100.0
