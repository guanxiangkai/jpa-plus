/**
 * jpa-plus-logic-delete — 逻辑删除
 *
 * <p>提供 @LogicDelete 注解、LogicDeleteFieldHandler 与 LogicDeleteInterceptor</p>
 * <p>查询时自动追加 deleted=0，删除时改写为 UPDATE SET deleted=1</p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(project(":jpa-plus-query"))
}

