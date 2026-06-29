"""Tests for Perplexity citation parsing."""

from app.models.diagnostic import ParseCitationsRequest
from app.services.citation_parser import parse_citations, parse_perplexity

PERPLEXITY_FIXTURE = {
    "choices": [
        {
            "message": {
                "content": "China Highlights is a top agency. Trip.com also offers tours.",
            }
        }
    ],
    "citations": [
        "https://www.chinahighlights.com/tours",
        "https://www.trip.com/china-tours",
    ],
}


def test_parse_perplexity_citations_and_brands():
    result = parse_perplexity(
        PERPLEXITY_FIXTURE,
        customer_brand="China Highlights",
        competitor_brands=["Trip.com"],
    )
    assert len(result.citations) == 2
    assert result.citations[0].url == "https://www.chinahighlights.com/tours"
    assert result.citations[0].rank == 1
    assert result.citations[0].domain == "chinahighlights.com"
    assert "China Highlights" in result.mentioned_brands
    assert result.rank == 1
    assert result.answer_text.startswith("China Highlights")


def test_parse_citations_endpoint_model():
    req = ParseCitationsRequest(
        platform="perplexity",
        raw_response_json=PERPLEXITY_FIXTURE,
        customer_brand="China Highlights",
        competitor_brands=["Trip.com"],
    )
    result = parse_citations(req)
    assert len(result.citations) == 2
    assert result.citations[1].is_competitor is True


def test_parse_citations_hidden_params_fallback():
    raw = {
        "choices": [{"message": {"content": "Answer"}}],
        "hidden_params": {"citations": ["https://example.com/page"]},
    }
    result = parse_perplexity(raw)
    assert len(result.citations) == 1
    assert result.citations[0].domain == "example.com"
