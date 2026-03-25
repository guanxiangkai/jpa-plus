package com.atomize.jpa.plus.permission.interceptor;

import com.atomize.jpa.plus.core.interceptor.Chain;
import com.atomize.jpa.plus.core.interceptor.DataInterceptor;
import com.atomize.jpa.plus.core.interceptor.Phase;
import com.atomize.jpa.plus.core.model.DataInvocation;
import com.atomize.jpa.plus.core.model.OperationType;
import com.atomize.jpa.plus.permission.annotation.DataScope;
import com.atomize.jpa.plus.permission.enums.DataScopeType;
import com.atomize.jpa.plus.permission.handler.DataScopeHandler;
import com.atomize.jpa.plus.query.ast.Condition;
import com.atomize.jpa.plus.query.ast.Conditions;
import com.atomize.jpa.plus.query.ast.Eq;
import com.atomize.jpa.plus.query.ast.In;
import com.atomize.jpa.plus.query.context.QueryContext;
import com.atomize.jpa.plus.query.context.QueryRuntime;
import com.atomize.jpa.plus.query.metadata.ColumnMeta;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 数据权限拦截器
 *
 * <p>在查询前根据实体类上的 {@link DataScope} 注解和 {@link DataScopeType} 自动注入
 * 行级数据权限条件到 AST，实现多粒度的数据权限控制。</p>
 *
 * <h3>权限范围与生成条件</h3>
 * <table>
 *   <tr><th>DataScopeType</th><th>生成条件</th></tr>
 *   <tr><td>{@link DataScopeType#ALL}</td><td>不追加任何条件</td></tr>
 *   <tr><td>{@link DataScopeType#DEPT}</td><td>{@code dept_id = :currentDeptId}</td></tr>
 *   <tr><td>{@link DataScopeType#DEPT_AND_CHILD}</td><td>{@code dept_id IN (:deptIds)}</td></tr>
 *   <tr><td>{@link DataScopeType#SELF}</td><td>{@code create_by = :currentUserId}</td></tr>
 *   <tr><td>{@link DataScopeType#CUSTOM}</td><td>由 {@link DataScopeHandler#customCondition} 决定</td></tr>
 * </table>
 *
 * <p><b>设计模式：</b>
 * <ul>
 *   <li>模板方法模式（Template Method） —— 定义权限注入流程，具体数据由 Handler 提供</li>
 *   <li>策略模式（Strategy） —— 通过 {@link DataScopeType} 选择不同的条件构建策略</li>
 * </ul>
 * </p>
 *
 * @author guanxiangkai
 * @see DataScope
 * @see DataScopeType
 * @see DataScopeHandler
 * @since 2026年03月25日 星期二
 */
@Slf4j
public class PermissionInterceptor implements DataInterceptor {

    private final DataScopeHandler handler;

    /**
     * 缓存每个实体类的 @DataScope 注解（避免重复反射扫描）
     */
    private final Map<Class<?>, DataScope> annotationCache = new ConcurrentHashMap<>();

    public PermissionInterceptor(DataScopeHandler handler) {
        this.handler = Objects.requireNonNull(handler, "DataScopeHandler must not be null");
    }

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
            Class<?> entityClass = ctx.metadata().root().entityClass();
            DataScope scope = resolveDataScope(entityClass);

            if (scope != null && scope.type() != DataScopeType.ALL) {
                Condition permission = buildCondition(scope, ctx);
                if (permission != null) {
                    Condition combined = Conditions.and(ctx.runtime().getWhere(), permission);
                    QueryRuntime newRuntime = ctx.runtime().withWhere(combined);
                    invocation = invocation.withQueryModel(ctx.withRuntime(newRuntime));

                    if (log.isDebugEnabled()) {
                        log.debug("DataScope: entity={}, type={}", entityClass.getSimpleName(), scope.type());
                    }
                }
            }
        }
        return chain.proceed(invocation);
    }

    /**
     * 解析实体类上的 {@link DataScope} 注解（带缓存）
     */
    private DataScope resolveDataScope(Class<?> entityClass) {
        if (annotationCache.containsKey(entityClass)) {
            return annotationCache.get(entityClass);
        }
        DataScope scope = entityClass.getAnnotation(DataScope.class);
        if (scope != null) {
            annotationCache.put(entityClass, scope);
        }
        return scope;
    }

    /**
     * 根据 {@link DataScopeType} 构建对应的权限条件
     */
    private Condition buildCondition(DataScope scope, QueryContext ctx) {
        return switch (scope.type()) {
            case ALL -> null;

            case DEPT -> {
                Object deptId = handler.getCurrentDeptId();
                yield deptId != null
                        ? new Eq(ColumnMeta.of(ctx.metadata().root(), scope.deptColumn(), deptId.getClass()), deptId)
                        : null;
            }

            case DEPT_AND_CHILD -> {
                Collection<?> deptIds = handler.getDeptAndChildIds();
                yield (deptIds != null && !deptIds.isEmpty())
                        ? new In(ColumnMeta.of(ctx.metadata().root(), scope.deptColumn(), Long.class), deptIds)
                        : null;
            }

            case SELF -> {
                Object userId = handler.getCurrentUserId();
                yield userId != null
                        ? new Eq(ColumnMeta.of(ctx.metadata().root(), scope.userColumn(), userId.getClass()), userId)
                        : null;
            }

            case CUSTOM -> handler.customCondition(ctx.metadata().root().entityClass());
        };
    }
}
