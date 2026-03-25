package com.atomize.jpa.plus.query.ast;

import com.atomize.jpa.plus.query.metadata.ColumnMeta;

/**
 * 大于等于条件：{@code column >= value}
 */
public record Ge(ColumnMeta column, Object value) implements Condition {
    @Override
    public <R> R accept(ConditionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

