package com.atomize.jpaplus.query.plan;

import com.atomize.jpaplus.core.exception.JpaPlusException;
import com.atomize.jpaplus.core.util.NamingUtils;
import com.atomize.jpaplus.core.util.ReflectionUtils;
import com.atomize.jpaplus.query.wrapper.SelectColumn;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 映射计划编译器
 *
 * <p>根据目标类型和 SELECT 列预计算映射方案，
 * 使用 {@link MethodHandle} 替代反射提高性能。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public class MappingPlanCompiler {

    private static final Map<String, MappingPlan<?>> CACHE = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * 编译映射计划
     */
    @SuppressWarnings("unchecked")
    public static <R> MappingPlan<R> compile(Class<R> targetType, List<SelectColumn> columns) {
        String cacheKey = targetType.getName() + "#" + columns.hashCode();
        return (MappingPlan<R>) CACHE.computeIfAbsent(cacheKey, _ -> doCompile(targetType, columns));
    }

    private static <R> MappingPlan<R> doCompile(Class<R> targetType, List<SelectColumn> columns) {
        try {
            Constructor<R> constructor = targetType.getDeclaredConstructor();
            List<FieldMapping> mappings = new ArrayList<>();

            for (int i = 0; i < columns.size(); i++) {
                SelectColumn sc = columns.get(i);
                String fieldName = sc.column().alias() != null ? sc.column().alias() : sc.column().columnName();

                // 尝试查找同名字段
                Field field = ReflectionUtils.findField(targetType, fieldName);
                if (field == null) {
                    // 尝试蛇形转驼峰
                    field = ReflectionUtils.findField(targetType, NamingUtils.snakeToCamel(fieldName));
                }

                if (field != null) {
                    MethodHandle setter = findSetter(targetType, field);
                    mappings.add(new FieldMapping(fieldName, i + 1, setter, field.getType()));
                }
            }

            return new MappingPlan<>(constructor, mappings);
        } catch (NoSuchMethodException e) {
            throw new JpaPlusException("Target type must have a no-arg constructor: " + targetType.getName(), e);
        }
    }

    private static MethodHandle findSetter(Class<?> type, Field field) {
        String setterName = "set" + field.getName().substring(0, 1).toUpperCase(Locale.ROOT) + field.getName().substring(1);
        try {
            return LOOKUP.findVirtual(type, setterName, MethodType.methodType(void.class, field.getType()));
        } catch (Exception e) {
            // 回退到直接字段访问
            try {
                field.setAccessible(true);
                return LOOKUP.unreflectSetter(field);
            } catch (IllegalAccessException ex) {
                throw new JpaPlusException("Cannot access field: " + field.getName(), ex);
            }
        }
    }
}

