package com.actomize.jpa.plus.core.metrics;

import com.actomize.jpa.plus.core.model.OperationType;

/**
 * 空操作指标实现 —— 无 Micrometer 时的默认实现，所有方法均为空，零开销
 *
 * @author guanxiangkai
 * @since 2026年04月11日
 */
final class NoopJpaPlusMetrics implements JpaPlusMetrics {

    @Override
    public void recordChainExecution(OperationType type, long nanos, boolean success) {
        // no-op
    }

    @Override
    public void recordFieldEngineBefore(Class<?> entityClass, long nanos) {
        // no-op
    }

    @Override
    public void recordFieldEngineAfter(Class<?> entityClass, long nanos) {
        // no-op
    }
}

