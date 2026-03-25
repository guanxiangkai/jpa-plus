/**
 * jpa-plus-all — 全功能聚合模块
 *
 * <p>聚合所有 JPA-Plus 功能模块，一次引入即可获得全部能力</p>
 * <p>适用于不需要按需引入的场景</p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(project(":jpa-plus-query"))
    api(project(":jpa-plus-field"))          // 字段治理：加密/脱敏/字典/敏感词/乐观锁
    api(project(":jpa-plus-interceptor"))    // 数据拦截：逻辑删除/自动排序/数据权限/多租户
    api(project(":jpa-plus-audit"))
    api(project(":jpa-plus-datasource"))
}

