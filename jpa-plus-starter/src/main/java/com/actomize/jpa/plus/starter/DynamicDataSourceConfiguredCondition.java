package com.actomize.jpa.plus.starter;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;


/**
 * 自定义条件：检测 {@code spring.datasource.dynamic.datasource} 下是否至少配置了一个数据源
 *
 * <p>{@code @ConditionalOnProperty} 无法检测 Map 类型属性，
 * 因此需要使用 {@link Binder} 来判断是否存在配置。</p>
 *
 * <p>替代原来的 {@code @ConditionalOnProperty(prefix = "spring.datasource.dynamic", name = "datasource")}。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月26日 星期四
 */
class DynamicDataSourceConfiguredCondition implements Condition {

    private static final String DATASOURCE_PREFIX = "spring.datasource.dynamic.datasource";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Binder.get(context.getEnvironment())
                .bind(DATASOURCE_PREFIX, Bindable.mapOf(String.class, Object.class))
                .map(map -> !map.isEmpty())
                .orElse(false);
    }
}


