package com.actomize.jpa.plus.datasource.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 数据库类型枚举
 *
 * <p>定义常见的数据库类型，用于数据源定义中标识底层数据库。
 * 每种类型携带默认驱动类名、校验 SQL 和 JDBC URL 前缀，支持自动识别。</p>
 *
 * <h3>自动检测</h3>
 * <p>框架可根据 JDBC URL 或驱动类名自动识别数据库类型：</p>
 * <pre>{@code
 * DatabaseType type = DatabaseType.fromUrl("jdbc:mysql://localhost:3306/db")
 *     .orElseThrow();  // MYSQL
 *
 * DatabaseType type = DatabaseType.fromDriverClassName("org.postgresql.Driver")
 *     .orElseThrow();  // POSTGRESQL
 * }</pre>
 *
 * <h3>扩展方式</h3>
 * <p>如果内置枚举不满足需求，用户可通过实现 {@link DbType} 接口自定义数据库类型：</p>
 * <pre>{@code
 * public enum MyDbType implements DbType {
 *     TIDB("com.mysql.cj.jdbc.Driver", "SELECT 1"),
 *     OCEANBASE("com.alipay.oceanbase.jdbc.OceanBaseDriver", "SELECT 1");
 *
 *     private final String driver;
 *     private final String validationQuery;
 *
 *     MyDbType(String driver, String validationQuery) {
 *         this.driver = driver;
 *         this.validationQuery = validationQuery;
 *     }
 *
 *     @Override public String driverClassName() { return driver; }
 *     @Override public String validationQuery() { return validationQuery; }
 * }
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public enum DatabaseType implements DbType {

    MYSQL("com.mysql.cj.jdbc.Driver", "SELECT 1", "jdbc:mysql:"),
    POSTGRESQL("org.postgresql.Driver", "SELECT 1", "jdbc:postgresql:"),
    ORACLE("oracle.jdbc.OracleDriver", "SELECT 1 FROM DUAL", "jdbc:oracle:"),
    SQL_SERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "SELECT 1", "jdbc:sqlserver:"),
    MARIADB("org.mariadb.jdbc.Driver", "SELECT 1", "jdbc:mariadb:"),
    H2("org.h2.Driver", "SELECT 1", "jdbc:h2:"),
    SQLITE("org.sqlite.JDBC", "SELECT 1", "jdbc:sqlite:"),
    DM("dm.jdbc.driver.DmDriver", "SELECT 1", "jdbc:dm:"),
    KINGBASE("com.kingbase8.Driver", "SELECT 1", "jdbc:kingbase8:"),
    CLICKHOUSE("com.clickhouse.jdbc.ClickHouseDriver", "SELECT 1", "jdbc:clickhouse:");

    private final String driverClassName;
    private final String validationQuery;
    private final String urlPrefix;

    DatabaseType(String driverClassName, String validationQuery, String urlPrefix) {
        this.driverClassName = driverClassName;
        this.validationQuery = validationQuery;
        this.urlPrefix = urlPrefix;
    }

    @Override
    public String driverClassName() {
        return driverClassName;
    }

    @Override
    public String validationQuery() {
        return validationQuery;
    }

    /**
     * 根据 JDBC URL 自动检测数据库类型
     *
     * <p>遍历所有枚举值，匹配 URL 前缀（不区分大小写）。</p>
     *
     * @param url JDBC URL（如 {@code jdbc:mysql://localhost:3306/db}）
     * @return 匹配的数据库类型，未匹配返回 {@link Optional#empty()}
     */
    public static Optional<DatabaseType> fromUrl(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String lower = url.toLowerCase();
        for (DatabaseType type : values()) {
            if (lower.startsWith(type.urlPrefix)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * 根据驱动类名自动检测数据库类型
     *
     * @param driverClassName JDBC 驱动全限定类名
     * @return 匹配的数据库类型，未匹配返回 {@link Optional#empty()}
     */
    public static Optional<DatabaseType> fromDriverClassName(String driverClassName) {
        if (driverClassName == null || driverClassName.isBlank()) {
            return Optional.empty();
        }
        for (DatabaseType type : values()) {
            if (type.driverClassName.equals(driverClassName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * 根据 JDBC URL 自动检测数据库类型（不存在时抛出异常）
     *
     * @param url JDBC URL
     * @return 匹配的数据库类型
     * @throws IllegalArgumentException URL 无法匹配任何已知数据库类型
     */
    public static DatabaseType requireFromUrl(String url) {
        return fromUrl(url).orElseThrow(() ->
                new IllegalArgumentException("Cannot detect database type from URL: " + url +
                        ". Supported prefixes: " + Arrays.toString(
                        Arrays.stream(values()).map(DatabaseType::urlPrefix).toArray())));
    }

    /**
     * JDBC URL 前缀（如 {@code jdbc:mysql:}）
     */
    public String urlPrefix() {
        return urlPrefix;
    }
}
