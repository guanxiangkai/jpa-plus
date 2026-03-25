package com.atomize.jpa.plus.logicdelete.interceptor;

import com.atomize.jpa.plus.core.interceptor.Chain;
import com.atomize.jpa.plus.core.interceptor.DataInterceptor;
import com.atomize.jpa.plus.core.interceptor.Phase;
import com.atomize.jpa.plus.core.model.DataInvocation;
import com.atomize.jpa.plus.core.model.OperationType;
import com.atomize.jpa.plus.query.ast.Condition;
import com.atomize.jpa.plus.query.ast.Conditions;
import com.atomize.jpa.plus.query.ast.Eq;
import com.atomize.jpa.plus.query.context.QueryContext;
import com.atomize.jpa.plus.query.context.QueryRuntime;
import com.atomize.jpa.plus.query.metadata.ColumnMeta;
import lombok.extern.slf4j.Slf4j;


/**
 * 逻辑删除拦截器
 *
 * <p>在查询和删除操作前自动注入逻辑删除条件到 AST：
 * <ul>
 *   <li>查询：追加 {@code deleted = 0} 条件，过滤已删除数据</li>
 *   <li>删除：追加 {@code deleted = 0} 条件（后续可扩展为 UPDATE 改写）</li>
 * </ul>
 * </p>
 *
 * <p><b>设计模式：</b>责任链模式（Chain of Responsibility） —— 作为拦截器链中的一环</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class LogicDeleteInterceptor implements DataInterceptor {

    /**
     * 逻辑删除列名
     */
    private final String deletedColumn;
    /**
     * 未删除的值
     */
    private final Object notDeletedValue;

    public LogicDeleteInterceptor() {
        this("deleted", 0);
    }

    public LogicDeleteInterceptor(String deletedColumn, Object notDeletedValue) {
        this.deletedColumn = deletedColumn;
        this.notDeletedValue = notDeletedValue;
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public Phase phase() {
        return Phase.BEFORE;
    }

    @Override
    public boolean supports(OperationType type) {
        return type == OperationType.QUERY || type == OperationType.DELETE;
    }

    @Override
    public Object intercept(DataInvocation invocation, Chain chain) throws Throwable {
        if (invocation.queryModel() instanceof QueryContext ctx) {
            Condition logicDeleteCondition = new Eq(
                    ColumnMeta.of(ctx.metadata().root(), deletedColumn, Integer.class),
                    notDeletedValue
            );
            Condition combined = Conditions.and(ctx.runtime().getWhere(), logicDeleteCondition);
            QueryRuntime newRuntime = ctx.runtime().withWhere(combined);
            invocation = invocation.withQueryModel(ctx.withRuntime(newRuntime));
        }
        return chain.proceed(invocation);
    }
}

