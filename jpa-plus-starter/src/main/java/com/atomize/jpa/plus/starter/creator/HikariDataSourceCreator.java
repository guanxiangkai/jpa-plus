package com.atomize.jpa.plus.starter.creator;

import com.atomize.jpa.plus.datasource.creator.DataSourceCreator;
import com.atomize.jpa.plus.datasource.model.DataSourceDefinition;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

/**
 * 基于 HikariCP 的数据源创建器
 *
 * <p>根据 {@link DataSourceDefinition} 创建 {@link HikariDataSource} 实例。
 * HikariCP 是 Spring Boot 默认的连接池实现，具备极高性能和可靠性。</p>
 *
 * <h3>连接池参数</h3>
 * <ul>
 *   <li>{@code minimumIdle} —— 最小空闲连接数（来自 {@link DataSourceDefinition#minIdle()}）</li>
 *   <li>{@code maximumPoolSize} —— 最大连接池大小（来自 {@link DataSourceDefinition#maxPoolSize()}）</li>
 *   <li>{@code connectionTimeout} —— 连接超时毫秒（来自 {@link DataSourceDefinition#connectionTimeout()}）</li>
 *   <li>{@code connectionTestQuery} —— 连接校验 SQL（来自 {@link DataSourceDefinition#validationQuery()}）</li>
 *   <li>{@code poolName} —— 连接池名称（格式：{@code jpa-plus-{name}}）</li>
 * </ul>
 *
 * <p>用户可通过实现 {@link DataSourceCreator} 接口替换为 Druid / DBCP2 等其他连接池。</p>
 *
 * <p><b>设计模式：</b>工厂模式（Factory） —— 根据定义创建数据源实例</p>
 *
 * @author guanxiangkai
 * @see DataSourceCreator
 * @see DataSourceDefinition
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class HikariDataSourceCreator implements DataSourceCreator {

    @Override
    public DataSource createDataSource(DataSourceDefinition definition) {
        var config = new HikariConfig();
        config.setJdbcUrl(definition.url());
        config.setUsername(definition.username());
        config.setPassword(definition.password());
        config.setDriverClassName(definition.driverClassName());
        config.setMinimumIdle(definition.minIdle());
        config.setMaximumPoolSize(definition.maxPoolSize());
        config.setConnectionTimeout(definition.connectionTimeout());
        config.setConnectionTestQuery(definition.validationQuery());
        config.setPoolName("jpa-plus-" + definition.name());

        log.debug("Creating HikariDataSource '{}' → {}", definition.name(), definition.url());
        return new HikariDataSource(config);
    }
}

