"""RBAC 关联接口 —— 对应 RbacController。

RESTful 子资源风格：
  /api/users/{userId}/roles            用户的角色
  /api/users/{userId}/permissions      用户的全部权限（聚合）
  /api/roles/{roleId}/permissions      角色的权限
"""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from ..core.deps import get_current_user
from ..core.result import Result
from ..database import get_db
from ..schemas import PermissionOut, RoleOut, UserPermissionVO
from ..services.rbac_service import RbacService

router = APIRouter(tags=["RBAC 关联管理"], dependencies=[Depends(get_current_user)])


# ── 用户-角色 ──────────────────────────────────────────────

@router.get("/api/users/{user_id}/roles", response_model=Result[list[RoleOut]])
def get_user_roles(user_id: int, db: Session = Depends(get_db)):
    return Result.success(RbacService(db).get_user_roles(user_id))


@router.post("/api/users/{user_id}/roles/{role_id}", response_model=Result[RoleOut])
def assign_role_to_user(user_id: int, role_id: int, db: Session = Depends(get_db)):
    role = RbacService(db).assign_role_to_user(user_id, role_id)
    return Result.success("角色分配成功", role)


@router.delete("/api/users/{user_id}/roles/{role_id}", response_model=Result[None])
def remove_role_from_user(user_id: int, role_id: int, db: Session = Depends(get_db)):
    RbacService(db).remove_role_from_user(user_id, role_id)
    return Result.success("角色移除成功", None)


# ── 角色-权限 ──────────────────────────────────────────────

@router.get("/api/roles/{role_id}/permissions", response_model=Result[list[PermissionOut]])
def get_role_permissions(role_id: int, db: Session = Depends(get_db)):
    return Result.success(RbacService(db).get_role_permissions(role_id))


@router.post("/api/roles/{role_id}/permissions/{permission_id}", response_model=Result[PermissionOut])
def assign_permission_to_role(role_id: int, permission_id: int, db: Session = Depends(get_db)):
    permission = RbacService(db).assign_permission_to_role(role_id, permission_id)
    return Result.success("权限分配成功", permission)


@router.delete("/api/roles/{role_id}/permissions/{permission_id}", response_model=Result[None])
def remove_permission_from_role(role_id: int, permission_id: int, db: Session = Depends(get_db)):
    RbacService(db).remove_permission_from_role(role_id, permission_id)
    return Result.success("权限移除成功", None)


# ── 聚合查询 ───────────────────────────────────────────────

@router.get("/api/users/{user_id}/permissions", response_model=Result[list[UserPermissionVO]])
def get_user_permissions(user_id: int, db: Session = Depends(get_db)):
    return Result.success(RbacService(db).get_user_permissions(user_id))
