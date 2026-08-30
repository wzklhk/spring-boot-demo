-- ============================================================
-- 种子数据（初始化时执行，幂等）
-- 默认管理员账号：admin / 123456（密码为 BCrypt hash，与 Spring Security 加密一致）
-- 依赖 user.username 唯一约束实现幂等：重复执行时 ON DUPLICATE KEY 只触碰自身（无副作用）
-- ============================================================

INSERT INTO user (username, email, password, created_at, updated_at)
VALUES ('admin', 'admin@example.com', '$2a$10$jOlfLO3hIZNZtiJ9f4dDXuXyX4UQHh.Sb1jWAyZvvYt/oyCW/qggm', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE username = username;
