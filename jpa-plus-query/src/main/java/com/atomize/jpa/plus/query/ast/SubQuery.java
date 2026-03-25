package com.atomize.jpa.plus.query.ast;

import com.atomize.jpa.plus.query.context.QueryContext;

/**
 * 子查询条件
 */
public record SubQuery(QueryContext query, Operator operator) implements Condition {
    @Override
    public <R> R accept(ConditionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

