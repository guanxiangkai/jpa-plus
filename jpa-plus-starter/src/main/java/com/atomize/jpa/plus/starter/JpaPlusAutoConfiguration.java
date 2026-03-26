package com.atomize.jpa.plus.starter;

import com.atomize.jpa.plus.core.executor.DefaultJpaPlusExecutor;
import com.atomize.jpa.plus.core.executor.JpaPlusExecutor;
import com.atomize.jpa.plus.core.field.FieldEngine;
import com.atomize.jpa.plus.core.field.FieldHandler;
import com.atomize.jpa.plus.core.interceptor.DataInterceptor;
import com.atomize.jpa.plus.core.interceptor.InterceptorChain;
import com.atomize.jpa.plus.desensitize.handler.DesensitizeFieldHandler;
import com.atomize.jpa.plus.dict.handler.DictFieldHandler;
import com.atomize.jpa.plus.dict.provider.JdbcDictProvider;
import com.atomize.jpa.plus.dict.spi.DictProvider;
import com.atomize.jpa.plus.encrypt.handler.EncryptFieldHandler;
import com.atomize.jpa.plus.encrypt.spi.EncryptKeyProvider;
import com.atomize.jpa.plus.logicdelete.handler.LogicDeleteFieldHandler;
import com.atomize.jpa.plus.orderby.interceptor.AutoOrderByInterceptor;
import com.atomize.jpa.plus.query.compiler.DebugSqlCompiler;
import com.atomize.jpa.plus.query.compiler.MySqlCompiler;
import com.atomize.jpa.plus.query.compiler.SqlCompiler;
import com.atomize.jpa.plus.query.context.FlushMode;
import com.atomize.jpa.plus.query.context.FlushStrategy;
import com.atomize.jpa.plus.query.context.QueryContext;
import com.atomize.jpa.plus.query.executor.DefaultQueryExecutor;
import com.atomize.jpa.plus.query.executor.QueryExecutor;
import com.atomize.jpa.plus.query.pagination.CountStrategy;
import com.atomize.jpa.plus.query.pagination.PaginationOptimizer;
import com.atomize.jpa.plus.query.resolver.DefaultJpaRelationResolver;
import com.atomize.jpa.plus.query.resolver.JpaRelationResolver;
import com.atomize.jpa.plus.sensitive.handler.SensitiveWordHandler;
import com.atomize.jpa.plus.sensitive.spi.SensitiveWordProvider;
import com.atomize.jpa.plus.starter.repository.JpaPlusRepositoryFactoryBean;
import com.atomize.jpa.plus.version.handler.VersionFieldHandler;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA-Plus Spring Boot 自动装配配置
 *
 * <p>自动注册以下核心 Bean：
 * <ul>
 *   <li>{@link SqlCompiler} —— SQL 编译器（默认 MySQL 方言）</li>
 *   <li>{@link QueryExecutor} —— 查询执行器</li>
 *   <li>{@link FieldEngine} —— 字段引擎</li>
 *   <li>{@link InterceptorChain} —— 拦截器链</li>
 *   <li>{@link JpaPlusExecutor} —— 统一执行器</li>
 *   <li>各治理模块的 FieldHandler</li>
 * </ul>
 * </p>
 *
 * <h3>配置项（前缀 {@code jpa-plus.*}）</h3>
 * <pre>{@code
 * jpa-plus:
 *   flush-mode: AUTO
 *   debug:
 *     enabled: false
 *     print-params: true
 *   pagination:
 *     count-strategy: SIMPLE
 *     force-subquery-for-join: true
 *   encrypt:
 *     key: "${ENCRYPT_KEY:JpaPlusEncKey128}"
 *   dict:
 *     jdbc:
 *       enabled: false
 *       table-name: jpa_plus_dict
 *       auto-init-schema: true
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@AutoConfiguration
@EnableJpaRepositories(
        repositoryFactoryBeanClass = JpaPlusRepositoryFactoryBean.class
)
public class JpaPlusAutoConfiguration {

    // ─────────── SQL 编译器 ───────────

    @Bean
    @ConditionalOnMissingBean
    public SqlCompiler sqlCompiler(
            @Value("${jpa-plus.debug.enabled:false}") boolean debugEnabled,
            @Value("${jpa-plus.debug.print-params:true}") boolean printParams) {
        SqlCompiler compiler = new MySqlCompiler();
        if (debugEnabled) {
            compiler = new DebugSqlCompiler(compiler, printParams);
        }
        return compiler;
    }

    // ─────────── 分页优化器 ───────────

    @Bean
    @ConditionalOnMissingBean
    public PaginationOptimizer paginationOptimizer(
            @Value("${jpa-plus.pagination.count-strategy:SIMPLE}") CountStrategy countStrategy) {
        return new PaginationOptimizer(countStrategy);
    }

    // ─────────── Flush 策略 ───────────

    @Bean
    @ConditionalOnMissingBean
    public FlushStrategy flushStrategy(
            @Value("${jpa-plus.flush-mode:AUTO}") FlushMode flushMode) {
        return new FlushStrategy(flushMode);
    }

    // ─────────── JPA 关联解析器 ───────────

    @Bean
    @ConditionalOnMissingBean
    public JpaRelationResolver jpaRelationResolver() {
        return new DefaultJpaRelationResolver();
    }

    // ─────────── 字段处理器（各治理模块） ───────────

    @Bean
    @ConditionalOnMissingBean
    public EncryptKeyProvider encryptKeyProvider(
            @Value("${jpa-plus.encrypt.key:JpaPlusEncKey128}") String encryptKey) {
        return () -> encryptKey;
    }

    @Bean
    @ConditionalOnMissingBean
    public EncryptFieldHandler encryptFieldHandler(EncryptKeyProvider keyProvider) {
        return new EncryptFieldHandler(keyProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public DesensitizeFieldHandler desensitizeFieldHandler() {
        return new DesensitizeFieldHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public VersionFieldHandler versionFieldHandler() {
        return new VersionFieldHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LogicDeleteFieldHandler logicDeleteFieldHandler() {
        return new LogicDeleteFieldHandler();
    }

    // ─────────── 拦截器（各治理模块） ───────────

    @Bean
    @ConditionalOnMissingBean
    public AutoOrderByInterceptor autoOrderByInterceptor() {
        return new AutoOrderByInterceptor();
    }

    // ─────────── 字典数据提供者（内置 JDBC 实现） ───────────

    @Bean
    @ConditionalOnMissingBean(DictProvider.class)
    @ConditionalOnProperty(prefix = "jpa-plus.dict.jdbc", name = "enabled", havingValue = "true")
    public JdbcDictProvider jdbcDictProvider(
            DataSource dataSource,
            @Value("${jpa-plus.dict.jdbc.table-name:jpa_plus_dict}") String tableName,
            @Value("${jpa-plus.dict.jdbc.auto-init-schema:true}") boolean autoInitSchema) {
        return new JdbcDictProvider(dataSource, tableName, autoInitSchema);
    }

    @Bean
    @ConditionalOnBean(DictProvider.class)
    @ConditionalOnMissingBean
    public DictFieldHandler dictFieldHandler(DictProvider dictProvider) {
        return new DictFieldHandler(dictProvider);
    }

    @Bean
    @ConditionalOnBean(SensitiveWordProvider.class)
    @ConditionalOnMissingBean
    public SensitiveWordHandler sensitiveWordHandler(SensitiveWordProvider provider) {
        return new SensitiveWordHandler(provider);
    }

    // ─────────── 字段引擎 ───────────

    @Bean
    @ConditionalOnMissingBean
    public FieldEngine fieldEngine(List<FieldHandler> handlers) {
        return new FieldEngine(handlers);
    }

    // ─────────── 拦截器链 ───────────

    @Bean
    @ConditionalOnMissingBean
    public InterceptorChain interceptorChain(List<DataInterceptor> interceptors) {
        return new InterceptorChain(interceptors != null ? interceptors : new ArrayList<>());
    }

    // ─────────── 查询执行器 ───────────

    @Bean
    @ConditionalOnMissingBean
    public QueryExecutor queryExecutor(EntityManager entityManager,
                                       SqlCompiler sqlCompiler,
                                       PaginationOptimizer paginationOptimizer) {
        return new DefaultQueryExecutor(entityManager, sqlCompiler, paginationOptimizer);
    }

    // ─────────── 统一执行器 ───────────

    @Bean
    @ConditionalOnMissingBean
    public JpaPlusExecutor jpaPlusExecutor(InterceptorChain interceptorChain,
                                           FieldEngine fieldEngine,
                                           QueryExecutor queryExecutor,
                                           EntityManager entityManager,
                                           SqlCompiler sqlCompiler,
                                           FlushStrategy flushStrategy) {
        InterceptorChain.CoreExecution coreExecution = invocation -> switch (invocation.type()) {
            case QUERY -> {
                flushStrategy.flushIfNeeded(entityManager);
                var ctx = invocation.queryModel();
                if (ctx instanceof QueryContext queryContext) {
                    var result = sqlCompiler.compile(queryContext);
                    var query = entityManager.createNativeQuery(result.sql(), invocation.entityClass());
                    result.params().forEach(query::setParameter);
                    yield query.getResultList();
                }
                yield List.of();
            }
            case SAVE -> {
                if (invocation.entity() != null) {
                    yield entityManager.merge(invocation.entity());
                }
                yield null;
            }
            case DELETE -> {
                if (invocation.entity() != null) {
                    entityManager.remove(
                            entityManager.contains(invocation.entity())
                                    ? invocation.entity()
                                    : entityManager.merge(invocation.entity())
                    );
                }
                yield null;
            }
        };

        return new DefaultJpaPlusExecutor(interceptorChain, fieldEngine, coreExecution);
    }
}

