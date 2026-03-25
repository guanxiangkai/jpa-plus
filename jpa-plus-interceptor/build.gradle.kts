/**
 * jpa-plus-interceptor — 数据拦截（合并模块）
 *
 * <p>聚合所有 DataInterceptor 实现：
 * <ul>
 *   <li>逻辑删除（@LogicDelete → LogicDeleteInterceptor + LogicDeleteFieldHandler）</li>
 *   <li>自动排序（@AutoOrderBy → AutoOrderByInterceptor）</li>
 *   <li>数据权限（PermissionInterceptor）</li>
 *   <li>多租户隔离（TenantInterceptor）</li>
 * </ul>
 * </p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(project(":jpa-plus-query"))
}

