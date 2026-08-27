"""应用配置 —— 对应 spring-boot 的 application.yml（jwt 段）。"""
import os

# JWT 密钥：优先环境变量，默认值仅用于本地开发（与 Java 版 JWT_SECRET 覆盖机制一致）
JWT_SECRET: str = os.getenv("JWT_SECRET", "fastapi-demo-dev-secret-change-me-0123456789abcdef")
# token 有效期（秒），与 Java 版 jwt.expiration 对应
JWT_EXPIRATION_SECONDS: int = int(os.getenv("JWT_EXPIRATION", "86400"))

# 数据库：默认 SQLite（对应 H2 内存库，零配置）；可用 DATABASE_URL 切换 MySQL/PostgreSQL
DATABASE_URL: str = os.getenv(
    "DATABASE_URL",
    "sqlite:///./fastapi_demo.db",  # 文件库，重启数据仍在；改 ":memory:" 即内存库
)

# 新增用户未传密码时的默认密码（与 Java 版一致）
DEFAULT_PASSWORD: str = "123456"
