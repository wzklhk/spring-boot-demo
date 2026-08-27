package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.service.BaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 统一 REST Controller 基类 —— 实体 CRUD + 分页查询。
 *
 * 子类只需声明 @RequestMapping 路径并注入实现 BaseService 的具体 Service：
 * <pre>
 * &#64;RestController
 * &#64;RequestMapping("/api/users")
 * public class UserController extends BaseController&lt;User, Long&gt; {
 *     public UserController(UserService userService) {
 *         super(userService);
 *     }
 * }
 * </pre>
 *
 * 端点约定（列表查询默认分页：page 从 1 起缺省 1，size 缺省 10）：
 * <pre>
 *   GET    /api/{resource}       分页查询（?page=1&amp;size=10）
 *   GET    /api/{resource}/{id}  按 ID 查询
 *   POST   /api/{resource}       创建（201）
 *   PUT    /api/{resource}/{id}  更新
 *   DELETE /api/{resource}/{id}  删除
 * </pre>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public abstract class BaseController<T, ID> {

    protected final BaseService<T, ID> service;

    protected BaseController(BaseService<T, ID> service) {
        this.service = service;
    }

    /** 分页查询 —— 返回列表数据的查询接口默认分页返回 */
    @GetMapping
    public Result<PageResult<T>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.page(page, size));
    }

    @GetMapping("/{id}")
    public Result<T> getById(@PathVariable ID id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<Result<T>> create(@Valid @RequestBody T entity) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(service.create(entity)));
    }

    @PutMapping("/{id}")
    public Result<T> update(@PathVariable ID id, @Valid @RequestBody T entity) {
        return Result.updated(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable ID id) {
        service.delete(id);
        return Result.deleted();
    }
}
