"""MyBatis 版用户接口 —— 对应 MyBatisUserController：/api/mybatis/users。

与 /api/users（ORM 版）功能等价，消息带 "(MyBatis)" 后缀 —— 用于对比验证双持久层。
"""
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from ..core.deps import get_current_user
from ..core.result import Result
from ..database import get_db
from ..schemas import UserCreate, UserOut, UserUpdate
from ..services.user_mybatis_service import UserMyBatisService

router = APIRouter(
    prefix="/api/mybatis/users", tags=["用户管理(MyBatis)"], dependencies=[Depends(get_current_user)]
)


@router.get("", response_model=Result[list[UserOut]])
def list_users(db: Session = Depends(get_db)):
    return Result.success("success (MyBatis)", UserMyBatisService(db).find_all())


@router.get("/username/{username}", response_model=Result[UserOut])
def get_by_username(username: str, db: Session = Depends(get_db)):
    return Result.success("success (MyBatis)", UserMyBatisService(db).find_by_username(username))


@router.get("/{user_id}", response_model=Result[UserOut])
def get_user(user_id: int, db: Session = Depends(get_db)):
    return Result.success("success (MyBatis)", UserMyBatisService(db).find_by_id(user_id))


@router.post("", response_model=Result[UserOut], status_code=status.HTTP_201_CREATED)
def create_user(body: UserCreate, db: Session = Depends(get_db)):
    user = UserMyBatisService(db).create(
        username=body.username, email=body.email, password=body.password
    )
    return Result.success("创建成功 (MyBatis)", user)


@router.put("/{user_id}", response_model=Result[UserOut])
def update_user(user_id: int, body: UserUpdate, db: Session = Depends(get_db)):
    user = UserMyBatisService(db).update(user_id, username=body.username, email=body.email)
    return Result.success("更新成功 (MyBatis)", user)


@router.delete("/{user_id}", response_model=Result[None])
def delete_user(user_id: int, db: Session = Depends(get_db)):
    UserMyBatisService(db).delete(user_id)
    return Result.success("删除成功 (MyBatis)", None)
