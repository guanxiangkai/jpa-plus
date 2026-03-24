package com.atomize.jpaplus.query.wrapper;

import com.atomize.jpaplus.query.metadata.ColumnMeta;

/**
 * SELECT 列
 *
 * @param column 列元数据（可能带别名）
 */
public record SelectColumn(ColumnMeta column) {
}

