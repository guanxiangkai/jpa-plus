package com.actomize.jpa.plus.query.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * SQL 编译器注册表
 *
 * <p>根据 JDBC URL 前缀或方言名称自动解析对应方言的 {@link SqlCompiler}。
 * 若 URL 无法匹配任何已知方言，则回退到 {@link MySqlCompiler}（最广泛兼容的默认值）。</p>
 *
 * <h3>自动探测逻辑（优先级由高到低）</h3>
 * <ol>
 *   <li>{@code jpa-plus.dialect} 配置项手动指定</li>
 *   <li>从 {@code DataSource} JDBC URL 前缀自动识别</li>
 *   <li>回退 {@link MySqlCompiler}（默认兜底）</li>
 * </ol>
 *
 * <h3>支持的方言</h3>
 * <table border="1">
 *   <tr><th>方言</th><th>JDBC URL 前缀</th><th>分页语法</th></tr>
 *   <tr><td>MySQL</td><td>{@code jdbc:mysql:}</td><td>{@code LIMIT offset, rows}</td></tr>
 *   <tr><td>MariaDB</td><td>{@code jdbc:mariadb:}</td><td>{@code LIMIT offset, rows}</td></tr>
 *   <tr><td>PostgreSQL</td><td>{@code jdbc:postgresql:}</td><td>{@code LIMIT rows OFFSET offset}</td></tr>
 *   <tr><td>Oracle</td><td>{@code jdbc:oracle:}</td><td>{@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}</td></tr>
 *   <tr><td>SQL Server</td><td>{@code jdbc:sqlserver:}</td><td>{@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}</td></tr>
 *   <tr><td>SQLite</td><td>{@code jdbc:sqlite:}</td><td>{@code LIMIT rows OFFSET offset}</td></tr>
 *   <tr><td>H2</td><td>{@code jdbc:h2:}</td><td>{@code LIMIT rows OFFSET offset}</td></tr>
 *   <tr><td>ClickHouse</td><td>{@code jdbc:clickhouse:}</td><td>{@code LIMIT rows OFFSET offset}</td></tr>
 * </table>
 *
 * @author guanxiangkai
 * @since 2026年04月11日
 */
public final class SqlCompilerRegistry {

    private static final Logger log = LoggerFactory.getLogger(SqlCompilerRegistry.class);

    /**
     * JDBC URL 前缀（小写）→ 编译器工厂，顺序敏感（mariadb 在 mysql 前防止前缀误匹配）
     */
    private static final Map<String, Supplier<AbstractSqlCompiler>> URL_REGISTRY = new LinkedHashMap<>();

    static {
        URL_REGISTRY.put("jdbc:mariadb:", MariaDbSqlCompiler::new);
        URL_REGISTRY.put("jdbc:mysql:", MySqlCompiler::new);
        URL_REGISTRY.put("jdbc:postgresql:", PgSqlCompiler::new);
        URL_REGISTRY.put("jdbc:oracle:", OracleSqlCompiler::new);
        URL_REGISTRY.put("jdbc:sqlserver:", SqlServerSqlCompiler::new);
        URL_REGISTRY.put("jdbc:sqlite:", SqliteSqlCompiler::new);
        URL_REGISTRY.put("jdbc:h2:", H2SqlCompiler::new);
        URL_REGISTRY.put("jdbc:clickhouse:", ClickHouseSqlCompiler::new);
        URL_REGISTRY.put("jdbc:dm:", OracleSqlCompiler::new);       // 达梦兼容 Oracle 语法
        URL_REGISTRY.put("jdbc:kingbase8:", PgSqlCompiler::new);           // 人大金仓兼容 PG 语法
    }

    private SqlCompilerRegistry() {
    }

    /**
     * 根据 JDBC URL 自动匹配编译器，未匹配则回退 {@link MySqlCompiler}
     *
     * @param jdbcUrl JDBC URL（如 {@code jdbc:postgresql://localhost/mydb}）
     * @return 对应方言的编译器实例
     */
    public static AbstractSqlCompiler resolve(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return new MySqlCompiler();
        }
        String lower = jdbcUrl.toLowerCase();
        for (Map.Entry<String, Supplier<AbstractSqlCompiler>> entry : URL_REGISTRY.entrySet()) {
            if (lower.startsWith(entry.getKey())) {
                return entry.getValue().get();
            }
        }
        log.warn("[jpa-plus] SqlCompilerRegistry: no dialect matched JDBC URL '{}', falling back to MySQL", jdbcUrl);
        return new MySqlCompiler();
    }

    /**
     * 根据方言名称解析编译器，用于 {@code jpa-plus.dialect} 配置项手动指定
     *
     * @param dialect 方言名称（不区分大小写），如 {@code mysql}、{@code oracle}、{@code postgresql}
     * @return 对应方言的编译器实例，未知方言回退 {@link MySqlCompiler}
     */
    public static AbstractSqlCompiler resolveByDialect(String dialect) {
        if (dialect == null || dialect.isBlank()) {
            return new MySqlCompiler();
        }
        return switch (dialect.trim().toLowerCase()) {
            case "mysql" -> new MySqlCompiler();
            case "mariadb" -> new MariaDbSqlCompiler();
            case "postgresql", "postgres", "pg" -> new PgSqlCompiler();
            case "oracle" -> new OracleSqlCompiler();
            case "sqlserver", "mssql",
                 "sql_server", "sql-server" -> new SqlServerSqlCompiler();
            case "sqlite" -> new SqliteSqlCompiler();
            case "h2" -> new H2SqlCompiler();
            case "clickhouse" -> new ClickHouseSqlCompiler();
            default -> {
                log.warn("[jpa-plus] SqlCompilerRegistry: unknown dialect '{}', falling back to MySQL",
                        dialect.trim());
                yield new MySqlCompiler();
            }
        };
    }
}

