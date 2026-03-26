package com.atomize.jpa.plus.datasource.routing;

import com.atomize.jpa.plus.datasource.context.JpaPlusContext;
import com.atomize.jpa.plus.datasource.enums.DsName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源
 *
 * <p>继承 Spring 的 {@link AbstractRoutingDataSource}，
 * 通过 {@link JpaPlusContext#currentDS()} 获取当前线程/虚拟线程的数据源名称作为路由 key。</p>
 *
 * <h3>路由逻辑</h3>
 * <ol>
 *   <li>方法标注 {@code @DS(DsName.SLAVE)} → AOP 创建 ScopedValue 作用域 → 路由到 slave</li>
 *   <li>方法未标注 {@code @DS} → ScopedValue 无绑定 → 默认路由到 "master"</li>
 *   <li>{@code @DS(DsName.SLAVE)} 方法执行完毕 → 退出 ScopedValue 作用域 → 自动恢复为 "master"</li>
 * </ol>
 *
 * <p><b>关键保证：</b>ScopedValue 是块作用域的，方法结束后自动恢复，
 * 不存在 ThreadLocal 忘记清理导致"粘连到从库"的问题。</p>
 *
 * @author guanxiangkai
 * @see JpaPlusContext
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    /**
     * 主数据源名称常量
     */
    public static final String MASTER = DsName.MASTER;

    @Override
    protected Object determineCurrentLookupKey() {
        String ds = JpaPlusContext.currentDS();
        if (log.isTraceEnabled()) {
            log.trace("DataSource routing → {}", ds);
        }
        return ds;
    }

    /**
     * 延迟初始化 —— 由 {@link com.atomize.jpa.plus.datasource.registry.DynamicDataSourceRegistry}
     * 在注册完所有数据源后调用 {@link #initialize()} 完成实际初始化。
     *
     * <p>重写为空操作，避免 Spring 容器在目标数据源尚未就绪时提前调用导致异常。</p>
     */
    @Override
    public void afterPropertiesSet() {
        // No-op: deferred to initialize()
    }

    /**
     * 执行实际初始化（解析目标数据源映射）
     *
     * <p>由 {@link com.atomize.jpa.plus.datasource.registry.DynamicDataSourceRegistry#init()}
     * 在设置好所有目标数据源后调用。</p>
     */
    public void initialize() {
        super.afterPropertiesSet();
    }
}

