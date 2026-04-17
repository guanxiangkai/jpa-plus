package com.actomize.jpa.plus.audit.event;

import org.springframework.context.ApplicationEventPublisher;

/**
 * 同步审计事件发布者（默认实现）
 *
 * <p>直接委托给 Spring {@link ApplicationEventPublisher#publishEvent(Object)}，
 * 与旧版行为完全兼容。建议在事务提交后处理。</p>
 *
 * @author guanxiangkai
 * @since 2026年04月11日
 */
public class SyncAuditEventPublisher implements AuditEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SyncAuditEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(AuditEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}

