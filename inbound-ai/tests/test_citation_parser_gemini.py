"""Tests for Gemini citation parsing."""

from app.services.citation_parser import parse_gemini

GEMINI_FIXTURE = {
    "choices": [
        {
            "message": {
                "content": "Dragon Journey Travel offers private China tours. China Highlights is also popular.",
            }
        }
    ],
    "vertex_ai_grounding_metadata": {
        "groundingChunks": [
            {"web": {"uri": "https://demo-dragonjourney.com/tours", "title": "Dragon Journey"}},
            {"web": {"uri": "https://www.chinahighlights.com/", "title": "China Highlights"}},
        ]
    },
}


def test_parse_gemini_grounding_chunks():
    result = parse_gemini(
        GEMINI_FIXTURE,
        customer_brand="Dragon Journey Travel",
        competitor_brands=["China Highlights"],
    )
    assert len(result.citations) == 2
    assert result.citations[0].domain == "demo-dragonjourney.com"
    assert "Dragon Journey Travel" in result.mentioned_brands
    assert result.rank == 1
