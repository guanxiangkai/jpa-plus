/**
 * jpa-plus-audit — 审计日志
 *
 * <p>提供 AuditInterceptor，在数据操作完成后发布 AuditEvent 事件</p>
 * <p>依赖 Spring Context 的 ApplicationEventPublisher</p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    compileOnly(libs.spring.context)
}

