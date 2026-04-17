package com.actomize.jpa.plus.audit.interceptor;

import com.actomize.jpa.plus.audit.annotation.AuditExclude;
import com.actomize.jpa.plus.audit.event.AuditEventPublisher;
import com.actomize.jpa.plus.audit.event.DataAuditEvent;
import com.actomize.jpa.plus.audit.snapshot.AuditSnapshot;
import com.actomize.jpa.plus.audit.snapshot.FieldDiff;
import com.actomize.jpa.plus.audit.snapshot.SnapshotSerializer;
import com.actomize.jpa.plus.core.interceptor.Chain;
import com.actomize.jpa.plus.core.interceptor.DataInterceptor;
import com.actomize.jpa.plus.core.interceptor.Phase;
import com.actomize.jpa.plus.core.model.DataInvocation;
import com.actomize.jpa.plus.core.model.DeleteInvocation;
import com.actomize.jpa.plus.core.model.OperationType;
import com.actomize.jpa.plus.core.model.SaveInvocation;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量快照审计拦截器
 *
 * <p>在 SAVE / DELETE 操作执行前通过实体标识读取当前持久化状态作为 "before 快照"，
 * 操作完成后对比实体字段差异生成 {@link AuditSnapshot}，并附加到 {@link DataAuditEvent}
 * 一并发布。</p>
 *
 * <h3>字段过滤（{@code @AuditExclude}）</h3>
 * <p>在实体字段上标注 {@link AuditExclude} 可跳过该字段的快照采集，适用于加密、脱敏、大字段等。</p>
 *
 * <h3>自定义序列化（{@link SnapshotSerializer}）</h3>
 * <p>可通过构造函数传入自定义 {@link SnapshotSerializer}，控制字段值的存储格式：
 * 默认使用 {@link SnapshotSerializer#NOOP}（保留原始 Java 对象），
 * 也可注入 {@link com.actomize.jpa.plus.audit.snapshot.JacksonSnapshotSerializer} 将复杂类型转为 JSON 字符串。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 默认（无序列化）
 * // 注册为 Spring Bean
 * public SnapshotAuditInterceptor snapshotAuditInterceptor(
 *         AuditEventPublisher publisher, EntityManager em) {
 *     return new SnapshotAuditInterceptor(publisher, em);
 * }
 *
 * // 使用 Jackson 序列化
 * // 注册为 Spring Bean
 * public SnapshotAuditInterceptor snapshotAuditInterceptor(
 *         AuditEventPublisher publisher, EntityManager em, ObjectMapper objectMapper) {
 *     return new SnapshotAuditInterceptor(publisher, em,
 *             new JacksonSnapshotSerializer(objectMapper));
 * }
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年04月12日
 */
@Slf4j
public class SnapshotAuditInterceptor implements DataInterceptor {

    private final AuditEventPublisher eventPublisher;
    private final EntityManager entityManager;
    /**
     * 字段值序列化策略，默认不做序列化转换
     */
    private final SnapshotSerializer serializer;
    /**
     * 实体类 → 已解锁可审计字段列表缓存（按类一次性 setAccessible，避免逐次反射开销）
     */
    private final ConcurrentHashMap<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    /**
     * 使用默认 NOOP 序列化器（字段值保持原始 Java 对象类型）
     */
    public SnapshotAuditInterceptor(AuditEventPublisher eventPublisher, EntityManager entityManager) {
        this(eventPublisher, entityManager, SnapshotSerializer.NOOP);
    }

    /**
     * 使用自定义序列化器
     *
     * @param serializer 字段值序列化策略（如 {@link com.actomize.jpa.plus.audit.snapshot.JacksonSnapshotSerializer}）
     */
    public SnapshotAuditInterceptor(AuditEventPublisher eventPublisher,
                                    EntityManager entityManager,
                                    SnapshotSerializer serializer) {
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.serializer = serializer != null ? serializer : SnapshotSerializer.NOOP;
    }

    @Override
    public int order() {
        return 590;
    }

    @Override
    public Phase phase() {
        return Phase.BEFORE;
    }

    @Override
    public boolean supports(OperationType type) {
        return type == OperationType.SAVE || type == OperationType.DELETE;
    }

    @Override
    public Object intercept(DataInvocation invocation, Chain chain) throws Throwable {
        Object entity = switch (invocation) {
            case SaveInvocation si -> si.entity();
            case DeleteInvocation di -> di.entity();
            default -> null;
        };
        if (entity == null) return chain.proceed(invocation);
        Class<?> entityClass = invocation.entityClass();

        // P1-01: Read the "before" snapshot from an isolated persistence context so managed entities
        // do not see their own in-memory dirty state as the persisted state.
        Map<String, Object> beforeValues = loadPersistedSnapshot(entityClass, entity);

        Object result = chain.proceed(invocation);

        if (invocation.type() == OperationType.SAVE) {
            Object auditedEntity = resolveSaveAuditEntity(entity, result, entityClass);
            Map<String, Object> afterValues = captureValues(auditedEntity, entityClass);
            AuditSnapshot snapshot = diff(entityClass, beforeValues, afterValues);
            try {
                eventPublisher.publish(new DataAuditEvent(auditedEntity, invocation.type(), snapshot));
            } catch (Exception e) {
                log.warn("[jpa-plus] Failed to publish SnapshotAuditEvent for entity={}, operation={}",
                        entityClass.getSimpleName(), invocation.type(), e);
            }
        } else {
            // DELETE: 将删除前的每个字段快照化为 FieldDiff(name, beforeValue, null)
            Map<String, FieldDiff> diffs = new LinkedHashMap<>();
            beforeValues.forEach((field, value) -> diffs.put(field, new FieldDiff(field, value, null)));
            AuditSnapshot snapshot = new AuditSnapshot(entityClass, diffs);
            try {
                eventPublisher.publish(new DataAuditEvent(entity, invocation.type(), snapshot));
            } catch (Exception e) {
                log.warn("[jpa-plus] Failed to publish SnapshotAuditEvent for entity={}, operation={}",
                        entityClass.getSimpleName(), invocation.type(), e);
            }
        }
        return result;
    }

    /**
     * P1-01: Loads the database-persisted snapshot of the entity before the operation.
     * Uses an isolated EntityManager so the current persistence context cannot pollute the "before" state.
     */
    private Map<String, Object> loadPersistedSnapshot(Class<?> entityClass, Object entity) {
        Object entityId = extractIdentifier(entity);
        if (entityId == null) {
            return Map.of();
        }

        EntityManager snapshotEntityManager = createSnapshotEntityManager();
        if (snapshotEntityManager != null) {
            try (snapshotEntityManager) {
                return findPersistedSnapshot(snapshotEntityManager, entityClass, entityId);
            }
        }

        // P1-2 fix: do NOT fall back to the primary EntityManager when the snapshot EM is unavailable.
        // The primary EM's first-level cache already holds the *modified* managed entity, so
        // findPersistedSnapshot() would return the in-memory state instead of the DB state, producing
        // an empty diff ("beforeValues == afterValues"). Return an empty snapshot with a warning instead.
        log.warn("[jpa-plus] SnapshotAuditInterceptor: unable to create isolated EntityManager for entity '{}'. " +
                "Audit snapshot will be empty (no before-values). Consider checking EMF lifecycle.", entityClass.getSimpleName());
        return Map.of();
    }

    /**
     * 优先通过 PersistenceUnitUtil 提取标识，兼容 property access / @EmbeddedId。
     * 若当前环境无法获取 PersistenceUnitUtil，则回退到字段反射。
     */
    private Object extractIdentifier(Object entity) {
        Object identifier = extractIdentifierViaPersistenceUnit(entity);
        return identifier != null ? identifier : extractIdentifierViaField(entity);
    }

    private Object extractIdentifierViaPersistenceUnit(Object entity) {
        try {
            var entityManagerFactory = entityManager.getEntityManagerFactory();
            if (entityManagerFactory == null) return null;
            var persistenceUnitUtil = entityManagerFactory.getPersistenceUnitUtil();
            return persistenceUnitUtil != null ? persistenceUnitUtil.getIdentifier(entity) : null;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.trace("[jpa-plus] snapshot: cannot resolve identifier via PersistenceUnitUtil for {}: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    private Object extractIdentifierViaField(Object entity) {
        Class<?> cls = entity.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                    try {
                        field.setAccessible(true);
                        return field.get(entity);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private EntityManager createSnapshotEntityManager() {
        try {
            var entityManagerFactory = entityManager.getEntityManagerFactory();
            return entityManagerFactory != null ? entityManagerFactory.createEntityManager() : null;
        } catch (IllegalStateException e) {
            log.trace("[jpa-plus] snapshot: cannot create isolated EntityManager: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> findPersistedSnapshot(EntityManager source, Class<?> entityClass, Object entityId) {
        Object persisted = source.find(entityClass, entityId);
        return persisted != null ? captureValues(persisted, entityClass) : Map.of();
    }

    private Object resolveSaveAuditEntity(Object originalEntity, Object result, Class<?> entityClass) {
        if (entityClass.isInstance(result)) {
            return result;
        }
        return originalEntity;
    }

    /**
     * 提取实体所有字段的当前值快照
     * <ul>
     *   <li>递归包括父类字段（通过 {@link #resolveFields(Class)} 缓存，每个类只解锁一次）</li>
     *   <li>跳过 static 字段</li>
     *   <li>跳过 {@link AuditExclude} 标注的字段</li>
     *   <li>通过 {@link SnapshotSerializer} 对字段值进行序列化转换</li>
     * </ul>
     */
    private Map<String, Object> captureValues(Object entity, Class<?> entityClass) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Field field : resolveFields(entityClass)) {
            try {
                Object raw = field.get(entity);
                values.put(field.getName(), serializer.serializeValue(field.getName(), raw));
            } catch (Exception e) {
                log.trace("[jpa-plus] snapshot: cannot read field '{}': {}", field.getName(), e.getMessage());
            }
        }
        return values;
    }

    /**
     * 按实体类解析并缓存可审计字段列表。
     * 每个字段只调用一次 {@code setAccessible(true)}，后续复用缓存。
     */
    private List<Field> resolveFields(Class<?> entityClass) {
        return fieldCache.computeIfAbsent(entityClass, cls -> {
            List<Field> result = new ArrayList<>();
            Class<?> current = cls;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    if (field.isAnnotationPresent(AuditExclude.class)) {
                        log.trace("[jpa-plus] snapshot: skipping @AuditExclude field '{}'", field.getName());
                        continue;
                    }
                    field.setAccessible(true);
                    result.add(field);
                }
                current = current.getSuperclass();
            }
            return List.copyOf(result);
        });
    }

    private AuditSnapshot diff(Class<?> entityClass,
                               Map<String, Object> before,
                               Map<String, Object> after) {
        Map<String, FieldDiff> diffs = new LinkedHashMap<>();
        for (String field : after.keySet()) {
            Object beforeVal = before.get(field);
            Object afterVal = after.get(field);
            if (!java.util.Objects.equals(beforeVal, afterVal)) {
                diffs.put(field, new FieldDiff(field, beforeVal, afterVal));
            }
        }
        return new AuditSnapshot(entityClass, diffs);
    }
}
