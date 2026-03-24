/**
 * jpa-plus-tenant — 多租户隔离
 *
 * <p>提供 TenantInterceptor，在查询前自动注入 tenant_id 条件</p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(project(":jpa-plus-query"))
}

