package com.actomize.jpa.plus.audit.spi;

import com.actomize.jpa.plus.audit.event.AuditEvent;
import com.actomize.jpa.plus.audit.event.DataAuditEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认审计事件错误处理器 —— 打印 WARN 级别日志
 *
 * <p>当用户未提供自定义 {@link AuditEventErrorHandler} Bean 时，框架使用此实现。</p>
 *
 * @author guanxiangkai
 * @since 2026年04月11日
 */
@Slf4j
public class LoggingAuditEventErrorHandler implements AuditEventErrorHandler {

    @Override
    public void onError(AuditEvent event, Throwable error) {
        if (event instanceof DataAuditEvent d) {
            String entityName = d.entity() == null ? "null" : d.entity().getClass().getSimpleName();
            log.warn("[jpa-plus] Data audit event publish failed — entity={}, operation={}",
                    entityName, d.operation(), error);
        } else {
            log.warn("[jpa-plus] Audit event publish failed — type={}", event.getClass().getSimpleName(), error);
        }
    }
}
