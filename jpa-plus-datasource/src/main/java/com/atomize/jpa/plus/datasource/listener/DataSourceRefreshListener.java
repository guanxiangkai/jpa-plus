package com.atomize.jpa.plus.datasource.listener;

import com.atomize.jpa.plus.datasource.registry.DynamicDataSourceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * 数据源配置变更监听器
 *
 * <p>监听 Spring 的 {@link ContextRefreshedEvent}，
 * 在应用上下文刷新时调用 {@link DynamicDataSourceRegistry#reload()} 差异化刷新数据源。</p>
 *
 * <p>搭配 Nacos / Apollo / Spring Cloud Config 等配置中心时，
 * 配置变更触发的 EnvironmentChangeEvent 也可通过注册额外监听器调用 {@code registry.reload()} 实现热更新。</p>
 *
 * <p><b>设计模式：</b>观察者模式（Observer） —— 使用 {@code @EventListener} 替代
 * 传统的 {@code ApplicationListener} 接口实现，更简洁且支持条件过滤</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
@RequiredArgsConstructor
public class DataSourceRefreshListener {

    private final DynamicDataSourceRegistry registry;

    /**
     * 应用上下文刷新时差异化重新加载数据源
     */
    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        registry.reload();
    }
}
