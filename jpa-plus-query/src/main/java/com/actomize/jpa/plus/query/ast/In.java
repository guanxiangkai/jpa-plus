package com.actomize.jpa.plus.query.ast;

import com.actomize.jpa.plus.query.metadata.ColumnMeta;

import java.util.Collection;

/**
 * IN 条件：column IN (values)
 */
public record In(ColumnMeta column, Collection<?> values) implements Condition {
    @Override
    public <R> R accept(ConditionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

