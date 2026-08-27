"""RBAC 关联业务 —— 对应 RbacService：用户-角色 / 角色-权限 分配与聚合查询。

聚合查询用原生 SQL（对应 RbacMapper.xml 的多表 join），
其余关联操作用 ORM（对应 JPA Repository）—— 双持久层协同，与 Java 版一致。
"""
from datetime import datetime

from sqlalchemy import text
from sqlalchemy.orm import Session

from .. import models
from ..schemas import UserPermissionVO
from .rbac_services import PermissionService, RoleService
from .user_service import BizError


class RbacService:
    def __init__(self, db: Session) -> None:
        self.db = db
        self.role_service = RoleService(db)
        self.permission_service = PermissionService(db)

    # ── 用户-角色 ──────────────────────────────────────────────

    def get_user_roles(self, user_id: int) -> list[models.Role]:
        if self.db.get(models.User, user_id) is None:
            raise BizError(f"用户不存在，ID: {user_id}")
        rows = (
            self.db.query(models.Role)
            .join(models.UserRole, models.UserRole.role_id == models.Role.id)
            .filter(models.UserRole.user_id == user_id)
            .order_by(models.Role.id)
            .all()
        )
        return rows

    def assign_role_to_user(self, user_id: int, role_id: int) -> models.Role:
        if self.db.get(models.User, user_id) is None:
            raise BizError(f"用户不存在，ID: {user_id}")
        role = self.role_service.find_by_id(role_id)
        exists = (
            self.db.query(models.UserRole)
            .filter(models.UserRole.user_id == user_id, models.UserRole.role_id == role_id)
            .first()
        )
        if not exists:  # 幂等：已分配则忽略
            self.db.add(models.UserRole(user_id=user_id, role_id=role_id))
            self.db.commit()
        return role

    def remove_role_from_user(self, user_id: int, role_id: int) -> None:
        if self.db.get(models.User, user_id) is None:
            raise BizError(f"用户不存在，ID: {user_id}")
        self.role_service.find_by_id(role_id)
        self.db.query(models.UserRole).filter(
            models.UserRole.user_id == user_id, models.UserRole.role_id == role_id
        ).delete()
        self.db.commit()

    # ── 角色-权限 ──────────────────────────────────────────────

    def get_role_permissions(self, role_id: int) -> list[models.Permission]:
        self.role_service.find_by_id(role_id)
        rows = (
            self.db.query(models.Permission)
            .join(models.RolePermission, models.RolePermission.permission_id == models.Permission.id)
            .filter(models.RolePermission.role_id == role_id)
            .order_by(models.Permission.id)
            .all()
        )
        return rows

    def assign_permission_to_role(self, role_id: int, permission_id: int) -> models.Permission:
        self.role_service.find_by_id(role_id)
        permission = self.permission_service.find_by_id(permission_id)
        exists = (
            self.db.query(models.RolePermission)
            .filter(
                models.RolePermission.role_id == role_id,
                models.RolePermission.permission_id == permission_id,
            )
            .first()
        )
        if not exists:  # 幂等
            self.db.add(models.RolePermission(role_id=role_id, permission_id=permission_id))
            self.db.commit()
        return permission

    def remove_permission_from_role(self, role_id: int, permission_id: int) -> None:
        self.role_service.find_by_id(role_id)
        self.permission_service.find_by_id(permission_id)
        self.db.query(models.RolePermission).filter(
            models.RolePermission.role_id == role_id,
            models.RolePermission.permission_id == permission_id,
        ).delete()
        self.db.commit()

    # ── 聚合查询（对应 RbacMapper.xml findUserPermissions） ───────

    def get_user_permissions(self, user_id: int) -> list[UserPermissionVO]:
        if self.db.get(models.User, user_id) is None:
            raise BizError(f"用户不存在，ID: {user_id}")
        sql = text(
            """
            SELECT p.id AS id,
                   p.code AS code,
                   p.name AS name,
                   p.description AS description,
                   r.code AS roleCode,
                   r.name AS roleName,
                   ur.created_at AS grantedAt
            FROM user_roles ur
                JOIN roles r ON ur.role_id = r.id
                JOIN role_permissions rp ON rp.role_id = r.id
                JOIN permissions p ON rp.permission_id = p.id
            WHERE ur.user_id = :userId
            ORDER BY p.id, r.id
            """
        )
        rows = self.db.execute(sql, {"userId": user_id}).all()
        return [
            UserPermissionVO(
                id=row.id,
                code=row.code,
                name=row.name,
                description=row.description,
                roleCode=row.roleCode,
                roleName=row.roleName,
                grantedAt=row.grantedAt,
            )
            for row in rows
        ]
