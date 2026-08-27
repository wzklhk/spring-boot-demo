"""角色 / 权限业务 —— 对应 RoleService / PermissionService（JPA 版）"""
from sqlalchemy.orm import Session

from .. import models
from .user_service import BizError


class RoleService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def find_all(self) -> list[models.Role]:
        return self.db.query(models.Role).order_by(models.Role.id).all()

    def find_by_id(self, role_id: int) -> models.Role:
        role = self.db.get(models.Role, role_id)
        if role is None:
            raise BizError(f"角色不存在，ID: {role_id}")
        return role

    def create(self, code: str, name: str, description: str | None = None) -> models.Role:
        if self.db.query(models.Role).filter(models.Role.code == code).first():
            raise BizError(f"角色编码已存在: {code}")
        role = models.Role(code=code, name=name, description=description)
        self.db.add(role)
        self.db.commit()
        self.db.refresh(role)
        return role

    def update(self, role_id: int, code: str, name: str, description: str | None = None) -> models.Role:
        role = self.find_by_id(role_id)
        role.code = code
        role.name = name
        role.description = description
        self.db.commit()
        self.db.refresh(role)
        return role

    def delete(self, role_id: int) -> None:
        if self.db.get(models.Role, role_id) is None:
            raise BizError(f"角色不存在，ID: {role_id}")
        # 级联清理：角色-权限、用户-角色 关联
        self.db.query(models.RolePermission).filter(models.RolePermission.role_id == role_id).delete()
        self.db.query(models.UserRole).filter(models.UserRole.role_id == role_id).delete()
        self.db.query(models.Role).filter(models.Role.id == role_id).delete()
        self.db.commit()


class PermissionService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def find_all(self) -> list[models.Permission]:
        return self.db.query(models.Permission).order_by(models.Permission.id).all()

    def find_by_id(self, permission_id: int) -> models.Permission:
        permission = self.db.get(models.Permission, permission_id)
        if permission is None:
            raise BizError(f"权限不存在，ID: {permission_id}")
        return permission

    def create(self, code: str, name: str, description: str | None = None) -> models.Permission:
        if self.db.query(models.Permission).filter(models.Permission.code == code).first():
            raise BizError(f"权限编码已存在: {code}")
        permission = models.Permission(code=code, name=name, description=description)
        self.db.add(permission)
        self.db.commit()
        self.db.refresh(permission)
        return permission

    def update(self, permission_id: int, code: str, name: str, description: str | None = None) -> models.Permission:
        permission = self.find_by_id(permission_id)
        permission.code = code
        permission.name = name
        permission.description = description
        self.db.commit()
        self.db.refresh(permission)
        return permission

    def delete(self, permission_id: int) -> None:
        if self.db.get(models.Permission, permission_id) is None:
            raise BizError(f"权限不存在，ID: {permission_id}")
        self.db.query(models.RolePermission).filter(
            models.RolePermission.permission_id == permission_id
        ).delete()
        self.db.query(models.Permission).filter(models.Permission.id == permission_id).delete()
        self.db.commit()
