/**
 * jpa-plus-query — 查询 DSL + Join + SQL 编译
 * 依赖 jpa-plus-core，使用 Jakarta Persistence API
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(libs.jakarta.persistence.api)
    compileOnly(libs.hibernate.core)
    compileOnly(libs.spring.data.commons)
}

