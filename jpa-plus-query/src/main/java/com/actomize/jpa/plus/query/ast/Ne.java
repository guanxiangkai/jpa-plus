package com.actomize.jpa.plus.query.ast;

import com.actomize.jpa.plus.query.metadata.ColumnMeta;

/**
 * 不等于条件：column != value
 */
public record Ne(ColumnMeta column, Object value) implements Condition {
    @Override
    public <R> R accept(ConditionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

