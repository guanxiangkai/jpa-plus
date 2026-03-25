/**
 * jpa-plus-all — 全功能聚合模块
 *
 * <p>聚合所有 JPA-Plus 功能模块，一次引入即可获得全部能力</p>
 * <p>适用于不需要按需引入的场景</p>
 */
dependencies {
    api(project(":jpa-plus-core"))
    api(project(":jpa-plus-query"))
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
    api(project(":jpa-plus-order-by"))
}

