package com.actomize.jpa.plus.starter;

import com.actomize.jpa.plus.core.metrics.JpaPlusMetrics;
import com.actomize.jpa.plus.starter.repository.JpaPlusRepositoryFactoryBean;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

/**
 * JPA-Plus Spring Boot 自动装配配置（总入口 —— 薄编排层）
 *
 * <p>本类作为自动装配的统一入口，通过 {@code @Import} 引入各功能模块的独立自动装配类：</p>
 * <ul>
 *   <li>{@link JpaPlusQueryAutoConfiguration}   —— SQL 编译器、分页优化器、查询执行器</li>
 *   <li>{@link JpaPlusFieldAutoConfiguration}   —— 字段处理器（ID、填充、加密、字典等）及字段引擎</li>
 *   <li>{@link JpaPlusInterceptorAutoConfiguration} —— 自动排序、多租户等内置拦截器</li>
 *   <li>{@link JpaPlusAuditAutoConfiguration}       —— 审计事件发布、错误处理、AuditInterceptor</li>
 *   <li>{@link JpaPlusCoreAutoConfiguration}    —— 拦截器链、Flush 策略、统一执行器</li>
 * </ul>
 *
 * <p>本类自身只保留两个职责：</p>
 * <ol>
 *   <li>Repository Factory 替换（{@link JpaPlusRepositoryFactoryBean}）</li>
 *   <li>Micrometer 可观测性指标（可选）</li>
 * </ol>
 *
 * <p><b>新增配置项：</b></p>
 * <pre>{@code
 * jpa-plus:
 *   dialect: mysql
 *   debug.enabled: false
 *   debug.print-params: true
 *   debug.slow-sql.enabled: false
 *   debug.slow-sql.threshold: 1000
 *   metrics.prefix: jpa.plus
 *   flush-mode: AUTO
 *   pagination.count-strategy: SIMPLE
 *   id-generator.type: AUTO
 *   id-generator.snowflake.worker-id: 1
 *   id-generator.snowflake.datacenter-id: 1
 *   encrypt.key: JpaPlusEncKey128
 *   dict.jdbc.enabled: false
 *   dict.cache.enabled: true
 *   dict.cache.ttl-seconds: 300
 *   tenant.column: tenant_id
 *   audit.async.enabled: false
 *   audit.method.enabled: true
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@AutoConfiguration(after = JpaPlusAuditAutoConfiguration.class)
@ConditionalOnProperty(name = "jpa-plus.enabled", havingValue = "true", matchIfMissing = true)
@Import({
        JpaPlusQueryAutoConfiguration.class,
        JpaPlusFieldAutoConfiguration.class,
        JpaPlusInterceptorAutoConfiguration.class,
        JpaPlusCoreAutoConfiguration.class
})
@Slf4j
public class JpaPlusAutoConfiguration {

    // ─────────── Repository 工厂替换 ───────────

    /**
     * 自动将 Spring Data JPA 默认的 {@link JpaRepositoryFactoryBean}
     * 替换为 {@link JpaPlusRepositoryFactoryBean}，
     * 使所有 Repository 自动获得 jpa-plus 增强能力（拦截器链、字段处理等）。
     */
    @Bean
    static BeanDefinitionRegistryPostProcessor jpaPlusRepositoryFactoryReplacer() {
        return (BeanDefinitionRegistry registry) -> {
            String target = JpaRepositoryFactoryBean.class.getName();
            String replacement = JpaPlusRepositoryFactoryBean.class.getName();
            for (String name : registry.getBeanDefinitionNames()) {
                var bd = registry.getBeanDefinition(name);
                if (target.equals(bd.getBeanClassName())) {
                    bd.setBeanClassName(replacement);
                }
            }
        };
    }

    // ─────────── Micrometer 可观测性指标（可选） ───────────

    @Bean
    @ConditionalOnMissingBean(JpaPlusMetrics.class)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    JpaPlusMetrics jpaPlusMetrics(
            MeterRegistry registry,
            @Value("${jpa-plus.metrics.prefix:jpa.plus}") String prefix) {
        return new MicrometerJpaPlusMetrics(registry, prefix);
    }
}

