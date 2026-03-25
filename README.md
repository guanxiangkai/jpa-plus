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
- **模块化按需引入** — 14 个功能模块独立成 JAR，按需组合，不引入不生效
- **零侵入注解驱动** — 通过 `@Encrypt`、`@Desensitize`、`@LogicDelete` 等注解声明式启用，无需修改业务代码
- **现代 Java 技术栈** — 充分利用 JDK 25 的 `ScopedValue`、虚拟线程、`record`、`sealed class`、模式匹配等新特性

---

## ✨ 功能特性

| 功能模块      | 说明                                      | 注解 / API                       |
|-----------|-----------------------------------------|--------------------------------|
| **查询增强**  | Lambda DSL 条件构造、多表 Join、分页优化、多方言 SQL 编译 | `QueryWrapper` / `JoinWrapper` |
| **字段加密**  | 保存前自动加密，查询后自动解密（AES 等）                  | `@Encrypt`                     |
| **数据脱敏**  | 查询后自动掩码（手机号、邮箱、身份证、姓名、银行卡、地址等）          | `@Desensitize`                 |
| **敏感词检测** | 保存前检测敏感词，支持拒绝 / 替换策略                    | `@SensitiveWord`               |
| **字典回写**  | 查询后自动翻译字典值为标签                           | `@Dict`                        |
| **乐观锁**   | 保存时自动递增版本号                              | `@Version`                     |
| **逻辑删除**  | 查询自动追加未删除条件，删除改写为 UPDATE                | `@LogicDelete`                 |
| **多租户隔离** | 自动注入 `tenant_id` 条件实现数据隔离               | `TenantInterceptor`            |
| **数据权限**  | 查询前自动注入行级权限条件                           | `PermissionInterceptor`        |
| **审计日志**  | 操作完成后发布 Spring 事件，支持异步审计                | `AuditEvent`                   |
| **多数据源**  | 基于 `ScopedValue` 的线程安全数据源切换             | `@DS` / `JpaPlusContext`       |

---

## 🏗️ 架构设计

### 模块结构

```
jpa-plus
├── jpa-plus-core           # 核心引擎 — 拦截器链、字段引擎、SPI 加载器、统一异常
├── jpa-plus-query          # 查询 DSL — Lambda Wrapper、AST 条件树、SQL 编译器（MySQL / PgSQL）
├── jpa-plus-encrypt        # 字段加密 — @Encrypt + AES 加解密
├── jpa-plus-desensitize    # 数据脱敏 — @Desensitize + 多策略掩码
├── jpa-plus-sensitive-word # 敏感词检测 — @SensitiveWord + SPI 扩展
├── jpa-plus-dict           # 字典回写 — @Dict + DictProvider SPI
├── jpa-plus-version        # 乐观锁 — @Version + 版本自增
├── jpa-plus-logic-delete   # 逻辑删除 — @LogicDelete + 查询条件注入
├── jpa-plus-tenant         # 多租户 — TenantInterceptor + 条件注入
├── jpa-plus-permission     # 数据权限 — PermissionInterceptor + 条件注入
├── jpa-plus-audit          # 审计日志 — AuditInterceptor + Spring Event
├── jpa-plus-datasource     # 多数据源 — @DS + ScopedValue 上下文
├── jpa-plus-starter        # Spring Boot Starter — 自动装配 + JpaPlusRepository
└── jpa-plus-all            # 全功能聚合包（一次引入所有模块）
```

### 模块依赖关系

```
                    ┌─────────────┐
                    │  jpa-plus-  │
                    │   starter   │ ◄── Spring Boot AutoConfiguration
                    └──────┬──────┘
                           │ 聚合所有模块
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
 ┌─────────────┐   ┌─────────────┐   ┌──────────────┐
 │ jpa-plus-   │   │ jpa-plus-   │   │  治理模块     │
 │    core     │   │   query     │   │  (encrypt,   │
 │             │◄──│             │   │  desensitize,│
 └─────────────┘   └─────────────┘   │  dict, ...)  │
       ▲                              └──────┬───────┘
       │                                     │
       └─────────────── 依赖 ────────────────┘
```

### 核心执行流程

```
请求 → JpaPlusRepository
        │
        ▼
  JpaPlusExecutor.execute(DataInvocation)
        │
        ├─ 1. FieldEngine.beforeSave()          # 字段预处理（加密、敏感词、版本自增）
        │
        ├─ 2. InterceptorChain.proceed()         # 拦截器链
        │     ├─ BEFORE 阶段                     #   权限 → 租户 → 逻辑删除 → ...
        │     ├─ 核心执行（EntityManager）         #   实际 JPA 操作
        │     └─ AFTER 阶段                      #   审计日志 → ...
        │
        └─ 3. FieldEngine.afterQuery()           # 字段后处理（解密、脱敏、字典回写）
                │
                ▼
            返回结果
```

### 拦截器执行顺序

| order | 拦截器                      | 阶段     | 说明               |
|-------|--------------------------|--------|------------------|
| 100   | `PermissionInterceptor`  | BEFORE | 数据权限条件注入         |
| 150   | `TenantInterceptor`      | BEFORE | 多租户条件注入          |
| 200   | `LogicDeleteInterceptor` | BEFORE | 逻辑删除条件注入         |
| —     | **核心执行**                 | —      | EntityManager 操作 |
| 600   | `AuditInterceptor`       | AFTER  | 审计事件发布           |

### 字段处理器执行顺序

| order | 处理器                       | beforeSave | afterQuery |
|-------|---------------------------|:----------:|:----------:|
| 50    | `SensitiveWordHandler`    |  ✅ 敏感词检测   |     —      |
| 100   | `EncryptFieldHandler`     |    ✅ 加密    |    ✅ 解密    |
| 200   | `DictFieldHandler`        |     —      |   ✅ 字典回写   |
| 300   | `DesensitizeFieldHandler` |     —      |    ✅ 脱敏    |
| 400   | `VersionFieldHandler`     |   ✅ 版本自增   |     —      |
| 500   | `LogicDeleteFieldHandler` |  ✅ 默认值设置   |     —      |

---

## 🚀 快速开始

### 环境要求

| 要求          | 最低版本                        |
|-------------|-----------------------------|
| JDK         | 25+（需启用 `--enable-preview`） |
| Spring Boot | 4.1+                        |
| Gradle      | 9.4+（Kotlin DSL）            |

### 1. 添加依赖

```kotlin
// build.gradle.kts

// 方式一：引入 Starter（推荐，包含所有模块 + 自动装配）
implementation("com.atomize:jpa-plus-starter:2026.0.0")

// 方式二：按需引入（仅引入需要的模块）
implementation("com.atomize:jpa-plus-core:2026.0.0")
implementation("com.atomize:jpa-plus-query:2026.0.0")
implementation("com.atomize:jpa-plus-encrypt:2026.0.0")
// ... 其他模块按需添加

// 方式三：全量引入（不含 Spring Boot 自动装配）
implementation("com.atomize:jpa-plus-all:2026.0.0")
```

### 2. 定义实体

```java

@Entity
@Table(name = "sys_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Encrypt                                                    // 保存加密，查询解密
    private String phone;

    @Desensitize(strategy = DesensitizeStrategy.EMAIL)          // 查询后邮箱脱敏
    private String email;

    @Dict(type = "user_status")                                 // 查询后自动翻译字典标签
    private Integer status;
    private String statusLabel;                                 // 字典标签回写目标字段

    @SensitiveWord(strategy = SensitiveWordStrategy.REPLACE, replacement = "***")
    private String nickname;                                    // 保存前敏感词替换

    @Version                                                    // 乐观锁版本号
    private Integer version;

    @LogicDelete                                                // 逻辑删除标识
    private Integer deleted;

    // getter / setter ...
}
```

### 3. 定义 Repository

```java
// 只需继承 JpaPlusRepository，即可获得全部增强能力
public interface UserRepository extends JpaPlusRepository<User, Long> {
}
```

### 4. 使用 Lambda DSL 查询

```java

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;

    // ─── 条件查询 ───
    public List<User> findActiveUsers(String keyword) {
        return userRepo.list(
                QueryWrapper.from(User.class)
                        .eq(User::getDeleted, 0)
                        .like(User::getUsername, keyword)
                        .orderByDesc(User::getId)
        );
    }

    // ─── 查询单条 ───
    public Optional<User> findByUsername(String username) {
        return userRepo.one(
                QueryWrapper.from(User.class)
                        .eq(User::getUsername, username)
        );
    }

    // ─── 分页查询 ───
    public PageResult<User> pageUsers(int page, int size) {
        return userRepo.page(
                QueryWrapper.from(User.class),
                PageRequest.of(page - 1, size)
        );
    }

    // ─── 条件更新 ───
    public int disableUser(Long userId) {
        return userRepo.update(
                UpdateWrapper.from(User.class)
                        .set(User::getStatus, 0)
                        .eq(User::getId, userId)
        );
    }

    // ─── 条件删除 ───
    public int deleteByName(String name) {
        return userRepo.delete(
                DeleteWrapper.from(User.class)
                        .eq(User::getUsername, name)
        );
    }

    // ─── 调试 SQL ───
    public void debugQuery() {
        userRepo.debug(
                QueryWrapper.from(User.class)
                        .eq(User::getStatus, 1)
        );
        // 输出：==> SQL: SELECT u.* FROM sys_user u WHERE u.status = :eq_0
        // 输出：==> Params: {eq_0=1}
    }
}
```

### 5. 多表 Join 查询

```java
// 定义 VO
public record UserOrderVO(String username, String orderNo, BigDecimal amount) {
}

// Join 查询
public List<UserOrderVO> queryUserOrders(Long userId) {
    var userAlias = JoinWrapper.from(User.class).as(User.class, "u");
    var orderAlias = JoinWrapper.from(User.class).as(Order.class, "o");

    return queryExecutor.list(
            JoinWrapper.from(User.class)
                    .leftJoin(orderAlias,
                            userAlias.col(User::getId),
                            orderAlias.col(Order::getUserId))
                    .select(
                            userAlias.col(User::getUsername).as("username"),
                            orderAlias.col(Order::getOrderNo).as("orderNo"),
                            orderAlias.col(Order::getAmount).as("amount")
                    )
                    .eq(userAlias.col(User::getId), userId)
                    .orderByDesc(orderAlias.col(Order::getCreateTime)),
            UserOrderVO.class
    );
}
```

### 6. 多数据源切换

```java
// 方式一：注解式
@DS("slave")
public List<User> queryFromSlave() {
    return userRepo.findAll();
}

// 方式二：编程式（基于 ScopedValue，虚拟线程安全）
public User queryFromSlave(Long id) throws Exception {
    return JpaPlusContext.withDS("slave", () -> userRepo.findById(id).orElse(null));
}

// 方式三：无返回值
public void syncData() throws Exception {
    JpaPlusContext.runWithDS("slave", () -> {
        // 在 slave 数据源上执行
    });
}
```

---

## ⚙️ 配置说明

```yaml
jpa-plus:
  # ─── Flush 策略 ───
  # AUTO（默认）：仅当 Session 有脏数据时 flush
  # ALWAYS：每次查询前都 flush
  # NEVER：完全不主动 flush
  flush-mode: AUTO

  # ─── 调试模式 ───
  debug:
    enabled: false          # 是否在日志中输出编译后的 SQL
    print-params: true      # 是否输出参数值

  # ─── 分页优化 ───
  pagination:
    count-strategy: SIMPLE  # COUNT 策略：SIMPLE / SUBQUERY / FORCE_SUBQUERY
    force-subquery-for-join: true  # LEFT JOIN 时是否强制子查询

  # ─── 加密配置 ───
  encrypt:
    key: "${ENCRYPT_KEY:JpaPlusEncKey128}"  # AES 密钥（16/24/32 字节）
    # ⚠️ 生产环境务必通过环境变量或 Vault 注入密钥，切勿明文写入配置文件

  # ─── 自动 Join ───
  auto-join:
    many-to-one-fetch: false  # 是否自动为 @ManyToOne 添加 JOIN FETCH
    max-depth: 3              # Join 最大深度
```

---

## 🔌 SPI 扩展点

JPA Plus 通过自定义 SPI 机制（`META-INF/jpa-plus/` 目录）和 Spring Bean 两种方式实现模块解耦。

### 字典数据提供者

```java
// 实现 DictProvider 接口，注册为 Spring Bean
@Component
public class MyDictProvider implements DictProvider {

    @Override
    public Optional<String> getLabel(String type, Object value) {
        // 从数据库、缓存或远程服务查询字典标签
        return Optional.ofNullable(dictCache.get(type + ":" + value));
    }
}
```

### 敏感词数据提供者

```java
// 实现 SensitiveWordProvider 接口
@Component
public class MySensitiveWordProvider implements SensitiveWordProvider {

    @Override
    public boolean contains(String text) {
        // 对接 DFA / AC 自动机等敏感词检测引擎
        return dfaEngine.match(text);
    }

    @Override
    public String replace(String text, String replacement) {
        return dfaEngine.replace(text, replacement);
    }
}
```

### 多租户 ID 获取

```java

@Component
public class MyTenantInterceptor extends TenantInterceptor {

    @Override
    protected String getCurrentTenantId() {
        // 从安全上下文获取当前租户 ID
        return SecurityContextHolder.getContext().getTenantId();
    }
}
```

### 数据权限条件构建

```java

@Component
public class MyPermissionInterceptor extends PermissionInterceptor {

    @Override
    protected Condition getPermissionCondition(Class<?> entityClass) {
        // 根据当前用户角色构建行级权限条件
        Long deptId = SecurityContextHolder.getContext().getDeptId();
        if (deptId == null) return null; // 无限制
        return new Eq(
                ColumnMeta.of(/* root table */, "dept_id", Long.class),
                deptId
        );
    }
}
```

### 自定义审计监听

```java

@Component
public class AuditEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAudit(AuditEvent event) {
        log.info("审计: entity={}, operation={}, time={}",
                event.entity().getClass().getSimpleName(),
                event.operation(),
                event.timestamp());
        // 写入审计日志表 / 发送消息队列 ...
    }
}
```

### SPI 文件注册

在模块的 `src/main/resources/META-INF/jpa-plus/` 目录下创建以接口全限定名命名的文件：

```
# 文件: META-INF/jpa-plus/com.atomize.jpaplus.core.field.FieldHandler
# 每行一个实现类全限定名，支持 # 注释
com.example.MyCustomFieldHandler
```

---

## 🧩 模块详解

### jpa-plus-core（核心引擎）

框架的基础设施层，**无 Spring 依赖**，提供：

| 组件                       | 类型      | 说明                                                |
|--------------------------|---------|---------------------------------------------------|
| `InterceptorChain`       | 拦截器链    | 按 `order()` 排序，分 BEFORE / AFTER 两个阶段执行            |
| `DataInterceptor`        | 接口      | 拦截器契约 — `order()` + `phase()` + `supports()` 三维控制 |
| `Chain`                  | 函数式接口   | 拦截器链传递 — `chain.proceed(invocation)`              |
| `Phase`                  | 枚举      | 拦截阶段 — `BEFORE`（核心逻辑前）/ `AFTER`（核心逻辑后）            |
| `FieldEngine`            | 字段引擎    | 管理所有 `FieldHandler`，在 beforeSave / afterQuery 时触发 |
| `FieldHandler`           | SPI 接口  | 策略模式，每个模块实现一种字段处理逻辑                               |
| `JpaPlusExecutor`        | 门面接口    | 所有数据操作的统一入口                                       |
| `DefaultJpaPlusExecutor` | 默认实现    | 模板方法：字段预处理 → 拦截器链 → 字段后处理                         |
| `JpaPlusLoader`          | SPI 加载器 | 从 `META-INF/jpa-plus/` 加载实现类，ConcurrentHashMap 缓存 |
| `DataInvocation`         | record  | 不可变调用封装（操作类型 + 实体 + 实体类 + 查询模型）                   |
| `OperationType`          | 枚举      | `QUERY` / `SAVE` / `DELETE`                       |
| `JpaPlusException`       | 异常      | 框架统一异常基类                                          |
| `ReflectionUtils`        | 工具类     | 反射操作集中管理（查找字段、获取/设置值）                             |
| `NamingUtils`            | 工具类     | 驼峰 ⇄ 蛇形命名转换                                       |

### jpa-plus-query（查询 DSL）

类型安全的查询构造与 SQL 编译引擎：

| 子包           | 核心类                        | 说明                                                                                               |
|--------------|----------------------------|--------------------------------------------------------------------------------------------------|
| `wrapper`    | `QueryWrapper<T>`          | Lambda DSL 单表查询构造器                                                                               |
| `wrapper`    | `UpdateWrapper<T>`         | Lambda DSL 更新构造器                                                                                 |
| `wrapper`    | `DeleteWrapper<T>`         | Lambda DSL 删除构造器                                                                                 |
| `wrapper`    | `JoinWrapper<T>`           | 多表 Join 查询构造器                                                                                    |
| `wrapper`    | `SFunction<T, R>`          | 可序列化 Lambda 函数引用（类型安全列名解析）                                                                       |
| `ast`        | `Condition` (sealed)       | 条件 AST 节点：Eq / Ne / Gt / Ge / Lt / Le / Like / In / Between / And / Or / Not / Exists / SubQuery |
| `compiler`   | `SqlCompiler`              | SQL 编译器接口                                                                                        |
| `compiler`   | `MySqlCompiler`            | MySQL 方言（`LIMIT offset, rows`）                                                                   |
| `compiler`   | `PgSqlCompiler`            | PostgreSQL 方言（`LIMIT rows OFFSET offset`）                                                        |
| `compiler`   | `DebugSqlCompiler`         | 装饰器 — 编译后输出 SQL 与参数                                                                              |
| `compiler`   | `SqlResult`                | 编译结果（SQL + 命名参数映射）                                                                               |
| `context`    | `QueryContext`             | 查询上下文 = 元数据（不可变）+ 运行时（可替换）                                                                       |
| `context`    | `FlushStrategy`            | Flush 策略（基于 `Session.isDirty()` 智能判断）                                                            |
| `executor`   | `QueryExecutor`            | 查询执行器接口                                                                                          |
| `executor`   | `DefaultQueryExecutor`     | 默认实现 — 编译 → 绑定参数 → 执行                                                                            |
| `pagination` | `PageResult<T>`            | 分页结果（record）                                                                                     |
| `pagination` | `PaginationOptimizer`      | 分页 COUNT 优化                                                                                      |
| `metadata`   | `TableMeta` / `ColumnMeta` | 表 / 列元数据                                                                                         |
| `metadata`   | `JoinGraph` / `JoinNode`   | Join 关系图                                                                                         |
| `plan`       | `MappingPlan<R>`           | 结果集映射计划（编译期优化）                                                                                   |
| `resolver`   | `JpaRelationResolver`      | JPA 关联关系解析器                                                                                      |

### jpa-plus-encrypt（字段加密）

| 注解 / 类                        | 说明                                                                 |
|-------------------------------|--------------------------------------------------------------------|
| `@Encrypt`                    | 标注在 `String` 字段上                                                   |
| `@Encrypt(algorithm = "AES")` | 可指定加密算法（须为 `javax.crypto.Cipher` 支持的算法）                            |
| `EncryptFieldHandler`         | beforeSave: 明文 → AES 加密 → Base64 编码；afterQuery: Base64 解码 → AES 解密 |

### jpa-plus-desensitize（数据脱敏）

| 策略          | 效果示例                  |
|-------------|-----------------------|
| `PHONE`     | `138****1234`         |
| `EMAIL`     | `a***@example.com`    |
| `ID_CARD`   | `110***********1234`  |
| `NAME`      | `张**`                 |
| `BANK_CARD` | `6222 **** **** 1234` |
| `ADDRESS`   | `北京市海淀区******`        |
| `CUSTOM`    | `首*尾`（首尾各保留一位）        |

```java
// 使用示例
@Desensitize(strategy = DesensitizeStrategy.PHONE)
private String phone;

@Desensitize(strategy = DesensitizeStrategy.EMAIL, maskChar = '#')
private String email;
```

### jpa-plus-sensitive-word（敏感词检测）

| 策略        | 行为                                     |
|-----------|----------------------------------------|
| `REJECT`  | 包含敏感词时抛出 `SensitiveWordException`，阻止保存 |
| `REPLACE` | 将敏感词替换为 `replacement` 指定字符串后继续保存       |

> ⚠️ 框架不内置敏感词库，需实现 `SensitiveWordProvider` 接口对接自有词库或第三方引擎

### jpa-plus-dict（字典回写）

```java

@Dict(type = "user_status")                    // 字典类型编码
private Integer status;
private String statusLabel;                    // 自动回写目标（默认: 字段名 + "Label"）

@Dict(type = "gender", labelField = "genderText")  // 指定回写字段名
private Integer gender;
private String genderText;
```

> ⚠️ 框架不内置字典缓存，需实现 `DictProvider` 接口提供字典翻译能力

### jpa-plus-logic-delete（逻辑删除）

- **字段级**：`@LogicDelete` 标注在实体字段上，保存时若字段为 null 则自动设为未删除默认值
- **查询级**：`LogicDeleteInterceptor` 拦截器自动追加 `deleted = 0` 条件到 AST
- 支持 `Integer`、`Boolean`、`Long`、`String` 类型

```java

@LogicDelete                              // 默认: value="1"(已删除), defaultValue="0"(未删除)
private Integer deleted;

@LogicDelete(value = "true", defaultValue = "false")  // 自定义删除值
private Boolean isDeleted;
```

### jpa-plus-version（乐观锁）

```java

@Version                                   // 每次保存自动 +1
private Integer version;                   // 支持 Integer / Long
```

### jpa-plus-tenant（多租户隔离）

- 继承 `TenantInterceptor` 并覆盖 `getCurrentTenantId()` 方法
- 自动在所有操作（QUERY / SAVE / DELETE）中注入 `tenant_id = ?` 条件
- 返回 `null` 时不启用租户隔离

### jpa-plus-permission（数据权限）

- 继承 `PermissionInterceptor` 并覆盖 `getPermissionCondition()` 方法
- 仅对 QUERY 操作生效，自动注入行级权限过滤条件到 AST
- 返回 `null` 时不限制

### jpa-plus-audit（审计日志）

- 在 SAVE / DELETE 操作完成后通过 Spring `ApplicationEventPublisher` 发布 `AuditEvent`
- `AuditEvent` 为 Java record：`entity`（实体对象）+ `operation`（操作类型）+ `timestamp`（时间戳）
- 建议配合 `@TransactionalEventListener` 在事务提交后异步处理

### jpa-plus-datasource（多数据源路由）

- `@DS("slave")` — 注解式切换（支持标注在方法或类上）
- `JpaPlusContext.withDS("slave", callable)` — 编程式切换（有返回值）
- `JpaPlusContext.runWithDS("slave", runnable)` — 编程式切换（无返回值）
- 基于 JDK 25 `ScopedValue` 实现，天然支持虚拟线程，无 ThreadLocal 泄漏风险
- ⚠️ 在活跃事务内不允许切换数据源，防止事务一致性问题

### jpa-plus-starter（Spring Boot 自动装配）

- 通过 `@AutoConfiguration` + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册
- 提供 `JpaPlusRepository<T, ID>` 接口 — 继承 `JpaRepository` 并扩展 Wrapper 查询能力
- 所有 Bean 均支持 `@ConditionalOnMissingBean`，用户可自定义替换任何组件
- 自动注册：`SqlCompiler`、`QueryExecutor`、`FieldEngine`、`InterceptorChain`、`JpaPlusExecutor` 及所有 FieldHandler

---

## 🎯 设计模式一览

| 模式                                | 应用场景                                                                  |
|-----------------------------------|-----------------------------------------------------------------------|
| **责任链** (Chain of Responsibility) | `InterceptorChain` — 拦截器链串联执行                                         |
| **策略** (Strategy)                 | `FieldHandler` / `SqlCompiler` / `DesensitizeStrategy` — 可插拔算法        |
| **模板方法** (Template Method)        | `TenantInterceptor` / `PermissionInterceptor` / `AbstractSqlCompiler` |
| **门面** (Facade)                   | `JpaPlusExecutor` — 统一数据操作入口                                          |
| **建造者** (Builder)                 | `QueryWrapper` / `JoinWrapper` — 链式 API 构建查询                          |
| **装饰器** (Decorator)               | `DebugSqlCompiler` — 透明增强 SQL 编译器                                     |
| **观察者** (Observer)                | `AuditEvent` + Spring Event — 事件驱动解耦                                  |
| **不可变值对象** (Immutable VO)         | `DataInvocation` / `QueryContext` / `PageResult` — Java Record        |
| **SPI 服务发现**                      | `JpaPlusLoader` — 自定义 SPI 加载与缓存                                       |
| **组合** (Composite)                | `FieldEngine` — 统一管理多个 FieldHandler                                   |
| **上下文对象** (Context Object)        | `JpaPlusContext` — ScopedValue 传递数据源                                  |
| **适配器** (Adapter)                 | `JpaPlusRepository` — 适配 JPA-Plus 到 Spring Data 体系                    |

---

## 🛠️ 技术栈

| 技术                  | 版本                | 用途                                             |
|---------------------|-------------------|------------------------------------------------|
| JDK                 | 25                | `ScopedValue`、虚拟线程、Record、Sealed Class、模式匹配    |
| Spring Boot         | 4.1.0-M3          | 自动装配、条件配置、事件机制                                 |
| Jakarta Persistence | (BOM 管理)          | JPA 标准 API                                     |
| Hibernate ORM       | (BOM 管理)          | ORM 实现、`Session.isDirty()`                     |
| Spring Data JPA     | (BOM 管理)          | Repository 抽象、Pageable                         |
| JUnit               | 6.1.0-M1          | 单元测试                                           |
| Lombok              | 9.2.0 (plugin)    | 编译期代码生成（`@Slf4j`、`@RequiredArgsConstructor` 等） |
| Gradle              | 9.4+ (Kotlin DSL) | 构建系统                                           |

---

## 📂 项目构建

```bash
# 克隆项目
git clone https://github.com/guanxiangkai/jpa-plus.git
cd jpa-plus

# 编译（需要 JDK 25+）
./gradlew build

# 运行测试
./gradlew test

# 清理构建
./gradlew clean
```

> ⚠️ 本项目使用了 JDK 25 预览特性（如 `ScopedValue`），编译和运行时需添加 `--enable-preview` 参数。  
> 该配置已在根 `build.gradle.kts` 中通过 `compilerArgs` 和 `jvmArgs` 自动设置，无需手动指定。

### Gradle 构建说明

- **版本目录**：所有依赖版本集中管理于 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)
- **子模块自动发现**：`settings.gradle.kts` 自动扫描根目录下包含 `.gradle.kts` 构建文件的子目录
- **BOM 统一管理**：通过 Spring Boot BOM 统一管理传递依赖版本
- **Lombok 插件**：所有子模块通过 `io.freefair.lombok` 插件统一配置

---

## 📦 发布到 GitHub Packages

所有子模块均可发布到 [GitHub Packages](https://github.com/guanxiangkai/jpa-plus/packages)，每个模块发布 3 个构件：

- `xxx.jar` — 主包
- `xxx-sources.jar` — 源码包
- `xxx-javadoc.jar` — 文档包

### 方式一：GitHub Actions 自动发布（推荐）

推送 `v*` 格式的 Tag 时自动触发发布：

```bash
git tag v2026.0.0
git push origin v2026.0.0
```

也可在 GitHub 仓库的 **Actions** 页面手动触发 `Publish to GitHub Packages` 工作流。

> GitHub Actions 使用内置的 `GITHUB_TOKEN`，无需额外配置密钥。

### 方式二：本地手动发布

**第 1 步**：创建 GitHub Personal Access Token

前往 [GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens)
，创建一个具有 **`write:packages`** 权限的 Token。

**第 2 步**：配置凭证

在 **全局** `~/.gradle/gradle.properties` 中添加（此文件不在项目中，不会被 Git 提交）：

```properties
gpr.user=你的GitHub用户名
gpr.key=ghp_你的PersonalAccessToken
```

**第 3 步**：执行发布

```bash
# 发布所有子模块
./gradlew publish

# 发布单个模块
./gradlew :jpa-plus-core:publish

# 先发布到本地 Maven 仓库验证
./gradlew publishToMavenLocal
```

### 消费方使用

其他项目引用 GitHub Packages 中的依赖：

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

dependencies {
    implementation("com.atomize:jpa-plus-starter:2026.0.0")
}
```

> ⚠️ GitHub Packages **读取也需要认证**（即使仓库是 public 的），消费方同样需要配置 Token（`read:packages` 权限即可）。

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
- [ ] 字典模块本地缓存支持（Caffeine L1 + Redis L2 SPI）
- [ ] 敏感词模块内置 DFA 引擎实现
- [ ] 自动排序 `@OrderBy` 注解支持
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

