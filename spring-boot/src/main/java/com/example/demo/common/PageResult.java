package com.example.demo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页响应 —— 所有列表类查询接口默认返回分页数据。
 * page 从 1 起（缺省第 1 页），size 每页条数（缺省 10）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数 */
    private long total;

    /** 当前页码（1 起） */
    private int page;

    /** 每页条数 */
    private int size;

    /** 总页数 */
    private int pages;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        int pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return PageResult.<T>builder()
                .list(list)
                .total(total)
                .page(page)
                .size(size)
                .pages(pages)
                .build();
    }
}
