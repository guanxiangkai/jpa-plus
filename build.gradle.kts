/**
 * 这是一个 Gradle 构建脚本，用于配置项目的构建过程。
 */
plugins {
    java
    alias(libs.plugins.lombok)
    alias(libs.plugins.springboot) apply false
}

allprojects {
    group = project.group
    version = project.version

    val jdk: String by project
    val encoding: String by project

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = jdk
        targetCompatibility = jdk
        options.encoding = encoding
        options.compilerArgs.addAll(listOf("-parameters", "--enable-preview"))
    }
}

// ── 在 subprojects{} 中无法直接访问 libs，提前提取引用 ──
val lombokPlugin: Provider<PluginDependency> = libs.plugins.lombok
val springBootDependencies: Provider<MinimalExternalModuleDependency> = libs.spring.boot.dependencies
val slf4jApi: Provider<MinimalExternalModuleDependency> = libs.slf4j.api
val testingBundle: Provider<ExternalModuleDependencyBundle> = libs.bundles.testing

// ── 版本分类 & 发布凭证 ──
val ver = VersionClassification(version.toString())
val sonatypeUser = findProperty("sonatypeUsername") as String? ?: ""
val sonatypePass = findProperty("sonatypePassword") as String? ?: ""
val stagingDir = layout.buildDirectory.dir("staging-deploy")

// ╔═══════════════════════════════════════════════════════════════════════════════════════════════════╗
// ║                                   子模块公共配置                                                   ║
// ╚═══════════════════════════════════════════════════════════════════════════════════════════════════╝
subprojects {

    apply {
        plugin("java-library")
        plugin("maven-publish")
        plugin("signing")
        plugin(lombokPlugin.get().pluginId)
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<Javadoc>().configureEach {
        val projectJdk = project.findProperty("jdk") as String
        val projectEncoding = project.findProperty("encoding") as String
        options {
            this as StandardJavadocDocletOptions
            encoding = projectEncoding
            addBooleanOption("-enable-preview", true)
            addStringOption("source", projectJdk)
            addBooleanOption("Xdoclint:none", true)
        }
        isFailOnError = false
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom { configureJpaPlusPom(project, project.name) }
            }
        }
        repositories {
            maven {
                name = "staging"
                url = uri(stagingDir)
            }
        }
    }

    configure<SigningExtension> {
        useGpgCmd()
        isRequired = sonatypeUser.isNotBlank() && !ver.isSnapshot
        sign(the<PublishingExtension>().publications)
    }

    dependencies {
        "implementation"(platform(springBootDependencies.get()))
        "implementation"(slf4jApi)
        testImplementation(testingBundle)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("--enable-preview", "-Xshare:off")
        testLogging { events("passed", "skipped", "failed") }
    }
}

// ╔═══════════════════════════════════════════════════════════════════════════════════════════════════╗
// ║                      Central Portal 发布流水线（三步：清理 → 打包 → 上传）                            ║
// ╚═══════════════════════════════════════════════════════════════════════════════════════════════════╝
registerCentralPortalTasks(ver, sonatypeUser, sonatypePass, stagingDir)


