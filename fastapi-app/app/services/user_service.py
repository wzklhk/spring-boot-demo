"""用户业务 —— 对应 JPA 版 UserService（SQLAlchemy ORM）。

与 mybatis 版功能等价，共享同一张 users 表。
"""
from sqlalchemy.orm import Session

from .. import models
from ..config import DEFAULT_PASSWORD
from ..core.security import password_encoder


class BizError(Exception):
    """业务异常 → 统一映射为 HTTP 400 + Result.error(400, message)"""


class UserService:
    """JPA 等价实现（ORM 查询）"""

    def __init__(self, db: Session) -> None:
        self.db = db

    def find_all(self) -> list[models.User]:
        return self.db.query(models.User).order_by(models.User.id).all()

    def find_by_id(self, user_id: int) -> models.User:
        user = self.db.get(models.User, user_id)
        if user is None:
            raise BizError(f"用户不存在，ID: {user_id}")
        return user

    def find_by_username(self, username: str) -> models.User:
        user = (
            self.db.query(models.User)
            .filter(models.User.username == username)
            .first()
        )
        if user is None:
            raise BizError(f"用户不存在，用户名: {username}")
        return user

    def create(self, username: str, email: str, password: str | None = None) -> models.User:
        if self.db.query(models.User).filter(models.User.username == username).first():
            raise BizError(f"用户名已存在: {username}")
        if self.db.query(models.User).filter(models.User.email == email).first():
            raise BizError(f"邮箱已被注册: {email}")
        # 密码 BCrypt 加密存储；未传密码时使用默认密码 123456（与 Java 版一致）
        raw = password if password and password.strip() else DEFAULT_PASSWORD
        user = models.User(
            username=username,
            email=email,
            password=password_encoder.encode(raw),
        )
        self.db.add(user)
        self.db.commit()
        self.db.refresh(user)
        return user

    def update(self, user_id: int, username: str, email: str) -> models.User:
        existing = self.find_by_id(user_id)
        existing.username = username
        existing.email = email
        self.db.commit()
        self.db.refresh(existing)
        return existing

    def delete(self, user_id: int) -> None:
        if self.db.get(models.User, user_id) is None:
            raise BizError(f"用户不存在，ID: {user_id}")
        # 级联清理：用户-角色 关联（对应 userRoleRepository.deleteByUserId）
        self.db.query(models.UserRole).filter(models.UserRole.user_id == user_id).delete()
        self.db.query(models.User).filter(models.User.id == user_id).delete()
        self.db.commit()
