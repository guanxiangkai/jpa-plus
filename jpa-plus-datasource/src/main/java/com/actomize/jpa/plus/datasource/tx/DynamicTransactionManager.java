package com.actomize.jpa.plus.datasource.tx;

import com.actomize.jpa.plus.datasource.context.JpaPlusContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态路由事务管理器
 *
 * <p>根据 {@link JpaPlusContext#currentDS()} 获取当前线程绑定的数据源名称，
 * 委托给对应的 {@link DataSourceTransactionManager} 执行事务操作。</p>
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li>为每个注册的数据源自动创建独立的 {@link DataSourceTransactionManager}</li>
 *   <li>{@code @Transactional} 触发事务时，根据当前 ScopedValue 路由到正确的事务管理器</li>
 *   <li>事务在同一数据源上下文内保持一致性</li>
 * </ol>
 *
 * <h3>注意事项</h3>
 * <p>此事务管理器仅保证<b>单数据源事务一致性</b>。跨数据源分布式事务需结合
 * {@link com.actomize.jpa.plus.datasource.spi.DataSourcePostProcessor} 集成
 * Seata 等分布式事务框架。</p>
 *
 * <p><b>设计模式：</b>策略模式（Strategy） + 委托模式（Delegation）</p>
 *
 * @author guanxiangkai
 * @see JpaPlusContext
 * @see com.actomize.jpa.plus.datasource.spi.DataSourcePostProcessor
 * @since 2026年03月26日 星期三
 */
@Slf4j
public class DynamicTransactionManager implements PlatformTransactionManager {

    /**
     * 数据源名称 → 事务管理器映射
     */
    private final ConcurrentHashMap<String, DataSourceTransactionManager> txManagerMap = new ConcurrentHashMap<>();

    /**
     * 默认数据源名称（primary）
     */
    private final String primaryName;

    /**
     * @param primaryName 默认数据源名称
     */
    public DynamicTransactionManager(String primaryName) {
        this.primaryName = Objects.requireNonNull(primaryName, "primaryName must not be null");
    }

    /**
     * 注册数据源对应的事务管理器
     *
     * @param name       数据源名称
     * @param dataSource 数据源实例
     */
    public void registerDataSource(String name, DataSource dataSource) {
        var txManager = new DataSourceTransactionManager(dataSource);
        txManager.afterPropertiesSet();
        txManagerMap.put(name, txManager);
        log.debug("Registered TransactionManager for datasource '{}'", name);
    }

    /**
     * 移除数据源对应的事务管理器
     *
     * @param name 数据源名称
     */
    public void removeDataSource(String name) {
        txManagerMap.remove(name);
        log.debug("Removed TransactionManager for datasource '{}'", name);
    }

    /**
     * 获取已注册的所有数据源名称
     */
    public Set<String> registeredNames() {
        return Set.copyOf(txManagerMap.keySet());
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        return determineTxManager().getTransaction(definition);
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        determineTxManager().commit(status);
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        determineTxManager().rollback(status);
    }

    /**
     * 根据当前上下文确定实际的事务管理器
     */
    private DataSourceTransactionManager determineTxManager() {
        String dsName = JpaPlusContext.currentDS(primaryName);
        DataSourceTransactionManager txManager = txManagerMap.get(dsName);
        if (txManager == null) {
            throw new IllegalStateException(
                    "No TransactionManager found for datasource '" + dsName + "'. " +
                            "Registered: " + txManagerMap.keySet() + ". " +
                            "Ensure the datasource is registered before starting a transaction.");
        }
        if (log.isTraceEnabled()) {
            log.trace("Transaction routing → {}", dsName);
        }
        return txManager;
    }
}

