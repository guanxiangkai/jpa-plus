package com.actomize.jpa.plus.datasource.creator;

import com.actomize.jpa.plus.datasource.model.DataSourceDefinition;

import javax.sql.DataSource;

/**
 * 数据源创建器接口（SPI）
 *
 * <p>解耦连接池实现（HikariCP / Druid / DBCP2 等），
 * 用户可根据项目需要提供自定义创建器。</p>
 *
 * <p>框架在 Starter 中提供基于 HikariCP 的默认实现。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@FunctionalInterface
public interface DataSourceCreator {

    /**
     * 根据定义创建数据源
     *
     * @param definition 数据源配置
     * @return 已初始化的数据源实例
     */
    DataSource createDataSource(DataSourceDefinition definition);
}

