"""安全组件 —— 对应 Java 的 JwtService + Spring Security PasswordEncoder（BCrypt）。

- JWT：subject=用户名、claim uid、jti 唯一、登出黑名单（内存实现，重启清空）
- 密码：BCrypt 加密（对应 PasswordEncoder.encode / matches）
"""
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any

import bcrypt
import jwt as pyjwt

from ..config import JWT_EXPIRATION_SECONDS, JWT_SECRET


class JwtService:
    """JWT 令牌服务：生成 / 解析 / 校验 + 登出黑名单（对应 Java JwtService）"""

    def __init__(self) -> None:
        self.secret = JWT_SECRET
        self.expiration_seconds = JWT_EXPIRATION_SECONDS
        # 已登出的 jti 黑名单（内存版，重启失效；生产可换 Redis —— 与 Java 版注释一致）
        self._blacklist: set[str] = set()

    def generate_token(self, user: Any) -> str:
        """生成 token：jti 每次唯一（即使同一秒重复签发也各不相同）"""
        now = datetime.now(timezone.utc)
        payload = {
            "jti": str(uuid.uuid4()),
            "sub": user.username,
            "uid": user.id,
            "iat": now,
            "exp": now + timedelta(seconds=self.expiration_seconds),
        }
        return pyjwt.encode(payload, self.secret, algorithm="HS256")

    def parse_claims(self, token: str) -> dict | None:
        """解析并校验签名/过期；无效返回 None"""
        try:
            return pyjwt.decode(token, self.secret, algorithms=["HS256"])
        except Exception:
            return None

    def extract_username(self, token: str) -> str | None:
        claims = self.parse_claims(token)
        return claims.get("sub") if claims else None

    def is_valid(self, token: str) -> bool:
        claims = self.parse_claims(token)
        return bool(claims and claims.get("jti") not in self._blacklist)

    def invalidate(self, token: str) -> None:
        """登出：按 jti 加入黑名单（仅失效这一个 token）"""
        claims = self.parse_claims(token)
        if claims and claims.get("jti"):
            self._blacklist.add(claims["jti"])


class PasswordEncoder:
    """BCrypt 密码编码器 —— 对应 Spring Security BCryptPasswordEncoder"""

    @staticmethod
    def encode(raw: str) -> str:
        return bcrypt.hashpw(raw.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

    @staticmethod
    def matches(raw: str, hashed: str) -> bool:
        try:
            return bcrypt.checkpw(raw.encode("utf-8"), hashed.encode("utf-8"))
        except ValueError:
            return False


# 单例（对应 Spring 的单例 Bean）
jwt_service = JwtService()
password_encoder = PasswordEncoder()
