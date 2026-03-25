package com.atomize.jpa.plus.starter;

import com.atomize.jpa.plus.query.context.FlushMode;
import com.atomize.jpa.plus.query.pagination.CountStrategy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JPA-Plus 配置属性
 *
 * <p>通过 {@code application.yml} 中 {@code jpa-plus.*} 前缀配置：</p>
 * <pre>{@code
 * jpa-plus:
 *   flush-mode: AUTO          # Flush 策略：AUTO / ALWAYS / NEVER
 *   debug:
 *     enabled: true           # 是否开启 SQL 调试输出
 *     print-params: true      # 是否输出参数值
 *   pagination:
 *     count-strategy: SIMPLE  # COUNT 优化策略：SIMPLE / SUBQUERY / FORCE_SUBQUERY
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
}
