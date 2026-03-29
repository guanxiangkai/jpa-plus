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
val lombokPlugin:             Provider<PluginDependency>                = libs.plugins.lombok
val springBootDependencies:   Provider<MinimalExternalModuleDependency> = libs.spring.boot.dependencies
val slf4jApi:                 Provider<MinimalExternalModuleDependency> = libs.slf4j.api
val testingBundle:            Provider<ExternalModuleDependencyBundle>  = libs.bundles.testing

// ╔═══════════════════════════════════════════════════════════════════════════════════════════════════╗
// ║                         版本号分类（自动判断发布策略）                                                ║
// ║  规范：                                                                                          ║
// ║    正式版    2026.0.0                              → AUTOMATIC（自动发布到 Maven Central）          ║
// ║    预发布    2026.0.0-M1 / -RC1 / -alpha1 / -beta2 → AUTOMATIC（自动发布到 Maven Central）         ║
// ║    快照版    2026.0.0-SNAPSHOT                      → 禁止上传 Central Portal                      ║
// ╚═══════════════════════════════════════════════════════════════════════════════════════════════════╝
val versionStr   = version.toString()
val isSnapshot   = versionStr.endsWith("-SNAPSHOT", ignoreCase = true)
val isPreRelease = !isSnapshot && Regex(
    ".*-(?:M\\d+|RC\\d+|alpha\\.?\\d*|beta\\.?\\d*|CR\\d+)", RegexOption.IGNORE_CASE
).matches(versionStr)
val isRelease    = !isSnapshot && !isPreRelease

val publishingType = if (!isSnapshot) "AUTOMATIC" else "NONE"
val versionLabel   = when {
    isRelease    -> "正式版 ✅"
    isPreRelease -> "预发布 🟡"
    else         -> "快照版 🔴"
}
logger.lifecycle("📌 版本: $versionStr | 类型: $versionLabel | 发布策略: $publishingType")

// ── 发布凭证 & 暂存目录 ──
val sonatypeUser: String = findProperty("sonatypeUsername") as String? ?: ""
val sonatypePass: String = findProperty("sonatypePassword") as String? ?: ""
val stagingDir: Provider<Directory> = layout.buildDirectory.dir("staging-deploy")

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
        options {
            this as StandardJavadocDocletOptions
            encoding = "UTF-8"
            addBooleanOption("-enable-preview", true)
            addStringOption("source", "25")
            addBooleanOption("Xdoclint:none", true)
        }
        isFailOnError = false
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom { configureJpaPlusPom(project.name) }
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
        isRequired = sonatypeUser.isNotBlank() && !isSnapshot
        sign(the<PublishingExtension>().publications)
    }

    dependencies {
        "api"(platform(springBootDependencies.get()))
        "api"(slf4jApi)
        testImplementation(testingBundle)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
        testLogging { events("passed", "skipped", "failed") }
    }
}

// ╔═══════════════════════════════════════════════════════════════════════════════════════════════════╗
// ║                      Central Portal 发布流水线（三步：清理 → 打包 → 上传）                            ║
// ╚═══════════════════════════════════════════════════════════════════════════════════════════════════╝

// ① 清理上次暂存产物
val cleanStaging by tasks.registering(Delete::class) {
    group = "publishing"
    description = "清理 Central Portal 本地暂存目录"
    delete(stagingDir)
}

// ② 将所有子模块产物打成一个 ZIP 部署包
val bundleForCentralPortal by tasks.registering(Zip::class) {
    group = "publishing"
    description = "将暂存仓库打包为 Central Portal 部署包（ZIP）"
    dependsOn(cleanStaging)
    dependsOn(subprojects.map { it.tasks.named("publishMavenJavaPublicationToStagingRepository") })
    from(stagingDir)
    archiveFileName.set("central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central-portal"))
}

// ③ 上传 ZIP 到 Sonatype Central Portal
val publishToCentralPortal by tasks.registering(Exec::class) {
    group = "publishing"
    description = "上传部署包到 Central Portal（正式版 & 预发布均 AUTOMATIC / 快照禁止）"
    dependsOn(bundleForCentralPortal)

    val bundleFile = layout.buildDirectory.file("central-portal/central-bundle.zip")

    doFirst {
        require(!isSnapshot)                      { "❌ SNAPSHOT 版本 ($versionStr) 不允许上传 Central Portal" }
        require(sonatypeUser.isNotBlank())         { "请在 ~/.gradle/gradle.properties 中配置 sonatypeUsername" }
        require(sonatypePass.isNotBlank())         { "请在 ~/.gradle/gradle.properties 中配置 sonatypePassword" }
        require(bundleFile.get().asFile.exists())  { "部署包不存在: ${bundleFile.get().asFile.path}" }

        val emoji = if (isRelease) "🚀" else "🟡"
        println("$emoji 正在上传 $versionStr 到 Central Portal（publishingType=$publishingType）…")
    }

    commandLine(
        "curl", "--fail-with-body",
        "-X", "POST",
        "https://central.sonatype.com/api/v1/publisher/upload",
        "-u", "$sonatypeUser:$sonatypePass",
        "-F", "bundle=@${bundleFile.get().asFile.path}",
        "-F", "publishingType=$publishingType"
    )
}

// ╔═══════════════════════════════════════════════════════════════════════════════════════════════════╗
// ║                                    工具函数                                                      ║
// ╚═══════════════════════════════════════════════════════════════════════════════════════════════════╝

/** POM 元数据配置（Maven Central 要求） */
fun org.gradle.api.publish.maven.MavenPom.configureJpaPlusPom(projectName: String) {
    name.set(projectName)
    description.set("JPA Plus - 企业级 JPA 增强框架 :: $projectName")
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
