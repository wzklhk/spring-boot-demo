"""用户管理接口 —— 对应 UserController（JPA 版）/api/user。"""
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from ..core.deps import get_current_user
from ..core.result import Result
from ..database import get_db
from ..schemas import PageResult, UserCreate, UserOut, UserUpdate, UserVO
from ..services.user_service import UserService

router = APIRouter(prefix="/api/user", tags=["用户管理"], dependencies=[Depends(get_current_user)])


@router.post("/query", response_model=Result[PageResult[UserVO]])
def query_users(body: UserVO, page: int = 1, size: int = 10, db: Session = Depends(get_db)):
    """统一分页查询：VO 非空字段为等值条件，空 VO {} 即普通分页查询"""
    return Result.success(UserService(db).query(body, page, size))


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