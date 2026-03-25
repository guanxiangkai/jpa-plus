package com.atomize.jpa.plus.query.wrapper;

import com.atomize.jpa.plus.query.metadata.ColumnMeta;

/**
 * SELECT 列
 *
 * @param column 列元数据（可能带别名）
 */
public record SelectColumn(ColumnMeta column) {
}

