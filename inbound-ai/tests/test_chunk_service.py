from app.services.chunk_service import chunk_text, count_tokens


def test_count_tokens_non_empty():
    assert count_tokens("hello world") >= 2


def test_single_short_chunk():
    text = "Dragon Journey Travel offers private China tours."
    chunks = chunk_text(text, chunk_size=512, chunk_overlap=64)
    assert len(chunks) == 1
    assert chunks[0] == text


def test_long_text_produces_multiple_chunks():
    words = ["word"] * 800
    text = " ".join(words)
    chunks = chunk_text(text, chunk_size=128, chunk_overlap=16)
    assert len(chunks) >= 2
    assert all(len(c) > 0 for c in chunks)


def test_overlap_preserves_context():
    text = " ".join(f"token{i}" for i in range(300))
    chunks = chunk_text(text, chunk_size=64, chunk_overlap=8)
    assert chunks[0].split()[0] == "token0"
    assert chunks[-1].split()[-1] == "token299"
