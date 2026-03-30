package com.actomize.jpa.plus.query.ast;

import com.actomize.jpa.plus.query.metadata.ColumnMeta;

/**
 * 大于条件：{@code column > value}
 */
public record Gt(ColumnMeta column, Object value) implements Condition {
    @Override
    public <R> R accept(ConditionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

