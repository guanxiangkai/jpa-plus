package com.atomize.jpa.plus.query.ast;

import com.atomize.jpa.plus.query.metadata.ColumnMeta;

/**
 * BETWEEN 条件：column BETWEEN start AND end
 */
public record Between(ColumnMeta column, Object start, Object end) implements Condition {
    @Override
    public <R> R accept(ConditionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

