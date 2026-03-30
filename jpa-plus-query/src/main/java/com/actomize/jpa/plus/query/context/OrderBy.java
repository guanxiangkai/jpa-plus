package com.actomize.jpa.plus.query.context;

import com.actomize.jpa.plus.query.metadata.ColumnMeta;

/**
 * 排序
 *
 * @param column    排序列
 * @param direction 排序方向
 */
public record OrderBy(ColumnMeta column, Direction direction) {

    public enum Direction {
        ASC, DESC
    }
}

