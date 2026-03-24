/**
 * 此脚本用于配置 Gradle 的多项目构建，优化版本。
 *
 * 功能：
 * - 自动发现根目录下的子项目。
 * - 子项目需包含 `.gradle.kts` 构建文件。
 * - 自动配置子项目的项目目录和构建文件名。
 * - 支持个人的阿里云 Maven 仓库配置。
 *
 * 使用的设计模式：
 * - 策略模式：判断目录是否可以作为子项目的条件。
 * - 工厂方法模式：生成项目名称的逻辑封装。
 * - 模板方法模式：定义子项目注册的通用流程。
 */

pluginManagement {
    // 配置全局插件管理的仓库地址
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 配置依赖解析时的仓库地址
    repositories {
        google()
        mavenCentral()
    }
}

// 设置根项目的名称（优先读取 gradle.properties 中的 name）
rootProject.name = rootDir.name

// 定义需要排除的目录集合，避免将这些目录作为子项目
val excludedDirs = setOf(".git", ".gradle", ".idea", "buildSrc", "build", "gradle", "src", "logs", "profile")

// 定义子项目构建文件的后缀
val buildFileExtension = ".gradle.kts"

// 初始化子项目集合
val subprojects = fileTree(rootDir).matching {
    include("**/*$buildFileExtension") // 匹配所有构建文件
    exclude(excludedDirs.map { "**/$it/**" }) // 排除指定目录
}.files.mapNotNull { file ->
    file.parentFile?.takeIf { dir ->
        dir.isPotentialProject(excludedDirs) // 判断是否是潜在子项目
    }
}.toSet()

// 遍历所有子项目并注册
subprojects.forEach { dir ->
    dir.includeAsProject()
}

/**
 * 扩展函数：判断目录是否可以作为子项目。
 *
 * 使用策略模式，将条件判断逻辑封装为可扩展的方式。
 *
 * @param excludedDirs 排除目录的集合。
 * @return 如果满足条件，返回 true；否则返回 false。
 */
fun File.isPotentialProject(excludedDirs: Set<String>): Boolean {
    return isDirectory && // 必须是目录
            this != rootDir && // 不能是根目录
            name !in excludedDirs && // 不在排除列表中
            listFiles()?.any { it.name.endsWith(buildFileExtension) } == true // 必须包含构建文件
}


/**
 * 扩展函数：将目录注册为子项目。
 *
 * 使用模板方法模式，定义子项目注册的通用流程。
 */
fun File.includeAsProject() {
    val projectName = calculateProjectName(this) // 生成项目名称
    include(":$projectName") // 注册子项目

    // 查找目录中的第一个 .gradle.kts 文件作为构建文件
    val buildFile = listFiles()?.find { it.name.endsWith(buildFileExtension) }

    project(":$projectName").apply {
        projectDir = this@includeAsProject // 设置项目目录
        buildFileName = buildFile?.name ?: "${name}$buildFileExtension" // 设置找到的构建文件名称
    }
    println("已注册子项目: $projectName 位于 ${this.path}，构建文件: ${buildFile?.name}")// 日志输出
}

/**
 * 工厂方法：根据目录层级生成项目名称。
 *
 * 生成的名称符合 Gradle 的多层项目格式。
 *
 * @param dir 当前目录对象。
 * @return 生成的项目名称。
 */
fun calculateProjectName(dir: File): String =
    dir.toRelativeString(rootDir).replace(File.separator, ":")
