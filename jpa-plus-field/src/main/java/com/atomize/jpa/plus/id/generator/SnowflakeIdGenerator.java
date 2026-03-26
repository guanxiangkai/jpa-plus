package com.atomize.jpa.plus.id.generator;

/**
 * 雪花算法（Snowflake）ID 生成器
 *
 * <p>生成 64 位分布式唯一 ID，结构如下：</p>
 * <pre>
 * ┌─────────┬──────────────────────────────────┬────────────┬────────────┬──────────────┐
 * │  1 bit  │             41 bits              │   5 bits   │   5 bits   │   12 bits    │
 * │  符号位  │       时间戳差值（毫秒）            │  数据中心ID │  工作节点ID │    序列号     │
 * │   (0)   │     (当前时间 - 自定义纪元)         │   (0~31)   │   (0~31)   │  (0~4095)    │
 * └─────────┴──────────────────────────────────┴────────────┴────────────┴──────────────┘
 * </pre>
 *
 * <h3>特性</h3>
 * <ul>
 *   <li><b>趋势递增</b> —— 时间戳在高位，天然有序，B+ 树索引友好</li>
 *   <li><b>分布式唯一</b> —— 数据中心 + 工作节点 ID 保证多节点不重复</li>
 *   <li><b>高性能</b> —— 纯内存运算，单节点 QPS 可达 400w+/秒</li>
 *   <li><b>无外部依赖</b> —— 不需要数据库或 Redis 参与</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <pre>{@code
 * jpa-plus:
 *   id-generator:
 *     type: SNOWFLAKE
 *     snowflake:
 *       worker-id: ${SNOWFLAKE_WORKER_ID:1}       # 工作节点 ID（0~31）
 *       datacenter-id: ${SNOWFLAKE_DC_ID:1}       # 数据中心 ID（0~31）
 *       epoch: 1700000000000                       # 自定义纪元（毫秒时间戳）
 * }</pre>
 *
 * <p><b>注意：</b>{@code epoch} 一旦确定不可修改，否则可能生成重复 ID。
 * 建议设为项目上线日期附近的时间戳，以最大化可用年限（约 69 年）。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月26日 星期四
 */
public class SnowflakeIdGenerator {

    // ─── 位分配 ───
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    // ─── 最大值 ───
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);         // 31
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 31
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);          // 4095

    // ─── 位移量 ───
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                              // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;         // 17
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    private final long workerId;
    private final long datacenterId;
    private final long epoch;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * @param workerId     工作节点 ID（0~31）
     * @param datacenterId 数据中心 ID（0~31）
     * @param epoch        自定义纪元（毫秒时间戳，不可修改）
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId, long epoch) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "Worker ID must be between 0 and " + MAX_WORKER_ID + ", got: " + workerId);
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException(
                    "Datacenter ID must be between 0 and " + MAX_DATACENTER_ID + ", got: " + datacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.epoch = epoch;
    }

    /**
     * 生成下一个唯一 ID
     *
     * <p>线程安全，同一毫秒内通过序列号区分（最多 4096 个/毫秒/节点）。
     * 如果同一毫秒序列号耗尽，阻塞等待到下一毫秒。</p>
     *
     * @return 64 位唯一 ID
     * @throws IllegalStateException 系统时钟回拨时抛出
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 容忍 5ms 以内的回拨，等待追上
                try {
                    Thread.sleep(offset << 1);
                    timestamp = currentTimeMillis();
                    if (timestamp < lastTimestamp) {
                        throw clockBackwardsException(timestamp);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw clockBackwardsException(timestamp);
                }
            } else {
                throw clockBackwardsException(timestamp);
            }
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内：序列号递增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新的毫秒：序列号归零
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    // ─── 内部方法 ───

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private IllegalStateException clockBackwardsException(long timestamp) {
        return new IllegalStateException(
                "Clock moved backwards. Refusing to generate ID for " +
                        (lastTimestamp - timestamp) + " milliseconds. " +
                        "Last timestamp: " + lastTimestamp + ", current: " + timestamp);
    }
}

