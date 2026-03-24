package com.atomize.jpaplus.query.ast;

import com.atomize.jpaplus.query.metadata.ColumnMeta;

/**
 * 大于条件：column > value
 */
public record Gt(ColumnMeta column, Object value) implements Condition {
    @Override
    public <R> R accept(ConditionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

