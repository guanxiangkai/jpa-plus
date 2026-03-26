package com.atomize.jpa.plus.datasource.registry;

import com.atomize.jpa.plus.datasource.creator.DataSourceCreator;
import com.atomize.jpa.plus.datasource.model.DataSourceDefinition;
import com.atomize.jpa.plus.datasource.provider.DataSourceProvider;
import com.atomize.jpa.plus.datasource.routing.DynamicRoutingDataSource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动态数据源注册中心
 *
 * <p>管理所有数据源的生命周期。通过 {@link DataSourceProvider} 获取用户提供的配置列表，
 * 结合 {@link DataSourceCreator} 创建实例，变更后自动同步到 {@link DynamicRoutingDataSource}。</p>
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>{@link #init()} —— 启动时从 Provider 加载全部数据源</li>
 *   <li>{@link #reload()} —— 配置变更时从 Provider 重新加载，差异化刷新（新增/更新/移除）</li>
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

    /**
     * 活跃数据源注册表
     */
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * 数据源定义快照（用于 reload 时判断配置是否变更）
     *
     * <p>注意：通过 {@link #setPrimaryDataSource(DataSource)} 注册的主数据源
     * 不在此快照中，因此 {@link #reload()} 不会移除或修改主数据源。</p>
     */
    private final Map<String, DataSourceDefinition> definitionMap = new ConcurrentHashMap<>();

    /**
     * 预注册的主数据源（来自 Spring Boot {@code spring.datasource.*} 配置）
     * -- SETTER --
     * 设置预注册的主数据源
     * <p>此数据源将作为 "master" 注册到路由表中，不受 Provider 管理。
     * 适用于主数据源由 Spring Boot 自动配置的场景。</p>
     *
     */
    @Setter
    private DataSource primaryDataSource;

    public DynamicDataSourceRegistry(DynamicRoutingDataSource routingDataSource,
                                     DataSourceCreator creator,
                                     DataSourceProvider provider) {
        this.routingDataSource = Objects.requireNonNull(routingDataSource);
        this.creator = Objects.requireNonNull(creator);
        this.provider = Objects.requireNonNull(provider);
    }

    /**
     * 初始化 —— 注册主数据源 + 从 Provider 加载额外数据源（启动时调用）
     */
    public synchronized void init() {
        // ① 预注册主数据源（不在 definitionMap 中，reload 时不会被移除）
        if (primaryDataSource != null && !dataSourceMap.containsKey(DynamicRoutingDataSource.MASTER)) {
            dataSourceMap.put(DynamicRoutingDataSource.MASTER, primaryDataSource);
            log.debug("Primary datasource pre-registered as '{}'", DynamicRoutingDataSource.MASTER);
        }

        // ② 从 Provider 加载额外数据源
        List<DataSourceDefinition> definitions = provider.provide();
        for (DataSourceDefinition def : definitions) {
            DataSource ds = creator.createDataSource(def);
            dataSourceMap.put(def.name(), ds);
            definitionMap.put(def.name(), def);
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

        // ① 新增 / 更新
        for (DataSourceDefinition def : latest) {
            DataSourceDefinition existing = definitionMap.get(def.name());
            if (existing == null) {
                // 新增
                doAdd(def);
                log.info("DataSource '{}' added → {}", def.name(), def.url());
            } else if (!existing.equals(def)) {
                // 配置变更 → 刷新
                doRefresh(def);
                log.info("DataSource '{}' refreshed → {}", def.name(), def.url());
            }
        }

        // ② 移除（Provider 不再返回的）
        Set<String> toRemove = new HashSet<>(definitionMap.keySet());
        toRemove.removeAll(latestMap.keySet());
        for (String name : toRemove) {
            doRemove(name);
            log.info("DataSource '{}' removed (no longer provided)", name);
        }

        syncToRouting();
        log.info("DataSource reload complete: {}", dataSourceMap.keySet());
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
        if (DynamicRoutingDataSource.MASTER.equals(name)) {
            throw new IllegalArgumentException("Cannot remove master datasource");
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

    private void doAdd(DataSourceDefinition def) {
        DataSource ds = creator.createDataSource(def);
        dataSourceMap.put(def.name(), ds);
        definitionMap.put(def.name(), def);
    }

    private void doRefresh(DataSourceDefinition def) {
        DataSource old = dataSourceMap.get(def.name());
        DataSource newDs = creator.createDataSource(def);
        dataSourceMap.put(def.name(), newDs);
        definitionMap.put(def.name(), def);
        // 新数据源就绪后再关闭旧的，减少不可用窗口
        if (old != null) {
            closeQuietly(old);
        }
    }

    private void doRemove(String name) {
        DataSource removed = dataSourceMap.remove(name);
        definitionMap.remove(name);
        if (removed != null) {
            closeQuietly(removed);
        }
    }

    private void syncToRouting() {
        Map<Object, Object> targetMap = new HashMap<>(dataSourceMap);
        routingDataSource.setTargetDataSources(targetMap);

        DataSource master = dataSourceMap.get(DynamicRoutingDataSource.MASTER);
        if (master != null) {
            routingDataSource.setDefaultTargetDataSource(master);
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

