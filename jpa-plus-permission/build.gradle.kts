/**
 * jpa-plus-permission — 数据权限
 *
 * <p>提供 PermissionInterceptor，在查询前注入数据权限条件到 AST</p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(project(":jpa-plus-query"))
}

