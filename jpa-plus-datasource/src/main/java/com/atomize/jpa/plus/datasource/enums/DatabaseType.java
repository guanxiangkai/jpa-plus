package com.atomize.jpa.plus.datasource.enums;

/**
 * 数据库类型枚举
 *
 * <p>定义常见的数据库类型，用于数据源定义中标识底层数据库。
 * 每种类型携带默认驱动类名和校验 SQL。</p>
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

    MYSQL("com.mysql.cj.jdbc.Driver", "SELECT 1"),
    POSTGRESQL("org.postgresql.Driver", "SELECT 1"),
    ORACLE("oracle.jdbc.OracleDriver", "SELECT 1 FROM DUAL"),
    SQL_SERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "SELECT 1"),
    MARIADB("org.mariadb.jdbc.Driver", "SELECT 1"),
    H2("org.h2.Driver", "SELECT 1"),
    SQLITE("org.sqlite.JDBC", "SELECT 1"),
    DM("dm.jdbc.driver.DmDriver", "SELECT 1"),
    KINGBASE("com.kingbase8.Driver", "SELECT 1"),
    CLICKHOUSE("com.clickhouse.jdbc.ClickHouseDriver", "SELECT 1");

    private final String driverClassName;
    private final String validationQuery;

    DatabaseType(String driverClassName, String validationQuery) {
        this.driverClassName = driverClassName;
        this.validationQuery = validationQuery;
    }

    @Override
    public String driverClassName() {
        return driverClassName;
    }

    @Override
    public String validationQuery() {
        return validationQuery;
    }
}

