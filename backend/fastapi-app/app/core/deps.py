"""FastAPI 依赖注入 —— 对应 Spring Security 的认证上下文 / @CurrentUser。

- get_db：每个请求一个数据库会话
- get_current_user：从 Authorization: Bearer 提取并校验 JWT（对应 JwtAuthenticationFilter 被动式行为：
  无 token/无效 token → 401；有效 token → 注入当前用户）
- extract_bearer_token：对应 AuthController.extractBearerToken
"""
from fastapi import Depends, Header, HTTPException
from sqlalchemy.orm import Session

from .. import models
from ..database import get_db
from ..core.security import jwt_service

UNAUTHORIZED = HTTPException(status_code=401, detail="未认证或 token 已失效")


def extract_bearer_token(authorization: str | None) -> str | None:
    """从请求头提取 Bearer token；无/格式错误返回 None"""
    if authorization and authorization.startswith("Bearer "):
        return authorization[7:]
    return None


def get_current_user(
    authorization: str | None = Header(default=None, alias="Authorization"),
    db: Session = Depends(get_db),
) -> models.User:
    """认证依赖：校验 token 并加载用户（对应 SecurityConfig 对 /api/** 的认证要求）"""
    token = extract_bearer_token(authorization)
    if not token or not jwt_service.is_valid(token):
        raise UNAUTHORIZED

    username = jwt_service.extract_username(token)
    if not username:
        raise UNAUTHORIZED

    user = db.query(models.User).filter(models.User.username == username).first()
    if not user:
        raise UNAUTHORIZED
    return user
