package com.atomize.jpaplus.audit.event;

import com.atomize.jpaplus.core.model.OperationType;

import java.time.Instant;

/**
 * 审计事件
 *
 * <p>记录数据操作的审计信息，由 {@link com.atomize.jpaplus.audit.interceptor.AuditInterceptor}
 * 在操作完成后发布。建议通过 {@code @TransactionalEventListener} 在事务提交后异步处理。</p>
 *
 * <p><b>设计模式：</b>观察者模式（Observer） —— Spring 事件驱动机制</p>
 *
 * @param entity    操作的实体对象
 * @param operation 操作类型（SAVE / DELETE）
 * @param timestamp 操作时间戳
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public record AuditEvent(
        Object entity,
        OperationType operation,
        Instant timestamp
) {
    /**
     * 使用当前时间构造审计事件
     */
    public AuditEvent(Object entity, OperationType operation) {
        this(entity, operation, Instant.now());
    }
}

