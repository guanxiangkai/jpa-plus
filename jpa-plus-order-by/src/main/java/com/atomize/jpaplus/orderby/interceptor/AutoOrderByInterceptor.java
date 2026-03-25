package com.atomize.jpaplus.orderby.interceptor;

import com.atomize.jpaplus.core.interceptor.Chain;
import com.atomize.jpaplus.core.interceptor.DataInterceptor;
import com.atomize.jpaplus.core.interceptor.Phase;
import com.atomize.jpaplus.core.model.DataInvocation;
import com.atomize.jpaplus.core.model.OperationType;
import com.atomize.jpaplus.core.util.NamingUtils;
import com.atomize.jpaplus.orderby.annotation.AutoOrderBy;
import com.atomize.jpaplus.query.context.OrderBy;
import com.atomize.jpaplus.query.context.QueryContext;
import com.atomize.jpaplus.query.context.QueryRuntime;
import com.atomize.jpaplus.query.metadata.ColumnMeta;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动排序拦截器
 *
 * <p>在查询操作前检测是否已有显式排序，若无则根据实体类上的 {@link AutoOrderBy}
 * 注解自动注入默认 ORDER BY 子句到 AST。</p>
 *
 * <p><b>执行时机：</b>BEFORE 阶段，order = 250（在逻辑删除之后、核心执行之前）</p>
 *
 * <p><b>核心逻辑：</b>
 * <ol>
 *   <li>仅对 QUERY 操作生效</li>
 *   <li>若 {@link QueryRuntime#orderBys()} 非空（用户已显式指定排序），直接跳过</li>
 *   <li>扫描实体类（含父类）中标注了 {@code @AutoOrderBy} 的字段</li>
 *   <li>按 {@link AutoOrderBy#priority()} 排序后，构建 {@link OrderBy} 列表</li>
 *   <li>将排序列表注入 {@link QueryRuntime}，生成新的 {@link QueryContext}</li>
 * </ol>
 * </p>
 *
 * <p><b>设计模式：</b>责任链模式（Chain of Responsibility） —— 作为拦截器链中的一环</p>
 *
 * @author guanxiangkai
 * @see AutoOrderBy
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class AutoOrderByInterceptor implements DataInterceptor {

    /**
     * 缓存每个实体类的默认排序规则（避免重复反射扫描）
     */
    private final Map<Class<?>, List<OrderByMeta>> cache = new ConcurrentHashMap<>();

    @Override
    public int order() {
        return 250;
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
            // 若已有显式排序，跳过
            if (!ctx.runtime().getOrderBys().isEmpty()) {
                return chain.proceed(invocation);
            }

            // 获取实体类的默认排序配置
            List<OrderByMeta> metas = cache.computeIfAbsent(
                    invocation.entityClass(), this::resolveOrderByMetas
            );

            if (!metas.isEmpty()) {
                List<OrderBy> defaultOrderBys = metas.stream()
                        .map(meta -> new OrderBy(
                                ColumnMeta.of(ctx.metadata().root(), meta.columnName(), meta.field().getType()),
                                meta.direction() == AutoOrderBy.Direction.ASC
                                        ? OrderBy.Direction.ASC
                                        : OrderBy.Direction.DESC
                        ))
                        .toList();

                QueryRuntime newRuntime = ctx.runtime().withOrderBys(defaultOrderBys);
                invocation = invocation.withQueryModel(ctx.withRuntime(newRuntime));

                if (log.isDebugEnabled()) {
                    log.debug("AutoOrderBy: entity={}, orders={}",
                            invocation.entityClass().getSimpleName(),
                            metas.stream().map(m -> m.columnName() + " " + m.direction()).toList());
                }
            }
        }
        return chain.proceed(invocation);
    }

    /**
     * 扫描实体类及其父类，提取所有 @AutoOrderBy 注解的字段信息
     */
    private List<OrderByMeta> resolveOrderByMetas(Class<?> entityClass) {
        List<OrderByMeta> metas = new java.util.ArrayList<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                AutoOrderBy annotation = field.getAnnotation(AutoOrderBy.class);
                if (annotation != null) {
                    String columnName = annotation.column().isEmpty()
                            ? NamingUtils.camelToSnake(field.getName())
                            : annotation.column();
                    metas.add(new OrderByMeta(field, columnName, annotation.direction(), annotation.priority()));
                }
            }
            current = current.getSuperclass();
        }
        // 按 priority 排序
        metas.sort(Comparator.comparingInt(OrderByMeta::priority));
        return List.copyOf(metas);
    }

    /**
     * 排序元数据（内部缓存用）
     */
    private record OrderByMeta(
            Field field,
            String columnName,
            AutoOrderBy.Direction direction,
            int priority
    ) {
    }
}

