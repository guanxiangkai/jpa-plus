package com.actomize.jpa.plus.sharding.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * 分库分表配置属性
 *
 * <p>配置前缀：{@code jpa-plus.sharding}</p>
 *
 * <h3>配置示例</h3>
 * <pre>{@code
 * jpa-plus:
 *   sharding:
 *     enabled: true
 *     cross-shard-policy: REJECT   # REJECT（默认）/ BEST_EFFORT / SEATA
 *     rules:
 *       - logic-table-name: order
 *         db-count: 4
 *         table-count: 8
 *         db-pattern: "order_db_{index}"
 *         table-pattern: "order_{index}"
 *         sharding-key-field: userId   # 可选，优先使用分片键注解字段
 *       - logic-table-name: user
 *         db-count: 2
 *         table-count: 4
 *         db-pattern: "user_db_{index}"
 *         table-pattern: "user_{index}"
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年04月11日
 */
@Validated
@ConfigurationProperties(prefix = "jpa-plus.sharding")
public class ShardingProperties {

    /**
     * 是否启用分库分表模块（默认 true，有 ShardingRule 配置时才真正生效）
     */
    private boolean enabled = true;

    /**
     * 跨分片写入策略
     *
     * <ul>
     *   <li>{@code REJECT}（默认）—— 禁止跨分片写入，单次操作只能路由到一个分片</li>
     *   <li>{@code BEST_EFFORT} —— 尽力提交，不保证原子性（适合可接受最终一致的场景）</li>
     *   <li>{@code SEATA} —— 交由 Seata 管理分布式事务（需引入 seata-spring-boot-starter）</li>
     * </ul>
     */
    private CrossShardPolicy crossShardPolicy = CrossShardPolicy.REJECT;

    /**
     * 分片规则列表（每条规则对应一张逻辑表）
     */
    @Valid
    private List<RuleConfig> rules = new ArrayList<>();

    // ─── Getters / Setters ───

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CrossShardPolicy getCrossShardPolicy() {
        return crossShardPolicy;
    }

    public void setCrossShardPolicy(CrossShardPolicy crossShardPolicy) {
        this.crossShardPolicy = crossShardPolicy;
    }

    public List<RuleConfig> getRules() {
        return rules;
    }

    public void setRules(List<RuleConfig> rules) {
        this.rules = rules;
    }

    // ─── 跨分片策略枚举 ───

    public enum CrossShardPolicy {
        /**
         * 拒绝跨分片写入（默认，最安全）
         */
        REJECT,
        /**
         * 尽力提交，无分布式事务保证
         */
        BEST_EFFORT,
        /**
         * 由 Seata 管理分布式事务
         */
        SEATA
    }

    // ─── 单条规则配置 ───

    public static class RuleConfig {

        /**
         * 逻辑表名（与实体表名配置一致）
         */
        @NotBlank(message = "logicTableName 不能为空")
        private String logicTableName;

        /**
         * 分库数量（默认 1，不分库）
         */
        @Min(value = 1, message = "dbCount 必须 >= 1")
        private int dbCount = 1;

        /**
         * 每库表数量（默认 1，不分表）
         */
        @Min(value = 1, message = "tableCount 必须 >= 1")
        private int tableCount = 1;

        /**
         * 数据源命名模式，{index} 替换为库序号（0-based）
         */
        @NotBlank(message = "dbPattern 不能为空")
        @Pattern(regexp = ".*\\{index\\}.*", message = "dbPattern 必须包含 {index} 占位符")
        private String dbPattern;

        /**
         * 物理表名命名模式，{index} 替换为表序号（0-based）
         */
        @NotBlank(message = "tablePattern 不能为空")
        @Pattern(regexp = ".*\\{index\\}.*", message = "tablePattern 必须包含 {index} 占位符")
        private String tablePattern;

        /**
         * 分片键字段名（可选，优先使用分片键注解字段）
         */
        private String shardingKeyField;

        // ─── Getters / Setters ───

        public String getLogicTableName() {
            return logicTableName;
        }

        public void setLogicTableName(String logicTableName) {
            this.logicTableName = logicTableName;
        }

        public int getDbCount() {
            return dbCount;
        }

        public void setDbCount(int dbCount) {
            this.dbCount = dbCount;
        }

        public int getTableCount() {
            return tableCount;
        }

        public void setTableCount(int tableCount) {
            this.tableCount = tableCount;
        }

        public String getDbPattern() {
            return dbPattern;
        }

        public void setDbPattern(String dbPattern) {
            this.dbPattern = dbPattern;
        }

        public String getTablePattern() {
            return tablePattern;
        }

        public void setTablePattern(String tablePattern) {
            this.tablePattern = tablePattern;
        }

        public String getShardingKeyField() {
            return shardingKeyField;
        }

        public void setShardingKeyField(String shardingKeyField) {
            this.shardingKeyField = shardingKeyField;
        }
    }
}

