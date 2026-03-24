package com.atomize.jpaplus.permission.interceptor;

import com.atomize.jpaplus.core.interceptor.Chain;
import com.atomize.jpaplus.core.interceptor.DataInterceptor;
import com.atomize.jpaplus.core.interceptor.Phase;
import com.atomize.jpaplus.core.model.DataInvocation;
import com.atomize.jpaplus.core.model.OperationType;
import com.atomize.jpaplus.query.ast.Condition;
import com.atomize.jpaplus.query.ast.Conditions;
import com.atomize.jpaplus.query.context.QueryContext;
import com.atomize.jpaplus.query.context.QueryRuntime;
import lombok.extern.slf4j.Slf4j;


/**
 * 数据权限拦截器
 *
 * <p>在查询前自动注入数据权限条件到 AST，实现行级数据权限控制。
 * 用户需继承此类并覆盖 {@link #getPermissionCondition(Class)} 方法，
 * 根据当前用户上下文构建权限条件。</p>
 *
 * <p><b>设计模式：</b>模板方法模式（Template Method） —— 定义权限注入流程，子类实现具体权限逻辑</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class PermissionInterceptor implements DataInterceptor {

    @Override
    public int order() {
        return 100;
    }

    @Override
    public Phase phase() {
        return Phase.BEFORE;
    }

    @Override
    public boolean supports(OperationType type) {
        return type == OperationType.QUERY;
    }

    @Override
    public Object intercept(DataInvocation invocation, Chain chain) throws Throwable {
        if (invocation.queryModel() instanceof QueryContext ctx) {
            Condition permission = getPermissionCondition(ctx.metadata().root().entityClass());
            if (permission != null) {
                Condition combined = Conditions.and(ctx.runtime().getWhere(), permission);
                QueryRuntime newRuntime = ctx.runtime().withWhere(combined);
                invocation = invocation.withQueryModel(ctx.withRuntime(newRuntime));
            }
        }
        return chain.proceed(invocation);
    }

    /**
     * 获取数据权限条件（模板方法 —— 子类必须覆盖）
     *
     * @param entityClass 实体类
     * @return 权限条件 AST 节点，{@code null} 表示无限制
     */
    protected Condition getPermissionCondition(Class<?> entityClass) {
        return null;
    }
}

