/**
 * 这是一个 Gradle 构建脚本，用于配置项目的构建过程。
 *
 */
plugins {
    java
    alias(libs.plugins.lombok)
    alias(libs.plugins.springboot) apply false   // 仅下载到 classpath，子模块按需 apply
}

allprojects {
    // 为所有项目设置项目分组和版本
    group = project.group
    version = project.version

    // 从 gradle.properties 中获取属性值
    val jdk: String by project
    val encoding: String by project

    // 编译选项
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = jdk
        targetCompatibility = jdk
        options.encoding = encoding
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "--enable-preview"
            )
        )
    }
}

// ── 在 subprojects{} 中无法直接访问 libs，提前提取引用 ──
val lombokPlugin: Provider<PluginDependency> = libs.plugins.lombok
val springBootDependencies: Provider<MinimalExternalModuleDependency> = libs.spring.boot.dependencies
val slf4jApi: Provider<MinimalExternalModuleDependency> = libs.slf4j.api
val testingBundle: Provider<ExternalModuleDependencyBundle> = libs.bundles.testing

subprojects {
    apply {
        plugin("java-library")
        plugin(lombokPlugin.get().pluginId)
    }

    dependencies {
        // 导入 Spring Boot BOM（使用 api 确保版本管理传递到依赖方）
        "api"(platform(springBootDependencies.get()))

        // SLF4J 日志门面（Lombok @Slf4j 生成的代码依赖此 API，版本由 BOM 管理）
        // 注意：Lombok 只是编译期生成 Logger 样板代码，slf4j-api 仍然是必需的运行时依赖
        // 日志实现（logback-classic）不在此声明——库模块不应强制绑定具体日志实现，
        // 测试时由 spring-boot-starter-test 传递引入 logback，生产环境由最终应用决定
        "api"(slf4jApi)

        // 测试全家桶（spring-boot-starter-test + JUnit Jupiter + Platform Launcher）
        testImplementation(testingBundle)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

}
