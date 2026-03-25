package com.atomize.jpa.plus.datasource.provider;

import com.atomize.jpa.plus.datasource.model.DataSourceDefinition;

import java.util.List;

/**
 * 数据源配置提供者（SPI）
 *
 * <p>用户实现此接口，提供数据源配置列表。框架不关心数据来源 ——
 * 可以从 application.yml、数据库、Nacos、Apollo、Consul 等任意方式加载，
 * 由用户自行决定。</p>
 *
 * <h3>使用示例</h3>
 *
 * <p><b>1. 从配置文件加载：</b></p>
 * <pre>{@code
 * @Component
 * public class YamlDataSourceProvider implements DataSourceProvider {
 *
 *     @Value("${datasource.slave.url}") String slaveUrl;
 *     // ...
 *
 *     @Override
 *     public List<DataSourceDefinition> provide() {
 *         return List.of(
 *             new DataSourceDefinition("master", DatabaseType.MYSQL, masterUrl, ...),
 *             new DataSourceDefinition("slave", DatabaseType.MYSQL, slaveUrl, ...)
 *         );
 *     }
 * }
 * }</pre>
 *
 * <p><b>2. 从数据库加载（动态管理）：</b></p>
 * <pre>{@code
 * @Component
 * public class DbDataSourceProvider implements DataSourceProvider {
 *
 *     private final JdbcTemplate jdbc; // 使用主库的 JdbcTemplate
 *
 *     @Override
 *     public List<DataSourceDefinition> provide() {
 *         return jdbc.query("SELECT * FROM sys_datasource WHERE enabled = 1",
 *             (rs, i) -> new DataSourceDefinition(
 *                 rs.getString("name"),
 *                 DatabaseType.valueOf(rs.getString("db_type")),
 *                 rs.getString("url"),
 *                 rs.getString("username"),
 *                 rs.getString("password")
 *             ));
 *     }
 * }
 * }</pre>
 *
 * <p><b>设计模式：</b>策略接口模式（Strategy） —— 解耦数据源配置获取与数据源创建</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public interface DataSourceProvider {

    /**
     * 提供数据源配置列表
     *
     * <p>这是用户唯一需要实现的方法。返回的列表应包含所有需要注册的数据源
     * （含 master）。框架会对比当前注册表，自动完成新增、更新、移除。</p>
     *
     * <p>此方法在以下时机被调用：
     * <ul>
     *   <li>应用启动时 —— 初始化所有数据源</li>
     *   <li>配置变更时 —— 由 {@link com.atomize.jpa.plus.datasource.registry.DynamicDataSourceRegistry}
     *       调用 {@link #provide()} 获取最新配置并差异刷新</li>
     * </ul>
     * </p>
     *
     * @return 数据源定义列表（不可为 null）
     */
    List<DataSourceDefinition> provide();
}

