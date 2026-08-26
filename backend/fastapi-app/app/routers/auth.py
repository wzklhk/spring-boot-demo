"""认证接口 —— 对应 AuthController：注册 / 登录 / 退出 / 注销账号。"""
from fastapi import APIRouter, Depends, Header, HTTPException, status
from sqlalchemy.orm import Session

from .. import models
from ..core.deps import extract_bearer_token, get_current_user
from ..core.result import Result
from ..core.security import jwt_service, password_encoder
from ..database import get_db
from ..schemas import AuthResponse, AuthUserInfo, LoginRequest, RegisterRequest
from ..services.user_service import UserService

router = APIRouter(prefix="/api/auth", tags=["认证"])


def _build_auth_response(user: models.User, token: str) -> AuthResponse:
    """对应 AuthResponse.from(user, token, expiresIn)"""
    return AuthResponse(
        token=token,
        tokenType="Bearer",
        expiresIn=jwt_service.expiration_seconds,
        user=AuthUserInfo(id=user.id, username=user.username, email=user.email),
    )


@router.post("/register", response_model=Result[AuthResponse], status_code=status.HTTP_201_CREATED)
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    """注册用户（密码 BCrypt 加密存储），注册成功直接返回 token（注册即登录）"""
    user = UserService(db).create(username=req.username, email=req.email, password=req.password)
    token = jwt_service.generate_token(user)
    return Result.success("注册成功", _build_auth_response(user, token))


@router.post("/login", response_model=Result[AuthResponse])
def login(req: LoginRequest, db: Session = Depends(get_db)):
    """登录：校验用户名密码，成功返回 token"""
    service = UserService(db)
    user = service.find_by_username(req.username)
    if not password_encoder.matches(req.password, user.password):
        # 对应 BadCredentialsException → 401 用户名或密码错误（不暴露内部细节）
        raise HTTPException(status_code=401, detail="用户名或密码错误")
    token = jwt_service.generate_token(user)
    return Result.success("登录成功", _build_auth_response(user, token))


@router.post("/logout", response_model=Result[None])
def logout(
    authorization: str | None = Header(default=None, alias="Authorization"),
    _: models.User = Depends(get_current_user),
):
    """退出登录：token 加入黑名单，立即失效"""
    token = extract_bearer_token(authorization)
    if token:
        jwt_service.invalidate(token)
    return Result.success("退出成功", None)


@router.delete("/account", response_model=Result[None])
def delete_account(user: models.User = Depends(get_current_user), db: Session = Depends(get_db)):
    """注销账号：删除当前登录用户（含角色关联级联清理）"""
    UserService(db).delete(user.id)
    return Result.success("账号已注销", None)
