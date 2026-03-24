package com.atomize.jpaplus.audit.interceptor;

import com.atomize.jpaplus.audit.event.AuditEvent;
import com.atomize.jpaplus.core.interceptor.Chain;
import com.atomize.jpaplus.core.interceptor.DataInterceptor;
import com.atomize.jpaplus.core.interceptor.Phase;
import com.atomize.jpaplus.core.model.DataInvocation;
import com.atomize.jpaplus.core.model.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 审计拦截器
 *
 * <p>在数据操作（SAVE / DELETE）完成后，通过 Spring {@link ApplicationEventPublisher}
 * 发布 {@link AuditEvent}。建议配合 {@code @TransactionalEventListener} 在事务提交后处理。</p>
 *
 * <p><b>设计模式：</b>
 * <ul>
 *   <li>观察者模式（Observer） —— 通过 Spring 事件机制解耦审计逻辑</li>
 *   <li>责任链模式（Chain of Responsibility） —— 作为拦截器链中的一环</li>
 * </ul>
 * </p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class AuditInterceptor implements DataInterceptor {

    /**
     * Spring 事件发布器
     */
    private final ApplicationEventPublisher eventPublisher;

    public AuditInterceptor(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public int order() {
        return 600;
    }

    @Override
    public Phase phase() {
        return Phase.AFTER;
    }

    @Override
    public boolean supports(OperationType type) {
        return type == OperationType.SAVE || type == OperationType.DELETE;
    }

    @Override
    public Object intercept(DataInvocation invocation, Chain chain) throws Throwable {
        Object result = chain.proceed(invocation);
        if (invocation.entity() != null) {
            eventPublisher.publishEvent(new AuditEvent(invocation.entity(), invocation.type()));
        }
        return result;
    }
}

