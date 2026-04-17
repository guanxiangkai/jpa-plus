package com.actomize.jpa.plus.starter;

import com.actomize.jpa.plus.core.executor.DefaultJpaPlusExecutor;
import com.actomize.jpa.plus.core.executor.JpaPlusExecutor;
import com.actomize.jpa.plus.core.field.FieldEngine;
import com.actomize.jpa.plus.core.interceptor.DataInterceptor;
import com.actomize.jpa.plus.core.interceptor.InterceptorChain;
import com.actomize.jpa.plus.core.interceptor.InterceptorChainContributor;
import com.actomize.jpa.plus.core.metrics.JpaPlusMetrics;
import com.actomize.jpa.plus.core.model.DeleteInvocation;
import com.actomize.jpa.plus.core.model.QueryInvocation;
import com.actomize.jpa.plus.core.model.SaveInvocation;
import com.actomize.jpa.plus.core.spi.JpaPlusLoader;
import com.actomize.jpa.plus.query.compiler.SqlCompiler;
import com.actomize.jpa.plus.query.context.FlushMode;
import com.actomize.jpa.plus.query.context.FlushStrategy;
import com.actomize.jpa.plus.query.context.QueryContext;
import com.actomize.jpa.plus.query.executor.QueryExecutor;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Plus 核心执行器自动装配
 *
 * <p>负责注册：{@link InterceptorChain}、{@link FlushStrategy}、{@link JpaPlusExecutor}。</p>
 * <p>应用关闭时自动清理 SPI 缓存，防止 ClassLoader 泄漏。</p>
 *
 * @author guanxiangkai
 * @since 2026年04月12日
 */
@AutoConfiguration
@Slf4j
public class JpaPlusCoreAutoConfiguration {

    // ─────────── SPI 缓存生命周期管理 ───────────

    @PreDestroy
    void cleanupSpiCache() {
        JpaPlusLoader.invalidateAll();
    }

    // ─────────── Flush 策略 ───────────

    @Bean
    @ConditionalOnMissingBean
    FlushStrategy flushStrategy(
            @Value("${jpa-plus.flush-mode:AUTO}") FlushMode flushMode) {
        return new FlushStrategy(flushMode);
    }

    // ─────────── 拦截器链（含 SPI 贡献者） ───────────

    @Bean
    @ConditionalOnMissingBean
    InterceptorChain interceptorChain(List<DataInterceptor> interceptors) {
        List<DataInterceptor> all = new ArrayList<>(interceptors != null ? interceptors : List.of());

        // 加载 SPI InterceptorChainContributor 贡献的拦截器
        List<InterceptorChainContributor> contributors =
                JpaPlusLoader.loadAll(InterceptorChainContributor.class);
        for (InterceptorChainContributor contributor : contributors) {
            List<DataInterceptor> contributed = contributor.contribute();
            if (contributed != null) {
                all.addAll(contributed);
            }
        }

        return new InterceptorChain(all);
    }

    // ─────────── 统一执行器（含 Micrometer 指标） ───────────

    @Bean
    @ConditionalOnMissingBean
    JpaPlusExecutor jpaPlusExecutor(InterceptorChain interceptorChain,
                                    FieldEngine fieldEngine,
                                    QueryExecutor queryExecutor,
                                    EntityManager entityManager,
                                    SqlCompiler sqlCompiler,
                                    FlushStrategy flushStrategy,
                                    ObjectProvider<JpaPlusMetrics> metricsProvider,
                                    @Value("${jpa-plus.debug.slow-sql.enabled:false}") boolean slowSqlEnabled,
                                    @Value("${jpa-plus.debug.slow-sql.threshold:1000}") long slowSqlThresholdMs) {
        final long slowThreshold = slowSqlEnabled ? slowSqlThresholdMs : 0L;

        InterceptorChain.CoreExecution coreExecution = invocation -> switch (invocation) {
            case QueryInvocation qi -> {
                flushStrategy.flushIfNeeded(entityManager);
                if (qi.queryContext() instanceof QueryContext queryContext) {
                    var result = sqlCompiler.compile(queryContext);
                    var query = entityManager.createNativeQuery(result.sql(), qi.entityClass());
                    result.params().forEach(query::setParameter);
                    long start = System.currentTimeMillis();
                    @SuppressWarnings("unchecked")
                    List<?> rows = query.getResultList();
                    long elapsed = System.currentTimeMillis() - start;
                    if (slowThreshold > 0 && elapsed >= slowThreshold) {
                        log.warn("[jpa-plus] Slow SQL detected ({}ms ≥ {}ms threshold): {}",
                                elapsed, slowThreshold, result.sql());
                    }
                    yield rows;
                }
                log.warn("[jpa-plus] Unknown queryContext type for QueryInvocation: {}, returning empty list",
                        qi.queryContext() != null ? qi.queryContext().getClass().getName() : "null");
                yield List.of();
            }
            case SaveInvocation si -> {
                if (si.entity() != null) {
                    yield entityManager.merge(si.entity());
                }
                yield null;
            }
            case DeleteInvocation di -> {
                if (di.entity() != null) {
                    entityManager.remove(
                            entityManager.contains(di.entity())
                                    ? di.entity()
                                    : entityManager.merge(di.entity())
                    );
                }
                yield null;
            }
        };

        JpaPlusMetrics metrics = metricsProvider.getIfAvailable(() -> JpaPlusMetrics.NOOP);
        return new DefaultJpaPlusExecutor(interceptorChain, fieldEngine, coreExecution, metrics);
    }
}

