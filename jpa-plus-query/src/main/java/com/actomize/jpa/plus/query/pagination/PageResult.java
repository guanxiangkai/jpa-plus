package com.actomize.jpa.plus.query.pagination;

import java.util.List;

/**
 * 分页结果（不可变值对象）
 *
 * <p>封装分页查询的完整结果信息，使用 Java {@code record} 保证不可变性。</p>
 *
 * @param records 当前页数据列表
 * @param total   总记录数（用于计算总页数）
 * @param page    当前页码（从 1 开始）
 * @param size    每页大小
 * @param <T>     数据类型
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public record PageResult<T>(
        List<T> records,
        long total,
        int page,
        int size
) {

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }

    public boolean hasNext() {
        return (long) page * size < total;
    }

    public boolean hasPrev() {
        return page > 1;
    }

    public int totalPages() {
        return (int) Math.ceil((double) total / size);
    }
}
