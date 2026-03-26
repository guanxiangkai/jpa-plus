package com.atomize.jpa.plus.starter;

import com.atomize.jpa.plus.datasource.aop.DSAspect;
import com.atomize.jpa.plus.datasource.creator.DataSourceCreator;
import com.atomize.jpa.plus.datasource.listener.DataSourceRefreshListener;
import com.atomize.jpa.plus.datasource.provider.DataSourceProvider;
import com.atomize.jpa.plus.datasource.provider.JdbcDataSourceProvider;
import com.atomize.jpa.plus.datasource.refresh.DataSourceRefresher;
import com.atomize.jpa.plus.datasource.refresh.ScheduledDataSourceRefresher;
import com.atomize.jpa.plus.datasource.registry.DynamicDataSourceRegistry;
import com.atomize.jpa.plus.datasource.routing.DynamicRoutingDataSource;
import com.atomize.jpa.plus.starter.creator.HikariDataSourceCreator;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * JPA-Plus 动态数据源自动装配
 *
 * <p>当满足以下条件时自动激活：
 * <ul>
 *   <li>{@link DynamicRoutingDataSource} 类在 classpath 上（jpa-plus-datasource 模块已引入）</li>
 *   <li>{@code jpa-plus.datasource.dynamic.enabled=true}（显式开启）</li>
 * </ul>
 * </p>
 *
 * <h3>自动注册的 Bean</h3>
 * <table>
 *   <tr><th>Bean</th><th>说明</th><th>覆盖方式</th></tr>
 *   <tr><td>{@link DataSourceCreator}</td><td>HikariCP 数据源创建器</td><td>用户自定义 Bean</td></tr>
 *   <tr><td>{@link DataSourceProvider}</td><td>JDBC 配置表读取器</td><td>用户自定义 Bean</td></tr>
 *   <tr><td>{@link DynamicRoutingDataSource}</td><td>路由数据源</td><td>—</td></tr>
 *   <tr><td>{@link DynamicDataSourceRegistry}</td><td>数据源注册中心</td><td>—</td></tr>
 *   <tr><td>{@link DataSourceRefreshListener}</td><td>事件监听刷新器</td><td>用户自定义 Bean</td></tr>
 *   <tr><td>{@link DSAspect}</td><td>@DS 注解切面</td><td>用户自定义 Bean</td></tr>
 *   <tr><td>{@link DataSourceRefresher}</td><td>编程式刷新门面</td><td>用户自定义 Bean</td></tr>
 *   <tr><td>{@link ScheduledDataSourceRefresher}</td><td>定时刷新器（需开启）</td><td>—</td></tr>
 * </table>
 *
 * <h3>配置示例</h3>
 * <pre>{@code
 * # 标准 Spring Boot 主数据源配置（作为 master）
 * spring:
 *   datasource:
 *     url: jdbc:mysql://localhost:3306/mydb
 *     username: root
 *     password: password
 *
 * # JPA-Plus 动态数据源配置
 * jpa-plus:
 *   datasource:
 *     dynamic:
 *       enabled: true
 *       table-name: jpa_plus_datasource
 *       auto-init-schema: true
 *       schedule:
 *         enabled: false
 *         interval: 30s
 * }</pre>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>从 {@code spring.datasource.*} 创建主（master）HikariDataSource</li>
 *   <li>{@link JdbcDataSourceProvider} 使用主数据源连接配置表，读取额外数据源定义</li>
 *   <li>{@link DynamicDataSourceRegistry} 注册 master + 额外数据源到路由表</li>
 *   <li>{@link DynamicRoutingDataSource} 作为 {@code @Primary DataSource}，替代 Spring Boot 默认数据源</li>
 *   <li>JPA / Hibernate 透明使用路由数据源</li>
 * </ol>
 *
 * @author guanxiangkai
 * @see JpaPlusAutoConfiguration
 * @since 2026年03月25日 星期三
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnClass(DynamicRoutingDataSource.class)
@ConditionalOnProperty(prefix = "jpa-plus.datasource.dynamic", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({JpaPlusProperties.class, DataSourceProperties.class})
public class JpaPlusDataSourceAutoConfiguration {

    // ─────────── 主数据源（从 spring.datasource.* 创建） ───────────

    /**
     * 使用 Spring Boot 的 {@link DataSourceProperties} 创建主数据源（HikariCP）
     *
     * <p>此 Bean 作为 master 数据源注册到路由表中，同时供 {@link JdbcDataSourceProvider}
     * 查询配置表使用。{@code @ConfigurationProperties(prefix = "spring.datasource.hikari")}
     * 确保 HikariCP 特有属性（如 {@code maximum-pool-size}）也被正确绑定。</p>
     */
    @Bean("jpaPlusMasterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSource masterDataSource(DataSourceProperties properties) {
        HikariDataSource ds = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("jpa-plus-master");
        return ds;
    }

    // ─────────── 数据源创建器 ───────────

    @Bean
    @ConditionalOnMissingBean
    public DataSourceCreator dataSourceCreator() {
        return new HikariDataSourceCreator();
    }

    // ─────────── 数据源配置提供者 ───────────

    @Bean
    @ConditionalOnMissingBean(DataSourceProvider.class)
    public JdbcDataSourceProvider jdbcDataSourceProvider(
            @Qualifier("jpaPlusMasterDataSource") DataSource masterDataSource,
            JpaPlusProperties properties) {
        var config = properties.getDatasource().getDynamic();
        return new JdbcDataSourceProvider(masterDataSource, config.getTableName(), config.isAutoInitSchema());
    }

    // ─────────── 路由数据源 ───────────

    @Bean
    @ConditionalOnMissingBean
    public DynamicRoutingDataSource dynamicRoutingDataSource() {
        return new DynamicRoutingDataSource();
    }

    // ─────────── 数据源注册中心 ───────────

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceRegistry dynamicDataSourceRegistry(
            DynamicRoutingDataSource routingDataSource,
            DataSourceCreator creator,
            DataSourceProvider provider,
            @Qualifier("jpaPlusMasterDataSource") DataSource masterDataSource) {
        var registry = new DynamicDataSourceRegistry(routingDataSource, creator, provider);
        registry.setPrimaryDataSource(masterDataSource);
        registry.init();
        return registry;
    }

    // ─────────── @Primary 数据源（替代 Spring Boot 默认） ───────────

    /**
     * 将路由数据源暴露为 {@code @Primary DataSource}
     *
     * <p>注入 {@link DynamicDataSourceRegistry} 参数确保注册中心已完成初始化，
     * 路由数据源的目标映射已就绪。</p>
     */
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
    @ConditionalOnProperty(prefix = "jpa-plus.datasource.dynamic.schedule", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public ScheduledDataSourceRefresher scheduledDataSourceRefresher(
            DataSourceRefresher refresher,
            JpaPlusProperties properties) {
        var interval = properties.getDatasource().getDynamic().getSchedule().getInterval();
        return new ScheduledDataSourceRefresher(refresher, interval);
    }
}

