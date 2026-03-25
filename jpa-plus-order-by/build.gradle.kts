/**
 * jpa-plus-order-by — 自动排序
 *
 * <p>提供 @AutoOrderBy 注解与 AutoOrderByInterceptor</p>
 * <p>查询时若未指定排序，自动按注解声明的默认排序规则排列</p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(project(":jpa-plus-query"))
}

