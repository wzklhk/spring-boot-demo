"""数据库引擎与会话 —— 对应 spring-boot 的 JPA EntityManager + H2。"""
from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from .config import DATABASE_URL

# SQLite 需要 check_same_thread=False（FastAPI 多线程处理请求）
connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}

engine = create_engine(DATABASE_URL, connect_args=connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    """ORM 基类（对应 JPA @Entity 的公共基类）"""


def init_db() -> None:
    """建表（对应 JPA ddl-auto=update）"""
    from . import models  # noqa: F401  确保模型已注册

    Base.metadata.create_all(bind=engine)


def get_db():
    """FastAPI 依赖：每请求一个 Session（对应 JPA OpenSessionInView）"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
