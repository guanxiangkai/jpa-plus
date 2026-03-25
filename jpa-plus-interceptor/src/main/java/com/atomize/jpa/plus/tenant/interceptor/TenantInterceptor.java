package com.atomize.jpa.plus.tenant.interceptor;

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
 * 多租户拦截器
 *
 * <p>在所有数据操作前自动注入 {@code tenant_id = :tenantId} 条件，实现数据隔离。
 * 用户需继承此类并覆盖 {@link #getCurrentTenantId()} 方法，从安全上下文中获取当前租户 ID。</p>
 *
 * <p><b>设计模式：</b>模板方法模式（Template Method） —— 子类实现租户 ID 获取逻辑</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class TenantInterceptor implements DataInterceptor {

    @Override
    public int order() {
        return 150;
    }

    @Override
    public Phase phase() {
        return Phase.BEFORE;
    }

    @Override
    public boolean supports(OperationType type) {
        return true;
    }

    @Override
    public Object intercept(DataInvocation invocation, Chain chain) throws Throwable {
        if (invocation.queryModel() instanceof QueryContext ctx) {
            String tenantId = getCurrentTenantId();
            if (tenantId != null) {
                Condition tenantCondition = new Eq(
                        ColumnMeta.of(ctx.metadata().root(), "tenant_id", String.class),
                        tenantId
                );
                Condition combined = Conditions.and(ctx.runtime().where(), tenantCondition);
                QueryRuntime newRuntime = ctx.runtime().withWhere(combined);
                invocation = invocation.withQueryModel(ctx.withRuntime(newRuntime));
            }
        }
        return chain.proceed(invocation);
    }

    /**
     * 获取当前租户 ID（模板方法 —— 子类必须覆盖）
     *
     * @return 租户 ID，{@code null} 表示不启用租户隔离
     */
    protected String getCurrentTenantId() {
        return null;
    }
}

