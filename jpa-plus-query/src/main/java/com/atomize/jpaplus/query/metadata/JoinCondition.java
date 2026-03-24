package com.atomize.jpaplus.query.metadata;

/**
 * Join 条件
 *
 * @param left  左表列
 * @param right 右表列
 */
public record JoinCondition(ColumnMeta left, ColumnMeta right) {
}

