from fastapi import Depends, Header, HTTPException, status

from app.config import Settings, get_settings


def verify_internal_token(
    authorization: str | None = Header(default=None),
    x_internal_token: str | None = Header(default=None, alias="X-Internal-Token"),
    settings: Settings = Depends(get_settings),
) -> None:
    """Validate service-to-service token (Bearer or X-Internal-Token)."""
    cfg = settings
    token: str | None = None

    if authorization and authorization.lower().startswith("bearer "):
        token = authorization[7:].strip()
    elif x_internal_token:
        token = x_internal_token.strip()

    if not token or token != cfg.ai_service_internal_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing internal token",
        )
