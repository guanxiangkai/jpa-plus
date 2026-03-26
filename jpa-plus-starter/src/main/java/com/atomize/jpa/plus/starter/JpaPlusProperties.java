package com.atomize.jpa.plus.starter;

import com.atomize.jpa.plus.query.context.FlushMode;
import com.atomize.jpa.plus.query.pagination.CountStrategy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JPA-Plus 配置属性
 *
 * <p>通过 {@code application.yml} 中 {@code jpa-plus.*} 前缀配置：</p>
 * <pre>{@code
 * jpa-plus:
 *   flush-mode: AUTO
 *   debug:
 *     enabled: true
 *     print-params: true
 *   pagination:
 *     count-strategy: SIMPLE
 *   dict:
 *     jdbc:
 *       enabled: true
 *       table-name: jpa_plus_dict
 *       auto-init-schema: true
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
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jpa-plus")
public class JpaPlusProperties {

    /**
     * Flush 策略
     */
    private FlushMode flushMode = FlushMode.AUTO;

    /**
     * 调试配置
     */
    private Debug debug = new Debug();

    /**
     * 自动 Join 配置
     */
    private AutoJoin autoJoin = new AutoJoin();

    /**
     * 分页优化配置
     */
    private Pagination pagination = new Pagination();

    /**
     * 加密配置
     */
    private EncryptConfig encrypt = new EncryptConfig();

    /**
     * 字典配置
     */
    private DictConfig dict = new DictConfig();

    /**
     * 数据源配置
     */
    private DataSourceConfig datasource = new DataSourceConfig();

    @Getter
    @Setter
    public static class Debug {
        private boolean enabled = false;
        private boolean printParams = true;
    }

    @Getter
    @Setter
    public static class AutoJoin {
        /**
         * 是否自动为 @ManyToOne 添加 JOIN FETCH
         */
        private boolean manyToOneFetch = false;

        /**
         * Join 最大深度
         */
        private int maxDepth = 3;
    }

    @Getter
    @Setter
    public static class Pagination {
        /**
         * 默认 COUNT 策略
         */
        private CountStrategy countStrategy = CountStrategy.SIMPLE;

        /**
         * 当 LEFT JOIN 可能影响行数时是否强制使用子查询
         */
        private boolean forceSubqueryForJoin = true;
    }

    @Getter
    @Setter
    public static class EncryptConfig {
        /**
         * AES 加密密钥（长度须为 16/24/32 字节，生产环境必须配置）
         */
        private String key = "JpaPlusEncKey128";
    }

    @Getter
    @Setter
    public static class DictConfig {
        /**
         * JDBC 字典提供者配置
         */
        private JdbcDictConfig jdbc = new JdbcDictConfig();
    }

    @Getter
    @Setter
    public static class JdbcDictConfig {
        /**
         * 是否启用内置 JDBC 字典提供者（从数据库表读取字典数据）
         */
        private boolean enabled = false;

        /**
         * 字典数据表名
         */
        private String tableName = "jpa_plus_dict";

        /**
         * 是否自动建表（首次启动时，若表不存在则自动创建）
         */
        private boolean autoInitSchema = true;
    }

    @Getter
    @Setter
    public static class DataSourceConfig {
        /**
         * 动态数据源配置
         */
        private DynamicConfig dynamic = new DynamicConfig();
    }

    @Getter
    @Setter
    public static class DynamicConfig {
        /**
         * 是否开启动态数据源
         */
        private boolean enabled = false;

        /**
         * 数据源配置表名
         */
        private String tableName = "jpa_plus_datasource";

        /**
         * 是否自动建表（首次启动时，若表不存在则自动创建）
         */
        private boolean autoInitSchema = true;

        /**
         * 定时刷新配置
         */
        private ScheduleConfig schedule = new ScheduleConfig();
    }

    @Getter
    @Setter
    public static class ScheduleConfig {
        /**
         * 是否开启定时刷新
         */
        private boolean enabled = false;

        /**
         * 轮询间隔（默认 30 秒，支持 Duration 格式：30s / 1m / PT30S）
         */
        private Duration interval = Duration.ofSeconds(30);
    }
}
