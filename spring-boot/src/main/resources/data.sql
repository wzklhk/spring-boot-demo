-- ============================================================
-- 种子数据（初始化时执行，幂等）
-- 默认管理员账号：admin / 123456（密码为 BCrypt hash，与 Spring Security 加密一致）
-- 依赖 user.username 唯一约束实现幂等：重复执行时 ON DUPLICATE KEY 只触碰自身（无副作用）
-- ============================================================

INSERT INTO user (username, email, password, created_at, updated_at)
VALUES ('admin', 'admin@example.com', '$2a$10$jOlfLO3hIZNZtiJ9f4dDXuXyX4UQHh.Sb1jWAyZvvYt/oyCW/qggm', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE username = username;

-- 内置 ADMIN 角色：拥有全部查看/修改权限；其余角色仅可查看
INSERT INTO role (code, name, description, created_at, updated_at)
VALUES ('ADMIN', '管理员', '系统管理员：可查看并修改用户/角色/权限等配置', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE name = name;

-- 将种子管理员 admin 绑定 ADMIN 角色（幂等：依赖 user_role 唯一约束）
INSERT INTO user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM user u
JOIN role r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE user_id = user_id;
