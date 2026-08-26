"""统一响应包装 —— 对应 Java 的 Result<T>：所有接口返回 {code, message, data[, errors]}"""
from typing import Any, Generic, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class Result(BaseModel, Generic[T]):
    """code: 200=成功 201=创建成功 400=业务/参数错误 401=未授权"""

    code: int
    message: str
    data: T | None = None
    errors: dict[str, str] | None = None

    @classmethod
    def success(cls, *args, **kwargs) -> "Result":
        """兼容 success(data) 与 success(message, data) 两种调用（对应 Java 版两个重载）"""
        if kwargs:
            return cls(code=200, message=kwargs.get("message", "success"), data=kwargs.get("data"))
        if len(args) == 2:
            message, data = args
            return cls(code=200, message=message, data=data)
        if len(args) == 1:
            return cls(code=200, message="success", data=args[0])
        return cls(code=200, message="success", data=None)

    @classmethod
    def created(cls, data: Any = None) -> "Result":
        return cls(code=201, message="创建成功", data=data)

    @classmethod
    def updated(cls, data: Any = None) -> "Result":
        return cls(code=200, message="更新成功", data=data)

    @classmethod
    def deleted(cls) -> "Result":
        return cls(code=200, message="删除成功", data=None)

    @classmethod
    def error(cls, code: int, message: str, errors: dict[str, str] | None = None) -> "Result":
        return cls(code=code, message=message, data=None, errors=errors)
