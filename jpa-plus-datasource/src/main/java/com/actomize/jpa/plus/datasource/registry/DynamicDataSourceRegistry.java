package com.actomize.jpa.plus.datasource.registry;

import com.actomize.jpa.plus.datasource.creator.DataSourceCreator;
import com.actomize.jpa.plus.datasource.model.DataSourceDefinition;
import com.actomize.jpa.plus.datasource.provider.DataSourceProvider;
import com.actomize.jpa.plus.datasource.routing.DynamicRoutingDataSource;
import com.actomize.jpa.plus.datasource.spi.DataSourcePostProcessor;
import com.actomize.jpa.plus.datasource.tx.DynamicTransactionManager;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动态数据源注册中心
 *
 * <p>管理所有数据源的生命周期。通过 {@link DataSourceProvider} 获取配置列表，
 * 结合 {@link DataSourceCreator} 创建实例，经 {@link DataSourcePostProcessor} 链处理后，
 * 自动同步到 {@link DynamicRoutingDataSource} 和 {@link DynamicTransactionManager}。</p>
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>{@link #init()} —— 启动时从 Provider 加载全部数据源（含 primary）</li>
 *   <li>{@link #reload()} —— 配置变更时差异化刷新（新增/更新/移除）</li>
 *   <li>{@link #add(DataSourceDefinition)} —— 运行时手动注册</li>
 *   <li>{@link #remove(String)} —— 运行时手动移除</li>
 *   <li>{@link #names()} —— 查看已注册的数据源名称</li>
 * </ul>
 *
 * <p><b>设计模式：</b>注册表模式（Registry） + 观察者模式（变更后通知路由数据源刷新）</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class DynamicDataSourceRegistry {

    private final DynamicRoutingDataSource routingDataSource;
    private final DataSourceCreator creator;
    private final DataSourceProvider provider;
    private final String primaryName;

    /**
     * 数据源后置处理器链（Seata 代理、监控包装等）
     */
    private final List<DataSourcePostProcessor> postProcessors;

    /**
     * 动态事务管理器（可选，为 null 时不联动事务管理器）
     */
    private final DynamicTransactionManager transactionManager;

    /**
     * 活跃数据源注册表
     */
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * 数据源定义快照（用于 reload 时判断配置是否变更）
     */
    private final Map<String, DataSourceDefinition> definitionMap = new ConcurrentHashMap<>();

    /**
     * 全参构造器
     *
     * @param routingDataSource  路由数据源
     * @param creator            数据源创建器
     * @param provider           数据源配置提供者
     * @param postProcessors     后置处理器链（可为空列表）
     * @param transactionManager 动态事务管理器（可为 null）
     */
    public DynamicDataSourceRegistry(DynamicRoutingDataSource routingDataSource,
                                     DataSourceCreator creator,
                                     DataSourceProvider provider,
                                     List<DataSourcePostProcessor> postProcessors,
                                     DynamicTransactionManager transactionManager) {
        this.routingDataSource = Objects.requireNonNull(routingDataSource);
        this.creator = Objects.requireNonNull(creator);
        this.provider = Objects.requireNonNull(provider);
        this.primaryName = routingDataSource.getPrimaryName();
        this.postProcessors = postProcessors != null ? List.copyOf(postProcessors) : List.of();
        this.transactionManager = transactionManager;
    }

    /**
     * 向后兼容的三参数构造器（无后置处理器、无事务管理器）
     */
    public DynamicDataSourceRegistry(DynamicRoutingDataSource routingDataSource,
                                     DataSourceCreator creator,
                                     DataSourceProvider provider) {
        this(routingDataSource, creator, provider, List.of(), null);
    }

    /**
     * 初始化 —— 从 Provider 加载全部数据源（启动时调用）
     */
    public synchronized void init() {
        List<DataSourceDefinition> definitions = provider.provide();
        if (definitions.isEmpty()) {
            throw new IllegalStateException(
                    "No datasource definitions provided! Check 'spring.datasource.dynamic.datasource' configuration.");
        }
        for (DataSourceDefinition def : definitions) {
            try {
                DataSource ds = createAndPostProcess(def);
                dataSourceMap.put(def.name(), ds);
                definitionMap.put(def.name(), def);
                registerTxManager(def.name(), ds);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to create datasource '" + def.name() + "' (url=" + def.url() + "): " + e.getMessage(), e);
            }
        }

        if (!dataSourceMap.containsKey(primaryName)) {
            throw new IllegalStateException(
                    "Primary datasource '" + primaryName + "' not found in provider definitions! " +
                            "Available: " + dataSourceMap.keySet() +
                            ". Check 'spring.datasource.dynamic.primary' matches a configured datasource name.");
        }

        syncToRouting();
        log.info("DataSource registry initialized: {}", dataSourceMap.keySet());
    }

    /**
     * 重新加载 —— 从 Provider 获取最新配置，差异化刷新
     *
     * <p>对比当前快照，自动完成：
     * <ul>
     *   <li>新增 —— Provider 返回了新的数据源名称</li>
     *   <li>更新 —— 同名数据源的配置发生了变更</li>
     *   <li>移除 —— Provider 不再返回的数据源名称</li>
     * </ul>
     * </p>
     */
    public synchronized void reload() {
        List<DataSourceDefinition> latest = provider.provide();
        Map<String, DataSourceDefinition> latestMap = latest.stream()
                .collect(Collectors.toMap(DataSourceDefinition::name, d -> d));

        boolean changed = false;

        // ① 新增 / 更新
        for (DataSourceDefinition def : latest) {
            DataSourceDefinition existing = definitionMap.get(def.name());
            if (existing == null) {
                doAdd(def);
                changed = true;
                log.info("DataSource '{}' added → {}", def.name(), def.url());
            } else if (!existing.equals(def)) {
                doRefresh(def);
                changed = true;
                log.info("DataSource '{}' refreshed → {}", def.name(), def.url());
            }
        }

        // ② 移除（Provider 不再返回的）
        Set<String> toRemove = new HashSet<>(definitionMap.keySet());
        toRemove.removeAll(latestMap.keySet());
        for (String name : toRemove) {
            doRemove(name);
            changed = true;
            log.info("DataSource '{}' removed (no longer provided)", name);
        }

        if (changed) {
            syncToRouting();
            log.info("DataSource reload complete: {}", dataSourceMap.keySet());
        } else {
            log.debug("DataSource reload: no changes detected");
        }
    }

    /**
     * 手动注册新数据源
     */
    public synchronized void add(DataSourceDefinition definition) {
        String name = definition.name();
        if (dataSourceMap.containsKey(name)) {
            throw new IllegalArgumentException("DataSource '" + name + "' already registered, " +
                    "use reload() or remove() first");
        }
        doAdd(definition);
        syncToRouting();
        log.info("DataSource '{}' registered → {}", name, definition.url());
    }

    /**
     * 手动移除数据源
     */
    public synchronized void remove(String name) {
        if (primaryName.equals(name)) {
            throw new IllegalArgumentException("Cannot remove primary datasource '" + primaryName + "'");
        }
        doRemove(name);
        syncToRouting();
        log.info("DataSource '{}' removed", name);
    }

    /**
     * 获取所有已注册的数据源名称
     */
    public Set<String> names() {
        return Collections.unmodifiableSet(dataSourceMap.keySet());
    }

    /**
     * 获取指定数据源
     */
    public DataSource get(String name) {
        return dataSourceMap.get(name);
    }

    /**
     * 关闭所有数据源（应用关停时调用）
     */
    public synchronized void destroy() {
        dataSourceMap.forEach((name, ds) -> closeQuietly(ds));
        dataSourceMap.clear();
        definitionMap.clear();
        log.info("All datasources destroyed");
    }

    // ─── 内部方法 ───

    /**
     * 创建数据源并经过后置处理器链处理
     */
    private DataSource createAndPostProcess(DataSourceDefinition def) {
        DataSource ds = creator.createDataSource(def);
        for (DataSourcePostProcessor processor : postProcessors) {
            ds = processor.postProcess(ds, def.name());
            Objects.requireNonNull(ds,
                    "DataSourcePostProcessor returned null for datasource '" + def.name() + "'");
        }
        return ds;
    }

    private void doAdd(DataSourceDefinition def) {
        DataSource ds = createAndPostProcess(def);
        dataSourceMap.put(def.name(), ds);
        definitionMap.put(def.name(), def);
        registerTxManager(def.name(), ds);
    }

    private void doRefresh(DataSourceDefinition def) {
        DataSource old = dataSourceMap.get(def.name());
        DataSource newDs = createAndPostProcess(def);
        dataSourceMap.put(def.name(), newDs);
        definitionMap.put(def.name(), def);
        registerTxManager(def.name(), newDs);
        // 新数据源就绪后再关闭旧的，减少不可用窗口
        if (old != null) {
            closeQuietly(old);
        }
    }

    private void doRemove(String name) {
        DataSource removed = dataSourceMap.remove(name);
        definitionMap.remove(name);
        if (transactionManager != null) {
            transactionManager.removeDataSource(name);
        }
        if (removed != null) {
            closeQuietly(removed);
        }
    }

    private void registerTxManager(String name, DataSource ds) {
        if (transactionManager != null) {
            transactionManager.registerDataSource(name, ds);
        }
    }

    private void syncToRouting() {
        Map<Object, Object> targetMap = new HashMap<>(dataSourceMap);
        routingDataSource.setTargetDataSources(targetMap);

        DataSource primary = dataSourceMap.get(primaryName);
        if (primary != null) {
            routingDataSource.setDefaultTargetDataSource(primary);
        }

        routingDataSource.initialize();
    }

    private void closeQuietly(DataSource ds) {
        try {
            if (ds instanceof Closeable closeable) {
                closeable.close();
            } else if (ds instanceof AutoCloseable autoCloseable) {
                autoCloseable.close();
            }
        } catch (Exception e) {
            log.warn("Failed to close datasource: {}", e.getMessage());
        }
    }
}

