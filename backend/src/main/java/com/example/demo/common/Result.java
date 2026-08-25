package com.example.demo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 统一响应包装类 —— 所有 REST 接口返回 {code, message, data[, errors]}
 * code: 200=成功 201=创建成功 400=业务/参数错误
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;

    /** 参数校验失败时的字段错误信息（可选） */
    private Map<String, String> errors;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder().code(200).message("success").data(data).build();
    }

    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder().code(200).message(message).data(data).build();
    }

    public static <T> Result<T> created(T data) {
        return Result.<T>builder().code(201).message("创建成功").data(data).build();
    }

    public static <T> Result<T> updated(T data) {
        return Result.<T>builder().code(200).message("更新成功").data(data).build();
    }

    public static <T> Result<T> deleted() {
        return Result.<T>builder().code(200).message("删除成功").build();
    }

    public static <T> Result<T> error(int code, String message) {
        return Result.<T>builder().code(code).message(message).build();
    }

    public static <T> Result<T> error(int code, String message, Map<String, String> errors) {
        return Result.<T>builder().code(code).message(message).errors(errors).build();
    }
}
