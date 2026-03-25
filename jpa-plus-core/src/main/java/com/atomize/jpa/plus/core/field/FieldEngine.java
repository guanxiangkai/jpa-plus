package com.atomize.jpa.plus.core.field;

import com.atomize.jpa.plus.core.util.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段引擎
 *
 * <p>管理所有 {@link FieldHandler}，在实体保存前 / 查询后遍历字段并触发对应处理器。
 * 内部使用 {@link ConcurrentHashMap} 缓存每个实体类的字段列表，避免重复反射扫描。</p>
 *
 * <p><b>设计模式：</b>
 * <ul>
 *   <li>组合模式（Composite） —— 统一管理多个 FieldHandler</li>
 *   <li>缓存模式 —— ConcurrentHashMap 缓存反射结果</li>
 * </ul>
 * </p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class FieldEngine {

    private final List<FieldHandler> handlers;
    private final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    public FieldEngine(List<FieldHandler> handlers) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(FieldHandler::order))
                .toList();
    }

    /**
     * 保存前处理实体字段
     */
    public void beforeSave(Object entity, Class<?> entityClass) {
        if (entity == null) return;
        List<Field> fields = getFields(entityClass);
        for (Field field : fields) {
            for (FieldHandler handler : handlers) {
                try {
                    if (handler.supports(field)) {
                        handler.beforeSave(entity, field);
                    }
                } catch (Exception e) {
                    log.error("FieldHandler beforeSave error: handler={}, field={}", handler.getClass().getSimpleName(), field.getName(), e);
                    throw e;
                }
            }
        }
    }

    /**
     * 查询后处理实体字段
     */
    public void afterQuery(Object entity, Class<?> entityClass) {
        if (entity == null) return;
        List<Field> fields = getFields(entityClass);
        for (Field field : fields) {
            for (FieldHandler handler : handlers) {
                try {
                    if (handler.supports(field)) {
                        handler.afterQuery(entity, field);
                    }
                } catch (Exception e) {
                    log.error("FieldHandler afterQuery error: handler={}, field={}", handler.getClass().getSimpleName(), field.getName(), e);
                    throw e;
                }
            }
        }
    }

    private List<Field> getFields(Class<?> entityClass) {
        return fieldCache.computeIfAbsent(entityClass, ReflectionUtils::getHierarchyFields);
    }
}
