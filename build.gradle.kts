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

// ── GitHub Packages 发布凭证（优先读取 gradle.properties，其次读取环境变量） ──
val gprUser: String = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR") ?: ""
val gprKey: String = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN") ?: ""

subprojects {
    apply {
        plugin("java-library")
        plugin("maven-publish")
        plugin(lombokPlugin.get().pluginId)
    }

    // ─────────── 生成源码包 & Javadoc 包 ───────────
    java {
        withSourcesJar()
        withJavadocJar()
    }

    // ─────────── Javadoc 编译选项（避免 preview 特性导致报错） ───────────
    tasks.withType<Javadoc>().configureEach {
        options {
            this as StandardJavadocDocletOptions
            encoding = "UTF-8"
            addBooleanOption("-enable-preview", true)
            addStringOption("source", "25")
            // 允许 Javadoc 警告但不中断构建
            addBooleanOption("Xdoclint:none", true)
        }
        isFailOnError = false
    }

    // ─────────── Maven 发布配置 ───────────
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("gpr") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set("JPA Plus - 企业级 JPA 增强框架 :: ${project.name}")
                    url.set("https://github.com/guanxiangkai/jpa-plus")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("guanxiangkai")
                            name.set("guanxiangkai")
                        }
                    }

                    scm {
                        url.set("https://github.com/guanxiangkai/jpa-plus")
                        connection.set("scm:git:git://github.com/guanxiangkai/jpa-plus.git")
                        developerConnection.set("scm:git:ssh://github.com/guanxiangkai/jpa-plus.git")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/guanxiangkai/jpa-plus")
                credentials {
                    username = gprUser
                    password = gprKey
                }
            }
        }
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
