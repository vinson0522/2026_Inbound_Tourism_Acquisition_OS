"""Fetch knowledge asset bytes from HTTP(S), MinIO, or local paths."""

from __future__ import annotations

import logging
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlparse

import httpx

from app.config import Settings, get_settings

logger = logging.getLogger(__name__)


class FileFetchError(Exception):
    pass


async def fetch_file_bytes(file_url: str, settings: Settings | None = None) -> tuple[bytes, str]:
    """Return raw bytes and a suffix hint (e.g. ``.pdf``)."""
    cfg = settings or get_settings()
    url = file_url.strip()
    if not url:
        raise FileFetchError("empty file_url")

    if url.startswith("minio://"):
        return _fetch_minio_uri(url, cfg)

    parsed = urlparse(url)
    if parsed.scheme in ("", "file"):
        path = _local_path_from_url(url)
        return _read_local(path)

    if parsed.scheme in ("http", "https"):
        if _is_minio_http_url(parsed, cfg):
            return _fetch_minio_http(parsed, cfg)
        return await _fetch_http(url)

    if _looks_like_local_path(url):
        return _read_local(Path(url))

    raise FileFetchError(f"unsupported file_url scheme: {url[:64]}")


def _local_path_from_url(url: str) -> Path:
    parsed = urlparse(url)
    if parsed.scheme == "file":
        raw = unquote(parsed.path)
        # Windows file:///D:/... → strip leading slash before drive letter
        if len(raw) >= 3 and raw[0] == "/" and raw[2] == ":":
            raw = raw[1:]
        return Path(raw)
    return Path(url)


def _looks_like_local_path(url: str) -> bool:
    if url.startswith(("/", "\\")):
        return True
    return len(url) > 2 and url[1] == ":" and url[0].isalpha()


def _read_local(path: Path) -> tuple[bytes, str]:
    if not path.is_file():
        raise FileFetchError(f"local file not found: {path}")
    suffix = path.suffix.lower() or ".txt"
    return path.read_bytes(), suffix


async def _fetch_http(url: str) -> tuple[bytes, str]:
    async with httpx.AsyncClient(timeout=120.0, follow_redirects=True) as client:
        resp = await client.get(url)
        resp.raise_for_status()
        content_type = resp.headers.get("content-type", "")
        suffix = _guess_suffix_from_url(url, content_type)
        return resp.content, suffix


def _is_minio_http_url(parsed, cfg: Settings) -> bool:
    if not cfg.minio_endpoint:
        return False
    minio = urlparse(cfg.minio_endpoint)
    return parsed.netloc == minio.netloc or parsed.netloc.startswith(f"{cfg.minio_bucket}.")


def _fetch_minio_uri(url: str, cfg: Settings) -> tuple[bytes, str]:
    # minio://bucket/object/key
    rest = url[len("minio://") :]
    parts = rest.split("/", 1)
    if len(parts) != 2:
        raise FileFetchError(f"invalid minio uri: {url}")
    bucket, object_key = parts[0], parts[1]
    return _minio_get_object(bucket, object_key, cfg)


def _fetch_minio_http(parsed, cfg: Settings) -> tuple[bytes, str]:
    path = unquote(parsed.path.lstrip("/"))
    parts = path.split("/", 1)
    if len(parts) != 2:
        raise FileFetchError(f"cannot parse minio object from url path: {parsed.path}")
    bucket, object_key = parts[0], parts[1]
    if bucket != cfg.minio_bucket and cfg.minio_bucket:
        # path-style URL may use configured default bucket when key is tenant/project/...
        object_key = path
        bucket = cfg.minio_bucket
    suffix = PurePosixPath(object_key).suffix.lower() or ".bin"
    data = _minio_get_object(bucket, object_key, cfg)[0]
    return data, suffix


def _minio_get_object(bucket: str, object_key: str, cfg: Settings) -> tuple[bytes, str]:
    if not cfg.minio_endpoint or not cfg.minio_access_key or not cfg.minio_secret_key:
        raise FileFetchError("MINIO_ENDPOINT / access credentials not configured")
    try:
        from minio import Minio  # noqa: PLC0415
    except ImportError as exc:
        raise FileFetchError("minio package not installed") from exc

    endpoint = cfg.minio_endpoint.replace("http://", "").replace("https://", "")
    secure = cfg.minio_endpoint.startswith("https://")
    client = Minio(
        endpoint,
        access_key=cfg.minio_access_key,
        secret_key=cfg.minio_secret_key,
        secure=secure,
    )
    try:
        response = client.get_object(bucket, object_key)
        try:
            data = response.read()
        finally:
            response.close()
            response.release_conn()
    except Exception as exc:
        raise FileFetchError(f"minio get_object failed bucket={bucket} key={object_key}: {exc}") from exc
    suffix = PurePosixPath(object_key).suffix.lower() or ".bin"
    return data, suffix


def _guess_suffix_from_url(file_url: str, content_type: str) -> str:
    lower = file_url.lower()
    for ext in (".pdf", ".docx", ".txt", ".md", ".markdown", ".html"):
        if lower.endswith(ext):
            return ext
    if "pdf" in content_type:
        return ".pdf"
    if "word" in content_type or "docx" in content_type:
        return ".docx"
    return ".txt"
