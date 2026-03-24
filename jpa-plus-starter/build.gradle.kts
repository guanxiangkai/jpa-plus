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

    // ═══════════ 治理模块（按需引入，此处全部聚合） ═══════════
    api(project(":jpa-plus-encrypt"))
    api(project(":jpa-plus-desensitize"))
    api(project(":jpa-plus-sensitive-word"))
    api(project(":jpa-plus-dict"))
    api(project(":jpa-plus-version"))
    api(project(":jpa-plus-logic-delete"))
    api(project(":jpa-plus-permission"))
    api(project(":jpa-plus-tenant"))
    api(project(":jpa-plus-audit"))
    api(project(":jpa-plus-datasource"))

    // ═══════════ Spring Boot ═══════════
    api(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)
}
