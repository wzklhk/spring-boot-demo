package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.service.BaseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 统一 REST Controller 基类 —— 实体 CRUD + 统一分页查询。
 *
 * 子类只需声明 @RequestMapping 路径并注入实现 BaseService 的具体 Service：
 * <pre>
 * &#64;RestController
 * &#64;RequestMapping("/api/user")
 * public class UserController extends BaseController&lt;User, Long, UserVO, UserVO&gt; {
 *     public UserController(UserService userService) {
 *         super(userService);
 *     }
 * }
 * </pre>
 *
 * 分页查询与条件分页查询合并为同一个 API：
 * <pre>
 *   POST   /api/{resource}/query      统一分页查询：请求体传实体 VO（非空字段作为等值条件；
 *                                     传空 VO {} 即退化为普通分页查询）
 *                                     分页参数 page 从 1 起（缺省 1）、size 每页条数（缺省 10）
 *   GET    /api/{resource}/{id}       按 ID 查询
 *   POST   /api/{resource}            创建（201）
 *   PUT    /api/{resource}/{id}       更新
 *   DELETE /api/{resource}/{id}       删除
 * </pre>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @param <Q>  查询入参类型（实体 VO）
 * @param <V>  查询返回类型（实体 VO）
 */
public abstract class BaseController<T, ID, Q, V> {

    protected final BaseService<T, ID, Q, V> service;

    protected BaseController(BaseService<T, ID, Q, V> service) {
        this.service = service;
    }

    /** 统一分页查询 —— 请求体传实体 VO（非空字段作为等值条件），传空 VO {} 即普通分页查询 */
    @PostMapping("/query")
    @Operation(summary = "分页查询（支持条件）",
            description = "普通分页与条件分页合并：请求体传实体 VO，非空字段作为等值查询条件；"
                    + "传空 VO {} 即退化为普通分页查询。page 从 1 起（缺省第 1 页），size 每页条数（缺省 10）")
    public Result<PageResult<V>> query(@RequestBody Q query,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.query(query, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "按 ID 查询", description = "查询单条记录，不存在时返回业务错误（code=400）")
    public Result<T> getById(@PathVariable ID id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建", description = "创建一条新记录，成功返回 201")
    public ResponseEntity<Result<T>> create(@Valid @RequestBody T entity) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(service.create(entity)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新", description = "按 ID 更新记录（未传字段保持原值；用户密码不可经此修改）")
    public Result<T> update(@PathVariable ID id, @Valid @RequestBody T entity) {
        return Result.updated(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除", description = "按 ID 删除记录（含关联数据级联清理）")
    public Result<Void> delete(@PathVariable ID id) {
        service.delete(id);
        return Result.deleted();
    }
}