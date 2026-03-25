package com.atomize.jpa.plus.permission.interceptor;

import com.atomize.jpa.plus.core.interceptor.Chain;
import com.atomize.jpa.plus.core.interceptor.DataInterceptor;
import com.atomize.jpa.plus.core.interceptor.Phase;
import com.atomize.jpa.plus.core.model.DataInvocation;
import com.atomize.jpa.plus.core.model.OperationType;
import com.atomize.jpa.plus.permission.annotation.DataScope;
import com.atomize.jpa.plus.permission.enums.DataScopeEnum;
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
 * 数据权限拦截���
 *
 * <p>在查询前根据实体类上的 {@link DataScope} 注解自动注入
 * 行级数据权限条件到 AST，实现多粒度的数据权限控制。
 * 支持内置 {@link DataScopeType} 和用户自定义 {@link DataScopeEnum} 实现。</p>
 *
 * @author guanxiangkai
 * @see DataScope
 * @see DataScopeEnum
 * @see DataScopeHandler
 * @since 2026年03月25日 星期二
 */
@Slf4j
public class PermissionInterceptor implements DataInterceptor {

    private final DataScopeHandler handler;

    private final Map<Class<?>, DataScope> annotationCache = new ConcurrentHashMap<>();
    private final Map<Class<? extends DataScopeEnum>, DataScopeEnum> enumCache = new ConcurrentHashMap<>();

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

            if (scope != null) {
                DataScopeEnum scopeEnum = resolveScopeEnum(scope);
                if (!scopeEnum.skipFilter()) {
                    Condition permission = buildCondition(scope, scopeEnum, ctx);
                    if (permission != null) {
                        Condition combined = Conditions.and(ctx.runtime().getWhere(), permission);
                        QueryRuntime newRuntime = ctx.runtime().withWhere(combined);
                        invocation = invocation.withQueryModel(ctx.withRuntime(newRuntime));

                        if (log.isDebugEnabled()) {
                            log.debug("DataScope: entity={}, scope={}", entityClass.getSimpleName(), scopeEnum.scopeName());
                        }
                    }
                }
            }
        }
        return chain.proceed(invocation);
    }

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
     * 解析 DataScopeEnum：customType 优先于 type
     */
    private DataScopeEnum resolveScopeEnum(DataScope scope) {
        Class<? extends DataScopeEnum> customClass = scope.customType();
        if (customClass != DataScopeEnum.class) {
            return enumCache.computeIfAbsent(customClass, this::instantiate);
        }
        return scope.type();
    }

    /**
     * 根据 scope 类型构建对应的权限条件
     */
    private Condition buildCondition(DataScope scope, DataScopeEnum scopeEnum, QueryContext ctx) {
        // 内置类型走 switch 分派
        if (scopeEnum instanceof DataScopeType builtIn) {
            return switch (builtIn) {
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

        // 自定义类型：统一走 handler.customCondition()
        return handler.customCondition(ctx.metadata().root().entityClass());
    }

    private DataScopeEnum instantiate(Class<? extends DataScopeEnum> clazz) {
        try {
            if (clazz.isEnum()) {
                DataScopeEnum[] constants = clazz.getEnumConstants();
                if (constants.length > 0) return constants[0];
            }
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot instantiate DataScopeEnum: " + clazz.getName(), e);
        }
    }
}
