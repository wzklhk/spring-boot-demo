package com.example.demo.mapper;

import com.example.demo.pojo.vo.UserPermissionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * RBAC 聚合查询 Mapper（MyBatis）
 * 多表 join 场景用 MyBatis XML 编写，与 JPA 的 Repository 共存。
 * SQL 定义见 src/main/resources/mapper/RbacMapper.xml
 */
public interface RbacMapper {

    /** 查询用户拥有的全部权限（经 用户→角色→权限 三表 join），含角色来源 */
    List<UserPermissionVO> findUserPermissions(@Param("userId") Long userId);

    /** 查询角色拥有的权限数量（校验用） */
    int countPermissionsByRoleId(@Param("roleId") Long roleId);
}
