<p align="center">
  <h1 align="center">JPA Plus</h1>
  <p align="center">🚀 基于 JDK 25 + Spring Boot 4.1 的企业级 JPA 增强框架</p>
  <p align="center">
    <img src="https://img.shields.io/badge/JDK-25-blue" alt="JDK 25"/>
    <img src="https://img.shields.io/badge/Spring%20Boot-4.1-green" alt="Spring Boot 4.1"/>
    <img src="https://img.shields.io/badge/License-Apache%202.0-orange" alt="License"/>
    <img src="https://img.shields.io/badge/Version-2026.0.0-brightgreen" alt="Version"/>
  </p>
</p>

---

## 📖 项目简介

**JPA Plus** 是一个轻量级、模块化的 JPA 增强框架，致力于在 **不改变 JPA 使用习惯** 的前提下，为企业级应用提供开箱即用的数据治理能力。

### 核心理念

- **只增强不改变** — 继承 `JpaRepository` 的所有能力，额外扩展 Lambda DSL 查询、字段加密、脱敏、审计等能力
- **模块化按需引入** — 8 个功能模块独立成 JAR，按需组合，不引入不生效
- **零侵入注解驱动** — 通过 `@Encrypt`、`@Desensitize`、`@LogicDelete` 等注解声明式启用，无需修改业务代码
- **现代 Java 技术栈** — 充分利用 JDK 25 的 `ScopedValue`、虚拟线程、`record`、`sealed class`、模式匹配等新特性

---

## ✨ 功能特性

| 功能模块      | 说明                                                           | 注解 / API                       |
|-----------|--------------------------------------------------------------|--------------------------------|
| **查询增强**  | Lambda DSL 条件构造、多表 Join、分页优化、多方言 SQL 编译                      | `QueryWrapper` / `JoinWrapper` |
| **字段加密**  | 保存前自动加密，查询后自动解密（AES 等）                                       | `@Encrypt`                     |
| **数据脱敏**  | 查询后自动掩码（手机号、邮箱、身份证、姓名、银行卡、地址等）                               | `@Desensitize`                 |
| **敏感词检测** | 保存前检测敏感词，支持拒绝 / 替换策略                                         | `@SensitiveWord`               |
| **字典回写**  | 查询后自动翻译字典值为标签                                                | `@Dict`                        |
| **乐观锁**   | 保存时自动递增版本号                                                   | `@Version`                     |
| **逻辑删除**  | 查询自动追加未删除条件，删除改写为 UPDATE                                     | `@LogicDelete`                 |
| **多租户隔离** | 自动注入 `tenant_id` 条件实现数据隔离                                    | `TenantInterceptor`            |
| **数据权限**  | 查询前自动注入行级权限条件                                                | `PermissionInterceptor`        |
| **审计日志**  | 操作完成后发布 Spring 事件，支持异步审计                                     | `AuditEvent`                   |
| **自动排序**  | 实体字段声明默认排序规则，查询时自动注入 ORDER BY                                | `@AutoOrderBy`                 |
| **多数据源**  | 基于 `ScopedValue` 的线程安全数据源切换，YAML 声明式多数据源 + 自动检测 DatabaseType | `@DS` / `JpaPlusContext`       |

---

## 🏗️ 模块结构

```
jpa-plus
├── jpa-plus-core           # 核心引擎 — 拦截器链、字段引擎、SPI 加载器、统一异常、反射工具
├── jpa-plus-query          # 查询 DSL — Lambda Wrapper、AST 条件树、SQL 编译器（ScopedValue 参数命名）
├── jpa-plus-field          # 字段治理 — @Encrypt 加密 / @Desensitize 脱敏 / @SensitiveWord 敏感词
│                           #           @Dict 字典回写 / @Version 乐观锁
├── jpa-plus-interceptor    # 数据拦截 — @LogicDelete 逻辑删除 / @AutoOrderBy 自动排序
│                           #           PermissionInterceptor 数据权限 / TenantInterceptor 多租户
├── jpa-plus-audit          # 审计日志 — AuditInterceptor + Spring Event
├── jpa-plus-datasource     # 多数据源 — @DS + ScopedValue 上下文 + YAML 声明式配置 + DatabaseType 自动检测 + 动态刷新
├── jpa-plus-starter        # Spring Boot Starter — 自动装配 + JpaPlusRepository
└── jpa-plus-all            # 全功能聚合包（一次引入所有模块）
```

---

## 📦 获取依赖

### 环境要求

| 要求          | 最低版本                        |
|-------------|-----------------------------|
| JDK         | 25+（需启用 `--enable-preview`） |
| Spring Boot | 4.1+                        |
| Gradle      | 9.4+（Kotlin DSL）            |

### 第 1 步：配置 GitHub Packages 仓库

> ⚠️ GitHub Packages **读取也需要认证**（即使仓库是 public），需先配置 Token。

前往 [GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens)
，创建一个具有 **`read:packages`** 权限的 Token。

在 **全局** `~/.gradle/gradle.properties` 中添加（此文件不在项目中，不会被 Git 提交）：

```properties
gpr.user=你的GitHub用户名
gpr.key=ghp_你的PersonalAccessToken
```

### 第 2 步：添加仓库地址

```kotlin
// settings.gradle.kts 或 build.gradle.kts
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/guanxiangkai/jpa-plus")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### 第 3 步：添加依赖

```kotlin
// build.gradle.kts
dependencies {
    // 方式一：引入 Starter（推荐，包含所有模块 + 自动装配）
    implementation("com.atomize:jpa-plus-starter:2026.0.0")

    // 方式二：按需引入（仅引入需要的模块）
    implementation("com.atomize:jpa-plus-core:2026.0.0")
    implementation("com.atomize:jpa-plus-query:2026.0.0")
    implementation("com.atomize:jpa-plus-field:2026.0.0")        // 字段治理（加密/脱敏/字典/敏感词/乐观锁）
    implementation("com.atomize:jpa-plus-interceptor:2026.0.0")  // 数据拦截（逻辑删除/排序/权限/租户）
    // ... 其他模块按需添加

    // 方式三：全量引入（不含 Spring Boot 自动装配）
    implementation("com.atomize:jpa-plus-all:2026.0.0")
}
```

---

## 🆕 2026.0.0 版本优化

### 动态数据源自动发现

- **YAML 声明式多数据源** — `spring.datasource.dynamic.datasource.*` 直接在 YAML 中声明所有数据源（含 master），无需单独的
  `spring.datasource.*` 配置
- **DatabaseType 自动检测** — 根据 JDBC URL 前缀自动识别数据库类型（MySQL / PostgreSQL / Oracle 等 10 种），无需手动指定
  `driver-class-name`
- **全局 + 局部 HikariCP 配置** — 全局 `hikari` 作为默认值，各数据源可通过自己的 `hikari` 节点覆盖
- **Strict 严格模式** — `strict: true` 时，路由 key 不存在则抛异常，防止误路由到默认库
- **可配置 Primary** — `primary: master` 指定默认数据源名称，不再硬编码 "master"
- **JDBC 配置表（可选）** — `JdbcDataSourceProvider` 从数据库表读取数据源配置，可与 YAML 配置共存
- **配置中心友好** — `EnvironmentDataSourceProvider` 每次从 Spring `Environment` 重新绑定，天然支持 Nacos / Apollo 推送
- **多方言自动建表** — 根据主库类型自动选择 MySQL / PostgreSQL / Oracle / H2 方言 DDL 创建配置表
- **多种动态刷新机制** — 定时轮询 `ScheduledDataSourceRefresher`、编程式 `DataSourceRefresher.refresh()`、事件驱动
  `DataSourceChangeEvent`
- **HikariCP 创建器** — `HikariDataSourceCreator` 开箱即用，支持完整连接池参数配置
- **Spring Boot 4.x 自动装配** — `JpaPlusDataSourceAutoConfiguration` 在 `DataSourceAutoConfiguration` 之前运行，透明替换为路由数据源

### 字典回写零代码

- **内置 `JdbcDictProvider`** — 从数据库表（`jpa_plus_dict`）自动读取字典数据，开启 `jpa-plus.dict.jdbc.enabled=true`
  即可，用户无需手动实现 `DictProvider`
- **多方言自动建表** — 自动识别 MySQL / PostgreSQL / Oracle / H2 方言并创建字典表
- **向后兼容** — 用户仍可自定义 `DictProvider` Bean（如 Redis 缓存方案），内置实现自动让位（`@ConditionalOnMissingBean`）

### 设计模式与代码质量

- **提取通用实例化工具** — `ReflectionUtils.instantiate()` 消除 5 个模块中重复的 `instantiate()` 私有方法
- **`@FunctionalInterface` 标注** — `EncryptKeyProvider`、`MaskStrategy`、`DataSourceCreator`、`SqlCompiler`、
  `ResultSetMapper` 等单方法接口均标注，支持 Lambda 表达式传参
- **异常处理改进** — `EncryptFieldHandler` 加解密失败时抛出 `JpaPlusException` 而非静默吞异常，避免数据静默损坏
- **NamingUtils 性能优化** — `camelToSnake()` 预分配 StringBuilder 容量，添加空值安全检查

### JDK 25 新特性采用

- **`ScopedValue` 全面替代 `ThreadLocal`** — `ParameterNamingStrategy` 从 `ThreadLocal<AtomicInteger>` 迁移到
  `ScopedValue`，配合 `runWhere()` 块作用域 API，虚拟线程友好且无需手动清理
- **`record` 访问器规范化** — `QueryRuntime` 移除冗余的 `getXxx()` 别名方法，全面使用 record 标准访问器 `where()`、
  `orderBys()`、`offset()`、`rows()` 等

### Spring Boot 4.1 最佳实践

- **`@EventListener` 替代 `ApplicationListener`** — `DataSourceRefreshListener` 使用声明式事件监听，更简洁且符合 Spring
  Boot 4 惯用方式

---

## 📋 版本信息

| 属性       | 值             |
|----------|---------------|
| Group    | `com.atomize` |
| Version  | `2026.0.0`    |
| JDK      | `25`          |
| Encoding | `UTF-8`       |

---

## 🗺️ 路线图

- [ ] 支持更多数据库方言（Oracle、SQL Server、SQLite 等）
- [ ] 集成 Micrometer 可观测性指标（拦截器耗时、字段处理耗时等）
- [ ] 拦截器链支持 SPI 动态扩展（`InterceptorChainContributor`）
- [ ] 事件总线异步模式（虚拟线程 + 可配置 `ErrorHandler`）
- [ ] 敏感词模块内置 DFA 引擎实现
- [x] ~~自动排序 `@AutoOrderBy` 注解支持~~
- [x] ~~`ScopedValue` 全面替代 `ThreadLocal`~~
- [x] ~~通用反射实例化工具 `ReflectionUtils.instantiate()`~~
- [x] ~~SPI 接口 `@FunctionalInterface` 标注~~
- [x] ~~动态数据源 JDBC 配置表自动发现 + 动态刷新~~
- [ ] 完善单元测试与集成测试覆盖
- [ ] 发布到 Maven Central

---

## 📄 开源协议

本项目基于 [Apache License 2.0](LICENSE.txt) 开源协议发布。

```
Copyright 2026 atomize

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/guanxiangkai">guanxiangkai</a>
</p>

