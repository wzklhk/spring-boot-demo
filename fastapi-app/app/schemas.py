"""Pydantic 请求/响应模型 —— 对应 Java 的 DTO/VO + 校验注解。

- LoginRequest / RegisterRequest：对应 auth/dto
- AuthResponse：对应 auth/vo（token + 用户摘要）
- UserOut / RoleOut / PermissionOut：序列化视图（password 永不输出，对应 @JsonProperty(WRITE_ONLY)）
- UserVO / RoleVO / PermissionVO：统一分页查询入参（非空字段为等值条件，空 VO 即普通分页）+ 返回体
- PageResult：分页包装，对应 Java 的 PageResult
"""
from datetime import datetime
from typing import Generic, Optional, TypeVar

from pydantic import BaseModel, ConfigDict, EmailStr, Field

T = TypeVar("T")


# ── 分页响应（对应 PageResult<T>） ─────────────────────────────
class PageResult(BaseModel, Generic[T]):
    """统一分页响应：list 当前页数据、total 总记录数、page 当前页（1 起）、size 每页条数、pages 总页数"""

    list: list[T]
    total: int
    page: int
    size: int
    pages: int


# ── 认证请求（对应 LoginRequest / RegisterRequest） ──────────────
class LoginRequest(BaseModel):
    username: str = Field(min_length=1, description="用户名")
    password: str = Field(min_length=1, description="密码")


class RegisterRequest(BaseModel):
    username: str = Field(min_length=3, max_length=32, description="用户名，3-32 位")
    email: EmailStr = Field(description="邮箱")
    password: str = Field(min_length=6, max_length=64, description="密码，6-64 位")


# ── 认证响应（对应 AuthResponse） ────────────────────────────────
class AuthUserInfo(BaseModel):
    id: int
    username: str
    email: str


class AuthResponse(BaseModel):
    token: str
    tokenType: str = "Bearer"
    expiresIn: int
    user: AuthUserInfo


# ── 实体输出视图（对应 Entity 的序列化结果） ──────────────────────
class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    username: str
    email: str
    created_at: datetime
    updated_at: datetime


class RoleOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    code: str
    name: str
    description: Optional[str] = None
    created_at: datetime
    updated_at: datetime


class PermissionOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    code: str
    name: str
    description: Optional[str] = None
    created_at: datetime
    updated_at: datetime


class UserPermissionVO(BaseModel):
    """聚合查询结果 —— 对应 UserPermissionVO（权限 + 来源角色）"""

    id: int
    code: str
    name: str
    description: Optional[str] = None
    roleCode: str
    roleName: str
    grantedAt: datetime


# ── 实体 VO（统一分页查询入参 + 返回体，对应 Java 的 UserVO/RoleVO/PermissionVO） ──
class UserVO(BaseModel):
    """用户视图对象 —— 非空字段作为等值查询条件，空 VO 即普通分页；不含密码"""

    id: Optional[int] = None
    username: Optional[str] = None
    email: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


class RoleVO(BaseModel):
    """角色视图对象 —— 非空字段作为等值查询条件，空 VO 即普通分页"""

    id: Optional[int] = None
    code: Optional[str] = None
    name: Optional[str] = None
    description: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


class PermissionVO(BaseModel):
    """权限视图对象 —— 非空字段作为等值查询条件，空 VO 即普通分页"""

    id: Optional[int] = None
    code: Optional[str] = None
    name: Optional[str] = None
    description: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


# ── 创建/更新请求（对应 @RequestBody Entity，含校验） ─────────────
class UserCreate(BaseModel):
    username: str = Field(min_length=3, max_length=32, description="用户名")
    email: EmailStr = Field(description="邮箱")
    password: Optional[str] = Field(
        default=None, min_length=6, max_length=64, description="密码，缺省时用默认密码 123456"
    )


class UserUpdate(BaseModel):
    username: str = Field(min_length=3, max_length=32, description="用户名")
    email: EmailStr = Field(description="邮箱")


class RoleCreate(BaseModel):
    code: str = Field(min_length=1, max_length=64, description="角色编码，如 ADMIN")
    name: str = Field(min_length=1, max_length=128, description="角色名称，如 管理员")
    description: Optional[str] = Field(default=None, max_length=255)


class RoleUpdate(BaseModel):
    code: str = Field(min_length=1, max_length=64)
    name: str = Field(min_length=1, max_length=128)
    description: Optional[str] = Field(default=None, max_length=255)


class PermissionCreate(BaseModel):
    code: str = Field(min_length=1, max_length=128, description="权限编码，如 user:create")
    name: str = Field(min_length=1, max_length=128, description="权限名称，如 创建用户")
    description: Optional[str] = Field(default=None, max_length=255)


class PermissionUpdate(BaseModel):
    code: str = Field(min_length=1, max_length=128)
    name: str = Field(min_length=1, max_length=128)
    description: Optional[str] = Field(default=None, max_length=255)