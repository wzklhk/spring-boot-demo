"""用户管理接口 —— 对应 UserController（JPA 版）/api/users。"""
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from ..core.deps import get_current_user
from ..core.result import Result
from ..database import get_db
from ..schemas import UserCreate, UserOut, UserUpdate
from ..services.user_service import UserService

router = APIRouter(prefix="/api/users", tags=["用户管理"], dependencies=[Depends(get_current_user)])


@router.get("", response_model=Result[list[UserOut]])
def list_users(db: Session = Depends(get_db)):
    return Result.success(UserService(db).find_all())


@router.get("/{user_id}", response_model=Result[UserOut])
def get_user(user_id: int, db: Session = Depends(get_db)):
    return Result.success(UserService(db).find_by_id(user_id))


@router.post("", response_model=Result[UserOut], status_code=status.HTTP_201_CREATED)
def create_user(body: UserCreate, db: Session = Depends(get_db)):
    user = UserService(db).create(username=body.username, email=body.email, password=body.password)
    return Result.created(user)


@router.put("/{user_id}", response_model=Result[UserOut])
def update_user(user_id: int, body: UserUpdate, db: Session = Depends(get_db)):
    user = UserService(db).update(user_id, username=body.username, email=body.email)
    return Result.updated(user)


@router.delete("/{user_id}", response_model=Result[None])
def delete_user(user_id: int, db: Session = Depends(get_db)):
    UserService(db).delete(user_id)
    return Result.deleted()
