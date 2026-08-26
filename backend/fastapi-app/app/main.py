"""FastAPI 应用入口 —— 对应 DemoApplication + GlobalExceptionHandler + SecurityConfig。

- 全局异常 → Result 统一包装（BizError→400；401→未认证/密码错误；校验错误→400+errors）
- /docs 为 Swagger UI（对应 Java 版 knife4j /doc.html），/openapi.json 对应 /v3/api-docs
- 所有 /api/** 需要 JWT（对应 SecurityConfig），docs 路径放行
"""
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .core.result import Result
from .database import init_db
from .routers import auth, mybatis_users, rbac, roles_permissions, users
from .services.user_service import BizError


@asynccontextmanager
async def lifespan(_: FastAPI):
    init_db()  # 建表（对应 JPA ddl-auto=update）
    yield


app = FastAPI(
    title="spring-boot-demo API (FastAPI 复刻版)",
    description=(
        "Spring Boot 3.5 演示项目的 FastAPI 复刻：JWT 鉴权 / BCrypt / "
        "JPA(ORM) + MyBatis(原生 SQL) 双持久层 / RBAC 角色权限。"
        "接口路径、请求/响应结构与 Java 版完全一致。"
    ),
    version="1.0.0",
    lifespan=lifespan,
)


# ── 全局异常处理（对应 GlobalExceptionHandler） ───────────────────

@app.exception_handler(BizError)
async def handle_biz_error(_: Request, exc: BizError) -> JSONResponse:
    """业务异常 → 400 + Result.error(400, message)（对应 RuntimeException handler）"""
    return JSONResponse(status_code=400, content=Result.error(400, str(exc)).model_dump())


@app.exception_handler(HTTPException)
async def handle_http_exception(_: Request, exc: HTTPException) -> JSONResponse:
    """401（未认证 / 密码错误）→ Result 包装（对应 BadCredentials/认证失败 handler）"""
    return JSONResponse(
        status_code=exc.status_code,
        content=Result.error(exc.status_code, str(exc.detail)).model_dump(),
    )


@app.exception_handler(RequestValidationError)
async def handle_validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
    """参数校验失败 → 400 + errors 字段（对应 MethodArgumentNotValidException handler）"""
    errors: dict[str, str] = {}
    for err in exc.errors():
        field = ".".join(str(loc) for loc in err.get("loc", []) if loc != "body")
        errors[field or "body"] = err.get("msg", "参数错误")
    return JSONResponse(
        status_code=400,
        content=Result.error(400, "参数校验失败", errors).model_dump(),
    )


# ── 路由注册（对应各 @RestController） ────────────────────────────
app.include_router(auth.router)
app.include_router(users.router)
app.include_router(mybatis_users.router)
app.include_router(roles_permissions.roles_router)
app.include_router(roles_permissions.permissions_router)
app.include_router(rbac.router)


@app.get("/api/health", tags=["系统"], response_model=Result[dict])
def health():
    """健康检查（对应 Spring Actuator 的轻量替代）"""
    return Result.success({"status": "UP"})


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
