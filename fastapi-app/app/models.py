"""ORM 模型 —— 对应 JPA Entity（5 张表与 Java 版完全一致）。"""
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from .database import Base


class TimestampMixin:
    """created_at / updated_at 自动填充（对应 JPA @PrePersist / @PreUpdate）"""

    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now(), nullable=False
    )


class User(TimestampMixin, Base):
    """用户 —— 对应 users 表（JPA User entity）"""

    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(50), unique=True, nullable=False)
    email: Mapped[str] = mapped_column(String(100), nullable=False)
    # 密码 BCrypt 加密存储；响应序列化时永不输出（见 schemas.UserOut）
    password: Mapped[str] = mapped_column(String(100), nullable=False)


class Role(TimestampMixin, Base):
    """角色 —— 对应 roles 表"""

    __tablename__ = "roles"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str | None] = mapped_column(String(255), nullable=True)


class Permission(TimestampMixin, Base):
    """权限 —— 对应 permissions 表；编码约定 资源:动作（user:create）"""

    __tablename__ = "permissions"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    code: Mapped[str] = mapped_column(String(128), unique=True, nullable=False)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str | None] = mapped_column(String(255), nullable=True)


class UserRole(Base):
    """用户-角色关联 —— 对应 user_roles 表（多对多）"""

    __tablename__ = "user_roles"
    __table_args__ = (UniqueConstraint("user_id", "role_id", name="uk_user_role"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    role_id: Mapped[int] = mapped_column(ForeignKey("roles.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False
    )


class RolePermission(Base):
    """角色-权限关联 —— 对应 role_permissions 表（多对多）"""

    __tablename__ = "role_permissions"
    __table_args__ = (UniqueConstraint("role_id", "permission_id", name="uk_role_permission"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    role_id: Mapped[int] = mapped_column(ForeignKey("roles.id"), nullable=False)
    permission_id: Mapped[int] = mapped_column(ForeignKey("permissions.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False
    )
