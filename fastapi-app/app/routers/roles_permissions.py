"""角色 / 权限管理接口 —— 对应 RoleController / PermissionController。"""
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from ..core.deps import get_current_user
from ..core.result import Result
from ..database import get_db
from ..schemas import (
    PageResult,
    PermissionCreate,
    PermissionOut,
    PermissionUpdate,
    PermissionVO,
    RoleCreate,
    RoleOut,
    RoleUpdate,
    RoleVO,
)
from ..services.rbac_services import PermissionService, RoleService

# ── 角色 ─────────────────────────────────────────────────────
roles_router = APIRouter(prefix="/api/role", tags=["角色管理"], dependencies=[Depends(get_current_user)])


@roles_router.post("/query", response_model=Result[PageResult[RoleVO]])
def query_roles(body: RoleVO, page: int = 1, size: int = 10, db: Session = Depends(get_db)):
    """统一分页查询：VO 非空字段为等值条件，空 VO {} 即普通分页查询"""
    return Result.success(RoleService(db).query(body, page, size))


@roles_router.get("/{role_id}", response_model=Result[RoleOut])
def get_role(role_id: int, db: Session = Depends(get_db)):
    return Result.success(RoleService(db).find_by_id(role_id))


@roles_router.post("", response_model=Result[RoleOut], status_code=status.HTTP_201_CREATED)
def create_role(body: RoleCreate, db: Session = Depends(get_db)):
    role = RoleService(db).create(code=body.code, name=body.name, description=body.description)
    return Result.created(role)


@roles_router.put("/{role_id}", response_model=Result[RoleOut])
def update_role(role_id: int, body: RoleUpdate, db: Session = Depends(get_db)):
    role = RoleService(db).update(role_id, code=body.code, name=body.name, description=body.description)
    return Result.updated(role)


@roles_router.delete("/{role_id}", response_model=Result[None])
def delete_role(role_id: int, db: Session = Depends(get_db)):
    RoleService(db).delete(role_id)
    return Result.deleted()


# ── 权限 ─────────────────────────────────────────────────────
permissions_router = APIRouter(
    prefix="/api/permission", tags=["权限管理"], dependencies=[Depends(get_current_user)]
)


@permissions_router.post("/query", response_model=Result[PageResult[PermissionVO]])
def query_permissions(body: PermissionVO, page: int = 1, size: int = 10, db: Session = Depends(get_db)):
    """统一分页查询：VO 非空字段为等值条件，空 VO {} 即普通分页查询"""
    return Result.success(PermissionService(db).query(body, page, size))


@permissions_router.get("/{permission_id}", response_model=Result[PermissionOut])
def get_permission(permission_id: int, db: Session = Depends(get_db)):
    return Result.success(PermissionService(db).find_by_id(permission_id))


@permissions_router.post("", response_model=Result[PermissionOut], status_code=status.HTTP_201_CREATED)
def create_permission(body: PermissionCreate, db: Session = Depends(get_db)):
    permission = PermissionService(db).create(
        code=body.code, name=body.name, description=body.description
    )
    return Result.created(permission)


@permissions_router.put("/{permission_id}", response_model=Result[PermissionOut])
def update_permission(permission_id: int, body: PermissionUpdate, db: Session = Depends(get_db)):
    permission = PermissionService(db).update(
        permission_id, code=body.code, name=body.name, description=body.description
    )
    return Result.updated(permission)


@permissions_router.delete("/{permission_id}", response_model=Result[None])
def delete_permission(permission_id: int, db: Session = Depends(get_db)):
    PermissionService(db).delete(permission_id)
    return Result.deleted()