package com.atomize.jpa.plus.logicdelete.interceptor;

import com.atomize.jpa.plus.core.interceptor.Chain;
import com.atomize.jpa.plus.core.interceptor.DataInterceptor;
import com.atomize.jpa.plus.core.interceptor.Phase;
import com.atomize.jpa.plus.core.model.DataInvocation;
import com.atomize.jpa.plus.core.model.OperationType;
import com.atomize.jpa.plus.core.util.NamingUtils;
import com.atomize.jpa.plus.logicdelete.annotation.LogicDelete;
import com.atomize.jpa.plus.logicdelete.handler.LogicDeleteFieldHandler;
import com.atomize.jpa.plus.query.ast.Condition;
import com.atomize.jpa.plus.query.ast.Conditions;
import com.atomize.jpa.plus.query.ast.Eq;
import com.atomize.jpa.plus.query.context.QueryContext;
import com.atomize.jpa.plus.query.context.QueryRuntime;
import com.atomize.jpa.plus.query.metadata.ColumnMeta;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 逻辑删除拦截器
 *
 * <p>自动扫描实体类中标注了 {@link LogicDelete} 的字段，根据字段类型推导
 * 未删除值和列名，在查询/删除时注入对应条件到 AST。</p>
 *
 * <p><b>无需任何构造参数</b> —— 所有信息从注解 + 字段类型自动获取。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class LogicDeleteInterceptor implements DataInterceptor {

    /**
     * 缓存每个实体类的逻辑删除元数据
     */
    private final Map<Class<?>, Optional<LogicDeleteMeta>> cache = new ConcurrentHashMap<>();

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
            Optional<LogicDeleteMeta> metaOpt = cache.computeIfAbsent(
                    invocation.entityClass(), this::resolveLogicDeleteMeta
            );

            if (metaOpt.isPresent()) {
                LogicDeleteMeta meta = metaOpt.get();
                Condition logicDeleteCondition = new Eq(
                        ColumnMeta.of(ctx.metadata().root(), meta.columnName(), meta.fieldType()),
                        meta.notDeletedValue()
                );
                Condition combined = Conditions.and(ctx.runtime().where(), logicDeleteCondition);
                QueryRuntime newRuntime = ctx.runtime().withWhere(combined);
                invocation = invocation.withQueryModel(ctx.withRuntime(newRuntime));
            }
        }
        return chain.proceed(invocation);
    }

    /**
     * 扫描实体类（含父类），查找 @LogicDelete 注解字段
     */
    private Optional<LogicDeleteMeta> resolveLogicDeleteMeta(Class<?> entityClass) {
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                LogicDelete annotation = field.getAnnotation(LogicDelete.class);
                if (annotation != null) {
                    String columnName = NamingUtils.camelToSnake(field.getName());
                    Object notDeletedValue = LogicDeleteFieldHandler.resolveNotDeletedValue(field.getType(), annotation);

                    if (log.isDebugEnabled()) {
                        log.debug("LogicDelete resolved: entity={}, column={}, type={}, notDeletedValue={}",
                                entityClass.getSimpleName(), columnName, field.getType().getSimpleName(), notDeletedValue);
                    }
                    return Optional.of(new LogicDeleteMeta(columnName, field.getType(), notDeletedValue));
                }
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    /**
     * 逻辑删除元数据
     */
    private record LogicDeleteMeta(String columnName, Class<?> fieldType, Object notDeletedValue) {
    }
}
