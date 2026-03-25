package com.atomize.jpa.plus.datasource.model;

import com.atomize.jpa.plus.datasource.enums.DatabaseType;
import com.atomize.jpa.plus.datasource.enums.DbType;

/**
 * 数据源定义（不可变值对象）
 *
 * <p>描述一个数据源的连接属性，用于运行时动态创建或刷新数据源。
 * 驱动类名和校验 SQL 由 {@link DbType} 提供，无需手动填写。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * var def = new DataSourceDefinition(
 *     "slave", DatabaseType.MYSQL,
 *     "jdbc:mysql://slave-host:3306/db",
 *     "root", "password"
 * );
 * registry.add(def);
 * }</pre>
 *
 * @param name              数据源名称（路由 key）
 * @param dbType            数据库类型（内置 {@link DatabaseType} 或用户自定义枚举）
 * @param url               JDBC URL
 * @param username          数据库用户名
 * @param password          数据库密码
 * @param minIdle           最小空闲连接数
 * @param maxPoolSize       最大连接池大小
 * @param connectionTimeout 连接超时（毫秒）
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public record DataSourceDefinition(
        String name,
        DbType dbType,
        String url,
        String username,
        String password,
        int minIdle,
        int maxPoolSize,
        long connectionTimeout
) {

    /**
     * 简化构造（使用默认连接池参数：minIdle=5, maxPoolSize=20, timeout=30s）
     */
    public DataSourceDefinition(String name, DbType dbType, String url,
                                String username, String password) {
        this(name, dbType, url, username, password, 5, 20, 30_000L);
    }

    /**
     * 获取驱动类名（委托给 DbType）
     */
    public String driverClassName() {
        return dbType.driverClassName();
    }

    /**
     * 获取校验 SQL（委托给 DbType）
     */
    public String validationQuery() {
        return dbType.validationQuery();
    }
}
