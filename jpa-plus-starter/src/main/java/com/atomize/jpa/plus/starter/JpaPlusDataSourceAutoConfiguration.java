package com.atomize.jpa.plus.starter;

import com.atomize.jpa.plus.datasource.aop.DSAspect;
import com.atomize.jpa.plus.datasource.creator.DataSourceCreator;
import com.atomize.jpa.plus.datasource.health.DynamicDataSourceHealthIndicator;
import com.atomize.jpa.plus.datasource.listener.DataSourceRefreshListener;
import com.atomize.jpa.plus.datasource.provider.DataSourceProvider;
import com.atomize.jpa.plus.datasource.refresh.DataSourceRefresher;
import com.atomize.jpa.plus.datasource.refresh.ScheduledDataSourceRefresher;
import com.atomize.jpa.plus.datasource.registry.DynamicDataSourceRegistry;
import com.atomize.jpa.plus.datasource.routing.DynamicRoutingDataSource;
import com.atomize.jpa.plus.datasource.spi.DataSourcePostProcessor;
import com.atomize.jpa.plus.starter.condition.DynamicDataSourceConfiguredCondition;
import com.atomize.jpa.plus.starter.creator.HikariDataSourceCreator;
import com.atomize.jpa.plus.starter.provider.EnvironmentDataSourceProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.List;

/**
 * JPA-Plus 动态数据源自动装配
 *
 * <p>当满足以下条件时自动激活：
 * <ul>
 *   <li>{@link DynamicRoutingDataSource} 类在 classpath 上（jpa-plus-datasource 模块已引入）</li>
 *   <li>{@code spring.datasource.dynamic.datasource} 中至少配置了一个数据源（Map 类型，使用自定义 Condition 检测）</li>
 * </ul>
 * </p>
 *
 * <h3>配置示例</h3>
 * <pre>{@code
 * spring:
 *   datasource:
 *     dynamic:
 *       primary: master
 *       strict: true
 *       hikari:
 *         maximum-pool-size: 10
 *         minimum-idle: 5
 *         connection-timeout: 30000
 *         idle-timeout: 600000
 *         max-lifetime: 1800000
 *       datasource:
 *         master:
 *           url: jdbc:mysql://localhost:3306/master_db
 *           username: root
 *           password: root
 *         slave_1:
 *           url: jdbc:mysql://localhost:3306/slave1_db
 *           username: root
 *           password: root
 *         pg_db:
 *           url: jdbc:postgresql://localhost:5432/pg_db
 *           username: postgres
 *           password: postgres
 *       refresh:
 *         enabled: true
 *         interval: 30s
 *         reset-pool: true
 *       jdbc:
 *         enabled: false
 *         table-name: jpa_plus_datasource
 * }</pre>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>从 {@code spring.datasource.dynamic.datasource.*} 读取所有数据源配置</li>
 *   <li>根据 JDBC URL 自动检测 {@link com.atomize.jpa.plus.datasource.enums.DatabaseType}</li>
 *   <li>全局 HikariCP 配置作为默认值，各数据源可单独覆盖</li>
 *   <li>{@link DynamicDataSourceRegistry} 创建并注册所有数据源，经 {@link DataSourcePostProcessor} 链处理</li>
 *   <li>{@link DynamicRoutingDataSource} 作为 {@code @Primary DataSource}</li>
 *   <li>事务由 Spring Boot 自动配置的 {@code JpaTransactionManager} 管理（基于 @Primary 路由数据源）</li>
 *   <li>定时刷新 / 事件驱动刷新自动检测配置变更</li>
 * </ol>
 *
 * <h3>事务模型</h3>
 * <p>本框架采用 <b>JPA 兼容</b> 的事务模型：</p>
 * <ol>
 *   <li>{@code @DS("slave")} 切面（优先级最高）先于 {@code @Transactional} 设置路由 key</li>
 *   <li>{@code JpaTransactionManager} 开启事务时从 {@link DynamicRoutingDataSource} 获取连接 → 路由到正确的底层数据源</li>
 *   <li>同一事务内的所有操作共享同一连接，保证单数据源事务一致性</li>
 *   <li>跨数据源操作需使用 {@code @Transactional(propagation = REQUIRES_NEW)} 或集成 Seata 分布式事务</li>
 * </ol>
 *
 * <h3>扩展点</h3>
 * <p>用户注册任意数量的 {@link DataSourcePostProcessor} Bean 即可在数据源创建后进行包装，
 * 典型场景如 Seata 分布式事务代理 —— 按数据源名称、类型等条件灵活决定是否代理。</p>
 *
 * @author guanxiangkai
 * @see DynamicDataSourceProperties
 * @see DynamicDataSourceConfiguredCondition
 * @since 2026年03月25日 星期三
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnClass(DynamicRoutingDataSource.class)
@Conditional(DynamicDataSourceConfiguredCondition.class)
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
public class JpaPlusDataSourceAutoConfiguration {

    // ─────────── 数据源创建器 ───────────

    @Bean
    @ConditionalOnMissingBean
    public DataSourceCreator dataSourceCreator() {
        return new HikariDataSourceCreator();
    }

    // ─────────── YAML 数据源提供者 ───────────

    @Bean
    @ConditionalOnMissingBean(name = "environmentDataSourceProvider")
    public EnvironmentDataSourceProvider environmentDataSourceProvider(Environment environment) {
        return new EnvironmentDataSourceProvider(environment);
    }

    // ─────────── 数据源提供者 ───────────

    @Bean
    @ConditionalOnMissingBean(DataSourceProvider.class)
    public DataSourceProvider dataSourceProvider(EnvironmentDataSourceProvider envProvider) {
        return envProvider;
    }

    // ─────────── 路由数据源 ───────────

    @Bean
    @ConditionalOnMissingBean
    public DynamicRoutingDataSource dynamicRoutingDataSource(DynamicDataSourceProperties properties) {
        return new DynamicRoutingDataSource(properties.getPrimary(), properties.isStrict());
    }

    // ─────────── 数据源注册中心 ───────────
    //
    // 事务管理由 Spring Boot 自动配置的 JpaTransactionManager 负责：
    //   1. @DS 切面（@Order(HIGHEST_PRECEDENCE)）先于 @Transactional 设置路由 key
    //   2. JpaTransactionManager 从 @Primary DynamicRoutingDataSource 获取连接 → 路由到正确的底层数据源
    //   3. 单数据源事务一致性由 JPA 原生事务保证
    //   4. 跨数据源分布式事务通过 DataSourcePostProcessor 集成 Seata 实现

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceRegistry dynamicDataSourceRegistry(
            DynamicRoutingDataSource routingDataSource,
            DataSourceCreator creator,
            DataSourceProvider provider,
            ObjectProvider<DataSourcePostProcessor> postProcessors) {
        List<DataSourcePostProcessor> processorList = postProcessors.orderedStream().toList();
        var registry = new DynamicDataSourceRegistry(
                routingDataSource, creator, provider, processorList, null);
        registry.init();
        return registry;
    }

    // ─────────── @Primary 数据源（替代 Spring Boot 默认） ───────────

    @Bean
    @Primary
    public DataSource dataSource(DynamicDataSourceRegistry registry,
                                 DynamicRoutingDataSource routingDataSource) {
        return routingDataSource;
    }

    // ─────────── @DS 切面 ───────────

    @Bean
    @ConditionalOnMissingBean
    public DSAspect dsAspect() {
        return new DSAspect();
    }

    // ─────────── 事件监听刷新 ───────────

    @Bean
    @ConditionalOnMissingBean
    public DataSourceRefreshListener dataSourceRefreshListener(DynamicDataSourceRegistry registry) {
        return new DataSourceRefreshListener(registry);
    }

    // ─────────── 编程式刷新门面 ───────────

    @Bean
    @ConditionalOnMissingBean
    public DataSourceRefresher dataSourceRefresher(DynamicDataSourceRegistry registry) {
        return new DataSourceRefresher(registry);
    }

    // ─────────── 定时刷新 ───────────

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.dynamic.refresh", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public ScheduledDataSourceRefresher scheduledDataSourceRefresher(
            DataSourceRefresher refresher,
            DynamicDataSourceProperties properties) {
        return new ScheduledDataSourceRefresher(refresher, properties.getRefresh().getInterval());
    }

    // ─────────── 健康检查 ───────────

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.dynamic.health", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    @ConditionalOnMissingBean
    public DynamicDataSourceHealthIndicator dynamicDataSourceHealthIndicator(
            DynamicDataSourceRegistry registry,
            DynamicDataSourceProperties properties) {
        return new DynamicDataSourceHealthIndicator(registry, properties.getHealth().isIncludeDetail());
    }
}

