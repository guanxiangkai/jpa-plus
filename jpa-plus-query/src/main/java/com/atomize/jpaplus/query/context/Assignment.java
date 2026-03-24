package com.atomize.jpaplus.query.context;

import com.atomize.jpaplus.query.metadata.ColumnMeta;

/**
 * UPDATE 赋值表达式
 *
 * @param column 目标列
 * @param value  赋值（null 表示 SET column = NULL）
 */
public record Assignment(ColumnMeta column, Object value) {
}

