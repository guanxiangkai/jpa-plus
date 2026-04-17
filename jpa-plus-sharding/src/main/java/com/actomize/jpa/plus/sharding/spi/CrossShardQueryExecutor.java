package com.actomize.jpa.plus.sharding.spi;

import com.actomize.jpa.plus.sharding.exception.ShardingRouteException;
import com.actomize.jpa.plus.sharding.model.ShardingTarget;
import com.actomize.jpa.plus.sharding.rule.ShardingRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 跨分片查询执行器 SPI
 *
 * <p>当业务需要对某张分片表执行全量扫描（散射查询）时，实现此接口以定义
 * "如何遍历所有分片并归并结果"的策略。</p>
 *
 * <h3>典型场景</h3>
 * <ul>
 *   <li>运营后台全局导出（容忍较高延迟的低频操作）</li>
 *   <li>内部数据核对与数据修复</li>
 *   <li>按非分片键查询（无法单分片路由时的兜底方案）</li>
 * </ul>
 *
 * <h3>框架内置实现</h3>
 * <ul>
 *   <li>{@link SequentialCrossShardQueryExecutor} —— 顺序遍历所有分片，结果在内存归并（默认）</li>
 *   <li>{@link ParallelCrossShardQueryExecutor} —— 使用虚拟线程并行查询各分片，适合延迟敏感场景</li>
 * </ul>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>散射查询性能与分片数量线性相关，建议仅用于低频、容忍高延迟的场景</li>
 *   <li>结果归并在应用层内存中进行，大量数据时注意 OOM 风险</li>
 *   <li>全局排序和分页在应用层内存完成，须收集全量数据后处理</li>
 * </ul>
 *
 * <p><b>设计模式：</b>策略模式（Strategy）</p>
 *
 * @author guanxiangkai
 * @since 2026年04月12日
 */
public interface CrossShardQueryExecutor {

    /**
     * 遍历所有分片并归并结果（无排序）
     *
     * @param rule      分片规则（提供所有分片目标）
     * @param shardTask 针对单个分片执行查询的任务工厂，入参为当前分片目标
     * @param <T>       结果类型
     * @return 所有分片结果的归并列表（顺序依赖实现策略）
     * @throws Exception 任意分片执行失败时抛出
     */
    <T> List<T> executeAll(ShardingRule rule, ShardTaskFactory<T> shardTask) throws Exception;

    /**
     * 遍历所有分片，归并后按指定 {@link Comparator} 全局排序后返回
     *
     * <p>适合需要按某一字段（如 createdTime、id）全局有序展示的低频查询场景。
     * 结果在应用层内存排序，须先收集全量数据，数据量极大时注意 OOM 风险。</p>
     *
     * @param rule       分片规则
     * @param shardTask  单分片查询任务工厂
     * @param comparator 全局排序规则，{@code null} 时退化为 {@link #executeAll}
     * @param <T>        结果类型
     * @return 全局有序的归并列表
     * @throws Exception 任意分片执行失败时抛出
     */
    default <T> List<T> executeAllSorted(ShardingRule rule,
                                         ShardTaskFactory<T> shardTask,
                                         Comparator<T> comparator) throws Exception {
        List<T> merged = executeAll(rule, shardTask);
        if (comparator != null && !merged.isEmpty()) {
            merged = new ArrayList<>(merged);
            merged.sort(comparator);
        }
        return merged;
    }

    /**
     * 遍历所有分片，归并 → 全局排序 → 内存分页
     *
     * <p>该方法会先收集全部分片结果，再按 {@code comparator} 做全局排序，最后使用
     * {@code page/pageSize} 在内存中截取当前页。因此它更适合总量可控的管理后台、导出预览等场景，
     * 不适合超大结果集的在线高频查询。</p>
     *
     * <p>分页参数采用 <strong>1-based</strong> 约定：{@code page=1} 表示第一页。
     * 若页码超出总范围，则返回空 {@code content}，但仍保留真实 {@code total}。</p>
     *
     * @param rule       分片规则
     * @param shardTask  单分片查询任务工厂
     * @param comparator 全局排序规则，{@code null} 时保持 {@link #executeAll(ShardingRule, ShardTaskFactory)} 的归并顺序
     * @param page       页码（从 1 开始）
     * @param pageSize   每页大小，必须大于 0
     * @param <T>        结果类型
     * @return 当前页结果与分页元信息
     * @throws IllegalArgumentException 当 {@code page < 1} 或 {@code pageSize < 1} 时抛出
     * @throws Exception                任意分片执行失败时抛出
     */
    default <T> PagedResult<T> executePaged(ShardingRule rule,
                                            ShardTaskFactory<T> shardTask,
                                            Comparator<T> comparator,
                                            int page,
                                            int pageSize) throws Exception {
        if (page < 1) throw new IllegalArgumentException("page must be >= 1, got: " + page);
        if (pageSize < 1) throw new IllegalArgumentException("pageSize must be >= 1, got: " + pageSize);

        List<T> all = executeAllSorted(rule, shardTask, comparator);
        int total = all.size();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= total) {
            return new PagedResult<>(List.of(), total, page, pageSize);
        }
        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PagedResult<>(all.subList(fromIndex, toIndex), total, page, pageSize);
    }

    /**
     * 懒惰流式归并（顺序遍历各分片，按分片顺序逐条消费，<strong>不需要将所有数据加载到内存</strong>）
     *
     * <p>与 {@link #executeAll} 相比，本方法返回一个 {@link Stream}，每调用一个分片的数据
     * 时才触发 {@code shardTask.execute(target)}，前一分片的数据不需要在内存中整体保留即可
     * 被消费方处理，显著降低大数据量场景下的峰值内存占用。</p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 边扫描边写文件，始终只在内存中保留当前批次数据
     * try (Stream<Order> stream = crossShardQueryExecutor.executeAsStream(rule, target ->
     *         orderRepository.findByDb(target.table()))) {
     *     stream.forEach(order -> fileWriter.write(order));
     * }
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>此 Stream 未缓冲，每次对 Stream 执行终止操作时才真正查询各分片，<strong>不支持重复消费</strong></li>
     *   <li>单个分片的结果仍在内存中，若单分片数据量极大仍需注意</li>
     *   <li>如需全局排序，请使用 {@link #executeAllSorted} 或 {@code stream.sorted(comparator)}
     *       （sorted() 会在内部收集全量数据，失去流式优势）</li>
     *   <li>跨分片事务在此模式下不可用</li>
     * </ul>
     *
     * @param rule      分片规则
     * @param shardTask 单分片查询任务工厂
     * @param <T>       结果类型
     * @return 覆盖所有分片的懒惰 {@link Stream}；若消费方使用 try-with-resources，调用 {@link Stream#close()} 是安全的
     */
    default <T> Stream<T> executeAsStream(ShardingRule rule, ShardTaskFactory<T> shardTask) {
        // 生成所有分片目标的有序列表
        List<ShardingTarget> targets = new ArrayList<>(rule.totalShards());
        for (int db = 0; db < rule.dbCount(); db++) {
            for (int table = 0; table < rule.tableCount(); table++) {
                targets.add(new ShardingTarget(rule.resolveDb(db), rule.resolveTable(table)));
            }
        }
        // flatMap 实现懒惰遍历：每个 target 的查询在被消费时才发生
        return targets.stream()
                .flatMap(target -> {
                    try {
                        List<T> shardResult = shardTask.execute(target);
                        return shardResult == null ? Stream.empty() : shardResult.stream();
                    } catch (Exception e) {
                        throw new ShardingRouteException(
                                "Cross-shard query failed at shard: " + target, e);
                    }
                });
    }

    /**
     * 懒惰流式归并并按 {@link Comparator} 全局排序
     *
     * <p>注意：{@link Stream#sorted(Comparator)} 是<strong>有状态的中间操作</strong>，
     * 内部会收集所有元素后排序，因此整体内存消耗与 {@link #executeAll} 相当。
     * 本方法的价值在于提供统一的流式 API，适合消费方需要 {@link Stream} 作为输入的场景。</p>
     *
     * @param rule       分片规则
     * @param shardTask  单分片查询任务工厂
     * @param comparator 全局排序规则，{@code null} 时等同于 {@link #executeAsStream(ShardingRule, ShardTaskFactory)}
     * @param <T>        结果类型
     * @return 全局有序的懒惰 {@link Stream}
     */
    default <T> Stream<T> executeAsStream(ShardingRule rule,
                                          ShardTaskFactory<T> shardTask,
                                          Comparator<T> comparator) {
        Stream<T> stream = executeAsStream(rule, shardTask);
        return comparator != null ? stream.sorted(comparator) : stream;
    }

    /**
     * 单个分片任务工厂 —— 将当前分片路由信息传入，返回该分片查询的结果。
     *
     * <p>泛型参数 {@code T} 表示单次查询返回的元素类型。</p>
     */
    @FunctionalInterface
    interface ShardTaskFactory<T> {

        /**
         * 执行当前分片的查询任务。
         *
         * <p>入参为当前分片目标（数据源名 + 物理表名），返回该分片的查询结果；
         * 无数据时可返回空列表或 {@code null}。</p>
         *
         * @throws Exception 单分片查询失败时抛出
         */
        List<T> execute(ShardingTarget target) throws Exception;
    }

    /**
     * 跨分片分页结果
     *
     * <p>包含当前页数据 {@code content}、全量总条数 {@code total}、当前页码 {@code page}
     * 与每页大小 {@code pageSize}。泛型参数 {@code T} 表示结果元素类型。</p>
     */
    record PagedResult<T>(List<T> content, int total, int page, int pageSize) {

        /**
         * 总页数。
         *
         * <p>当 {@code total=0} 时返回 {@code 0}；当存在数据时返回向上取整后的页数。</p>
         */
        public int totalPages() {
            return pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        }

        /**
         * 是否存在下一页
         */
        public boolean hasNext() {
            return page < totalPages();
        }

        /**
         * 是否存在上一页
         */
        public boolean hasPrevious() {
            return page > 1;
        }
    }

    // ─────────────────────────────────────────────
    // 内置实现
    // ─────────────────────────────────────────────

    /**
     * 顺序执行的跨分片查询器（默认实现）
     *
     * <p>依次遍历所有分片，在同一线程中顺序执行任务，将所有结果追加到结果列表后返回。
     * 简单可靠，无并发风险，适合分片数量较少（{@code ≤ 16}）的低频场景。</p>
     */
    class SequentialCrossShardQueryExecutor implements CrossShardQueryExecutor {

        @Override
        public <T> List<T> executeAll(ShardingRule rule, ShardTaskFactory<T> shardTask) throws Exception {
            List<T> merged = new ArrayList<>();
            for (int db = 0; db < rule.dbCount(); db++) {
                for (int table = 0; table < rule.tableCount(); table++) {
                    ShardingTarget target = new ShardingTarget(rule.resolveDb(db), rule.resolveTable(table));
                    List<T> shardResult = shardTask.execute(target);
                    if (shardResult != null) merged.addAll(shardResult);
                }
            }
            return merged;
        }
    }

    /**
     * 并行执行的跨分片查询器（结构化并发实现）
     *
     * <p>使用 JDK 25 {@link java.util.concurrent.StructuredTaskScope} 并行执行所有分片查询：</p>
     * <ul>
     *   <li><b>快速失败</b>：{@code awaitAllSuccessfulOrThrow} — 任一分片失败时立即取消其余分片</li>
     *   <li><b>结构化生命周期</b>：try-with-resources 自动清理作用域，无需手动关闭</li>
     *   <li><b>精确的异常传播</b>：{@link java.util.concurrent.StructuredTaskScope.FailedException}
     *       包装第一个失败原因</li>
     * </ul>
     */
    class ParallelCrossShardQueryExecutor implements CrossShardQueryExecutor {

        @Override
        public <T> List<T> executeAll(ShardingRule rule, ShardTaskFactory<T> shardTask) throws Exception {
            int totalShards = rule.dbCount() * rule.tableCount();
            List<java.util.concurrent.StructuredTaskScope.Subtask<List<T>>> subtasks =
                    new ArrayList<>(totalShards);

            try (var scope = java.util.concurrent.StructuredTaskScope.open(
                    java.util.concurrent.StructuredTaskScope.Joiner.<List<T>>awaitAllSuccessfulOrThrow())) {

                for (int db = 0; db < rule.dbCount(); db++) {
                    for (int table = 0; table < rule.tableCount(); table++) {
                        final ShardingTarget target =
                                new ShardingTarget(rule.resolveDb(db), rule.resolveTable(table));
                        subtasks.add(scope.fork(() -> shardTask.execute(target)));
                    }
                }

                scope.join();

                List<T> merged = new ArrayList<>(totalShards * 10);
                for (var subtask : subtasks) {
                    List<T> result = subtask.get();
                    if (result != null) merged.addAll(result);
                }
                return merged;
            } catch (java.util.concurrent.StructuredTaskScope.FailedException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception ex) throw ex;
                throw new ShardingRouteException("Parallel cross-shard query failed", cause);
            }
        }
    }
}

