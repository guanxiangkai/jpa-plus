package com.actomize.jpa.plus.core.interceptor;

import com.actomize.jpa.plus.core.model.DataInvocation;

import java.util.Comparator;
import java.util.List;

/**
 * 拦截器链实现
 *
 * <p>按 {@link DataInterceptor#order()} 排序所有拦截器，
 * 依次执行 {@link Phase#BEFORE} 阶段 → 核心逻辑 → {@link Phase#AFTER} 阶段。
 * 每个拦截器通过 {@link Chain#proceed(DataInvocation)} 将控制权传递给下一个。</p>
 *
 * <p><b>设计模式：</b>责任链模式（Chain of Responsibility）
 * —— 将多个拦截器串联成链，每个拦截器决定是否传递到下一环</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public class InterceptorChain {

    private final List<DataInterceptor> beforeInterceptors;
    private final List<DataInterceptor> afterInterceptors;

    public InterceptorChain(List<DataInterceptor> interceptors) {
        // 按 order 排序，再按 phase 分组
        List<DataInterceptor> sorted = interceptors.stream()
                .sorted(Comparator.comparingInt(DataInterceptor::order))
                .toList();

        this.beforeInterceptors = sorted.stream()
                .filter(i -> i.phase() == Phase.BEFORE)
                .toList();
        this.afterInterceptors = sorted.stream()
                .filter(i -> i.phase() == Phase.AFTER)
                .toList();
    }

    /**
     * 执行拦截器链
     *
     * @param invocation    数据调用
     * @param coreExecution 核心执行逻辑（不含拦截器时的实际执行）
     * @return 执行结果
     * @throws Throwable 执行异常
     */
    public Object proceed(DataInvocation invocation, CoreExecution coreExecution) throws Throwable {
        // 构建 BEFORE 链 → 核心执行 → AFTER 链
        Chain chain = buildChain(invocation, coreExecution);
        return chain.proceed(invocation);
    }

    private Chain buildChain(DataInvocation invocation, CoreExecution coreExecution) {

        // 核心执行 + AFTER 链
        Chain coreChain = inv -> {
            Object result = coreExecution.execute(inv);
            // 执行 AFTER 拦截器
            return executeAfterInterceptors(inv, result);
        };

        // BEFORE 链
        return buildBeforeChain(beforeInterceptors, 0, coreChain, invocation);
    }

    private Chain buildBeforeChain(List<DataInterceptor> interceptors, int index, Chain coreChain, DataInvocation invocation) {
        if (index >= interceptors.size()) {
            return coreChain;
        }

        DataInterceptor interceptor = interceptors.get(index);
        return inv -> {
            if (interceptor.supports(inv.type())) {
                Chain next = buildBeforeChain(interceptors, index + 1, coreChain, inv);
                return interceptor.intercept(inv, next);
            } else {
                return buildBeforeChain(interceptors, index + 1, coreChain, inv).proceed(inv);
            }
        };
    }


    private Object executeAfterInterceptors(DataInvocation invocation, Object result) throws Throwable {
        Object current = result;
        for (DataInterceptor interceptor : afterInterceptors) {
            if (interceptor.supports(invocation.type())) {
                // AFTER 拦截器接收的是执行后的结果
                final Object r = current;
                current = interceptor.intercept(invocation, _ -> r);
            }
        }
        return current;
    }

    /**
     * 核心执行逻辑接口（不含拦截器的实际数据操作）
     */
    @FunctionalInterface
    public interface CoreExecution {
        Object execute(DataInvocation invocation) throws Throwable;
    }
}
