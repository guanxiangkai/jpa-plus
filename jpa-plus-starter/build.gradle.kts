/**
 * jpa-plus-starter — Spring Boot 自动装配
 *
 * <p>聚合所有模块，提供开箱即用的 Spring Boot Starter。
 * 通过 {@code @AutoConfiguration} 自动注册所有核心 Bean。</p>
 */
plugins {
    alias(libs.plugins.springboot) apply false
}

dependencies {
    // ═══════════ 核心模块 ═══════════
    api(project(":jpa-plus-core"))
    api(project(":jpa-plus-query"))

    // ═══════════ 治理模块（合并后） ═══════════
    api(project(":jpa-plus-field"))          // 字段治理：加密/脱敏/字典/敏感词/乐观锁
    api(project(":jpa-plus-interceptor"))    // 数据拦截：逻辑删除/自动排序/数据权限/多租户

    // ═══════════ 独立治理模块 ═══════════
    api(project(":jpa-plus-audit"))          // 审计日志（依赖 spring-context）
    api(project(":jpa-plus-datasource"))     // 多数据源路由（ScopedValue）

    // ═══════════ Spring Boot ═══════════
    api(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)
}
