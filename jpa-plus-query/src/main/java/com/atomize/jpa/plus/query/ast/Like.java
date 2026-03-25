package com.atomize.jpa.plus.query.ast;

import com.atomize.jpa.plus.query.metadata.ColumnMeta;

/**
 * LIKE 条件：column LIKE pattern
 */
public record Like(ColumnMeta column, String value, LikeMode mode) implements Condition {
    @Override
    public <R> R accept(ConditionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    /**
     * 根据模式生成完整的 LIKE 值
     */
    public String pattern() {
        return switch (mode) {
            case START -> value + "%";
            case END -> "%" + value;
            case ANYWHERE -> "%" + value + "%";
        };
    }
}

