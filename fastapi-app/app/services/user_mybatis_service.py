"""用户业务（MyBatis 等价）—— 对应 UserMyBatisService。

与 JPA 版 UserService 功能等价，但用原生 SQL（对应 UserMapper.xml），
共享同一张 user 表 —— 复刻 Java 版"双持久层并存"的设计。
"""
from sqlalchemy import text
from sqlalchemy.orm import Session

from .. import models
from ..config import DEFAULT_PASSWORD
from ..core.security import password_encoder
from .user_service import BizError

# 对应 UserMapper.xml 的 Base_Column_List
BASE_COLUMNS = "id, username, email, password, created_at, updated_at"


class UserMyBatisService:
    """MyBatis 等价实现（原生 SQL，对应 UserMapper.xml 全部语句）"""

    def __init__(self, db: Session) -> None:
        self.db = db

    def _row_to_user(self, row) -> models.User | None:
        if row is None:
            return None
        return models.User(
            id=row.id, username=row.username, email=row.email, password=row.password,
            created_at=row.created_at, updated_at=row.updated_at,
        )

    def find_all(self) -> list[models.User]:
        rows = self.db.execute(text(f"SELECT {BASE_COLUMNS} FROM user ORDER BY id")).all()
        return [self._row_to_user(r) for r in rows]

    def find_by_id(self, user_id: int) -> models.User:
        row = self.db.execute(
            text(f"SELECT {BASE_COLUMNS} FROM user WHERE id = :id"), {"id": user_id}
        ).first()
        user = self._row_to_user(row)
        if user is None:
            raise BizError(f"用户不存在，ID: {user_id}")
        return user

    def find_by_username(self, username: str) -> models.User:
        row = self.db.execute(
            text(f"SELECT {BASE_COLUMNS} FROM user WHERE username = :username"),
            {"username": username},
        ).first()
        user = self._row_to_user(row)
        if user is None:
            raise BizError(f"用户不存在，用户名: {username}")
        return user

    def count_by_username(self, username: str) -> int:
        return self.db.execute(
            text("SELECT COUNT(*) FROM user WHERE username = :username"),
            {"username": username},
        ).scalar()

    def count_by_email(self, email: str) -> int:
        return self.db.execute(
            text("SELECT COUNT(*) FROM user WHERE email = :email"), {"email": email}
        ).scalar()

    def create(self, username: str, email: str, password: str | None = None) -> models.User:
        if self.count_by_username(username) > 0:
            raise BizError(f"用户名已存在: {username}")
        if self.count_by_email(email) > 0:
            raise BizError(f"邮箱已被注册: {email}")
        # 原生 SQL 不走 ORM 生命周期，时间戳由数据库默认值填充（对应 MyBatis XML 手动填时间）
        raw = password if password and password.strip() else DEFAULT_PASSWORD
        result = self.db.execute(
            text(
                "INSERT INTO user (username, email, password) VALUES (:username, :email, :password)"
            ),
            {"username": username, "email": email, "password": password_encoder.encode(raw)},
        )
        self.db.commit()
        return self.find_by_id(result.lastrowid)

    def update(self, user_id: int, username: str, email: str) -> models.User:
        existing = self.find_by_id(user_id)
        self.db.execute(
            text("UPDATE user SET username = :username, email = :email, updated_at = CURRENT_TIMESTAMP WHERE id = :id"),
            {"username": username, "email": email, "id": user_id},
        )
        self.db.commit()
        existing.username = username
        existing.email = email
        return existing

    def delete(self, user_id: int) -> None:
        if self.find_by_id(user_id) is None:
            raise BizError(f"用户不存在，ID: {user_id}")
        # 级联清理：用户-角色 关联
        self.db.execute(text("DELETE FROM user_role WHERE user_id = :id"), {"id": user_id})
        self.db.execute(text("DELETE FROM user WHERE id = :id"), {"id": user_id})
        self.db.commit()
