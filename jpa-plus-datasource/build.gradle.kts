/**
 * jpa-plus-datasource — 多数据源路由
 *
 * <p>提供 @DS 注解、动态路由数据源、运行时数据源注册，
 * 基于 ScopedValue 实现线程安全的数据源切换（虚拟线程友好）</p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    compileOnly(libs.bundles.spring.datasource)  // AbstractRoutingDataSource / @Aspect / ApplicationContext
}
