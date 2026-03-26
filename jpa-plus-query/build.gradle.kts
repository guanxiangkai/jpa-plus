/**
 * jpa-plus-query — 查询 DSL + Join + SQL 编译
 * 依赖 jpa-plus-core，使用 Jakarta Persistence API
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(libs.jakarta.persistence.api)
    compileOnly(libs.bundles.spring.query)       // Hibernate Session / Spring Data Pageable
}

