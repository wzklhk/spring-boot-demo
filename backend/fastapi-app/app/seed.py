"""可选种子数据脚本：初始化一个管理员 + RBAC 基础数据，方便直接体验。

用法：uv run python -m app.seed
（Java 版 H2 空库启动，本脚本按需执行，不影响行为等价性）
"""
from .database import SessionLocal, init_db
from .models import Permission, Role, RolePermission, User, UserRole
from .core.security import password_encoder

SEED = {
    "users": [{"username": "admin", "email": "admin@example.com", "password": "123456"}],
    "roles": [
        {"code": "ADMIN", "name": "管理员", "description": "拥有全部权限"},
        {"code": "OPERATOR", "name": "操作员", "description": "基础操作权限"},
    ],
    "permissions": [
        {"code": "user:create", "name": "创建用户", "description": "创建用户"},
        {"code": "user:read", "name": "查看用户", "description": "查看用户列表与详情"},
        {"code": "user:update", "name": "更新用户", "description": "更新用户信息"},
        {"code": "user:delete", "name": "删除用户", "description": "删除用户"},
        {"code": "role:manage", "name": "角色管理", "description": "角色的增删改查与权限分配"},
        {"code": "permission:manage", "name": "权限管理", "description": "权限的增删改查"},
    ],
}


def run() -> None:
    init_db()
    db = SessionLocal()
    try:
        admin = (
            db.query(UserRole)
            .join(Role, UserRole.role_id == Role.id)
            .filter(Role.code == "ADMIN")
            .first()
        )
        if admin:
            print("种子数据已存在，跳过")
            return

        user = None
        for u in SEED["users"]:
            user = models_user(db, u["username"], u["email"], u["password"])
        role_admin = None
        for r in SEED["roles"]:
            role = models_role(db, r["code"], r["name"], r["description"])
            if r["code"] == "ADMIN":
                role_admin = role
        for p in SEED["permissions"]:
            models_permission(db, p["code"], p["name"], p["description"])
        # ADMIN 角色挂上全部权限
        all_perms = db.query(Permission).all()
        for perm in all_perms:
            db.add(RolePermission(role_id=role_admin.id, permission_id=perm.id))
        # admin 用户挂 ADMIN 角色
        db.add(UserRole(user_id=user.id, role_id=role_admin.id))
        db.commit()
        print("种子数据完成：admin/123456，角色 ADMIN/OPERATOR，6 条权限")
    finally:
        db.close()


def models_user(db, username: str, email: str, password: str):
    from .models import User

    u = User(username=username, email=email, password=password_encoder.encode(password))
    db.add(u)
    db.flush()
    return u


def models_role(db, code: str, name: str, description: str):
    r = Role(code=code, name=name, description=description)
    db.add(r)
    db.flush()
    return r


def models_permission(db, code: str, name: str, description: str):
    p = Permission(code=code, name=name, description=description)
    db.add(p)
    db.flush()
    return p


if __name__ == "__main__":
    run()
