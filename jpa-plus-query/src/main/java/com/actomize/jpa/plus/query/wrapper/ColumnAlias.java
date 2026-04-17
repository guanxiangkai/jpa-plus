package com.actomize.jpa.plus.query.wrapper;

import com.actomize.jpa.plus.query.metadata.ColumnMeta;

/**
 * 列别名包装器
 */
public record ColumnAlias<T>(ColumnMeta meta, String alias) {

    /**
     * 指定别名
     */
    public ColumnAlias<T> as(String alias) {
        return new ColumnAlias<>(meta, alias);
    }
}

