package com.atomize.jpaplus.core.executor;

import com.atomize.jpaplus.core.field.FieldEngine;
import com.atomize.jpaplus.core.interceptor.InterceptorChain;
import com.atomize.jpaplus.core.model.DataInvocation;
import com.atomize.jpaplus.core.model.OperationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * JpaPlusExecutor 默认实现
 *
 * <p>执行流程：
 * <ol>
 *   <li>保存前：{@link FieldEngine#beforeSave} —— 字段级预处理（加密、敏感词、版本等）</li>
 *   <li>拦截器链：{@link InterceptorChain#proceed} —— 依次执行 BEFORE → 核心逻辑 → AFTER</li>
 *   <li>查询后：{@link FieldEngine#afterQuery} —— 字段级后处理（解密、脱敏、字典等）</li>
 * </ol>
 * </p>
 *
 * <p><b>设计模式：</b>模板方法模式（Template Method） —— 固定执行骨架，具体逻辑委托给各组件</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultJpaPlusExecutor implements JpaPlusExecutor {

    private final InterceptorChain interceptorChain;
    private final FieldEngine fieldEngine;
    private final InterceptorChain.CoreExecution coreExecution;

    @Override
    public Object execute(DataInvocation invocation) throws Throwable {
        // 1. 字段引擎预处理（beforeSave）
        if (invocation.type() == OperationType.SAVE && invocation.entity() != null) {
            fieldEngine.beforeSave(invocation.entity(), invocation.entityClass());
        }

        // 2. 执行拦截器链
        Object result = interceptorChain.proceed(invocation, coreExecution);

        // 3. 字段引擎后处理（afterQuery）
        if (invocation.type() == OperationType.QUERY && result instanceof List<?> list) {
            list.forEach(entity -> fieldEngine.afterQuery(entity, invocation.entityClass()));
        } else if (invocation.type() == OperationType.SAVE && result != null) {
            fieldEngine.afterQuery(result, invocation.entityClass());
        }

        return result;
    }
}
