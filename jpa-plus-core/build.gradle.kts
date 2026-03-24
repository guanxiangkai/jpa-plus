/**
 * jpa-plus-core — 核心执行引擎
 *
 * <p>提供拦截器链、字段引擎、SPI 加载器等核心基础设施，无 Spring 依赖</p>
 * <p>SLF4J 由根构建脚本统一以 api 级别传递，无需重复声明</p>
 */
dependencies {
    compileOnly(libs.jakarta.persistence.api)
}
